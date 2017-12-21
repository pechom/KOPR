package projectv2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

public class ProgressSwingWorker extends SwingWorker<Void, Integer> {

    private final JProgressBar progressbar;
    private int pocetSoketov;
    private int chunkSize;
    private final File file;
    private volatile boolean fullCancel = false;
    private volatile boolean isInterrupted = false;
    public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicInteger progres = new AtomicInteger(0);
    private PrintWriter pw = null;
    int rozdiel=0;

    ProgressSwingWorker(JProgressBar ProgressBar, File file, int pocetSoketov) {
        this.progressbar = ProgressBar;
        this.pocetSoketov = pocetSoketov;
        this.file = file;
    }

    @Override
    protected Void doInBackground() throws Exception {
        if ((Thread.currentThread().isInterrupted()) || this.isCancelled()) { //bude worker reagovat na prve?
            throw new InterruptedException();
        }
        try {
            upload();
        } catch (InterruptedException | IOException e) {
            System.out.println("worker 50");
            throw new InterruptedException();
        }
        return null;
    }

    public void upload() throws InterruptedException, IOException {
        System.out.println("worker 59");
        boolean jePokracovanie = false;
        OutputStream outputStream = null;
        List<Integer> offsety = null;
        try {
            Socket communicator = new Socket("localhost", 20000);
            System.out.println("worker 65");
            outputStream = communicator.getOutputStream();
            System.out.println("worker 67");
            pw = new PrintWriter(outputStream);
            pw.println(file.getName());
            pw.println(file.length());
            pw.println(pocetSoketov);
            pw.flush();
            InputStream inputStream = communicator.getInputStream();
            InputStreamReader streamReader = new InputStreamReader(inputStream);
            BufferedReader socketReader = new BufferedReader(streamReader);
            String mod = socketReader.readLine();
            System.out.println("worker 72");
            if (mod.equals("modPrerusene")) {
                System.out.println("worker 74");
                jePokracovanie = true;
                offsety = new ArrayList<>();
                pocetSoketov = Integer.parseInt(socketReader.readLine()); //stary pocet soketov pred prerusenim
                for (int i = 0; i < pocetSoketov; i++) {
                    offsety.add(Integer.parseInt(socketReader.readLine()));
                    rozdiel+=offsety.get(i);
                }
                Collections.sort(offsety);
            }
        } catch (IOException ex) {
            System.out.println("worker 83");
            pw.close(); //zatvorim socket, stream aj pw
            Thread.currentThread().interrupt();
            throw ex;
        }
        ExecutorService executor = Executors.newFixedThreadPool(pocetSoketov); //ma dany pocet vlaken
        Socket[] sokety = new Socket[pocetSoketov]; //vytvorim pole socketov
        List<Future<Integer>> futures = new LinkedList<>();
        try {
            for (int i = 0; i < sokety.length; i++) {
                sokety[i] = new Socket("localhost", 22000);  //sokety pre vlakna
            }

            // rozdelim subor na casti pre kazdy soket a kazde vlakno dostane cast suboru a soket cez ktory ho bude posielat
            //tak dostane kazdy soket priblizne rovnaku cast suboru na poslanie
            chunkSize = (int) (file.length() / pocetSoketov);
            int lastSize = (int) (file.length() - chunkSize * (pocetSoketov - 1));

            if (!jePokracovanie) {
                for (int i = 0; i < pocetSoketov - 1; i++) {
                    System.out.println("worker 104");
                    if (Thread.currentThread().isInterrupted()) {
                        System.out.println("worker 106");
                        shuttingDown(sokety, executor);
                        throw new InterruptedException();
                    }
                    Posielac posielac = new Posielac(i * chunkSize, chunkSize, sokety[i], file, this); //offset, dlzka
                    Future future = executor.submit(posielac);
                    futures.add(future);
                }
                Posielac posielac = new Posielac((pocetSoketov - 1) * chunkSize, lastSize, sokety[pocetSoketov - 1], file, this);
                Future future = executor.submit(posielac);
                futures.add(future);
            } else {
                System.out.println("worker 117");
                for (int k = 0; k < pocetSoketov; k++) {
                    rozdiel-=k*chunkSize;
                }
                progres.addAndGet(rozdiel);
                for (int i = 0; i < pocetSoketov - 1; i++) {
                    if (Thread.currentThread().isInterrupted() || (this.isCancelled())) {
                        System.out.println("worker 120");
                        shuttingDown(sokety, executor);
                        throw new InterruptedException();
                    }
                    Posielac posielac = new Posielac(offsety.get(i), (i * chunkSize) + chunkSize - offsety.get(i), sokety[i], file, this); //offset, dlzka
                    Future future = executor.submit(posielac);
                    futures.add(future);
                }
                int last = pocetSoketov - 1;
                Posielac posielac = new Posielac(offsety.get(last), (last * chunkSize) + lastSize - offsety.get(last), sokety[last], file, this);
                Future future = executor.submit(posielac);
                futures.add(future);
            }
            int chunk = 0;
            publish(rozdiel);
            while (chunk < progressbar.getMaximum()) { //toto bezi cely cas, urcite zachytim ked sa prerusi vlakno a vypnem uloham sockety
                if (Thread.currentThread().isInterrupted() || (this.isCancelled())) {
                    System.out.println("worker 135");
                    shuttingDown(sokety, executor);
                    throw new InterruptedException();
                }
                chunk = progres.get();
                publish(chunk);
            }

            for (int i = 0; i < pocetSoketov; i++) {
                for (Future<Integer> futurel : futures) {
                    try {
                        futurel.get();
                    } catch (InterruptedException ex) {
                        System.out.println("worker 148");
                        shuttingDown(sokety, executor); //bolo prerusene get future, radsej vypnem sokety
                    } catch (ExecutionException ex) { //vnutorna vynimka, boli im zavreté sokety
                        System.out.println("worker 151");
                        throw new InterruptedException();
                    }
                }
            }
            System.out.println("subor je poslaný");
            executor.shutdown();
        } catch (IOException ex) {
            System.out.println("worker 161");
        }
    }

    private void shuttingDown(Socket[] sokety, ExecutorService executor) throws IOException {
        if (fullCancel) {
            System.out.println("worker 168");
            if (pw != null) {
                pw.println("cancel");
                pw.close();
            }
        }
        for (Socket sokety1 : sokety) { //zatvorim uloham sokety, prerusia sa
            try {
                System.out.println("worker 176");
                sokety1.close();
            } catch (IOException ioe) {
                System.out.println("worker 179");
                ioe.printStackTrace();
                throw ioe;
            }
        }
        executor.shutdownNow();
    }

    @Override
    protected void process(List<Integer> chunks) {  //nazberané hodnoty pre progressbar
        progressbar.setValue(chunks.get(chunks.size() - 1));
    }

    @Override
    protected void done() { //pre EDT
        try {
            get();
            System.out.println("worker 195");
        } catch (InterruptedException | ExecutionException | CancellationException ex) {
            System.out.println("worker 197");
        }
        progressbar.setValue(progressbar.getMaximum()); //uz som dostal vsetky hodnoty
    }

    public AtomicInteger getProgres() {
        return progres;
    }

    /**
     * @param fullCancel the fullCancel to set
     */
    public void setFullCancel(boolean fullCancel) {
        this.fullCancel = fullCancel;
    }

    /**
     * @param isInterrupted the isInterrupted to set
     */
    public void setIsInterrupted(boolean isInterrupted) {
        this.isInterrupted = isInterrupted;
    }
}
