package debug;

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
    public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicInteger progres = new AtomicInteger(0);
    private PrintWriter pw = null;
    int rozdiel=0;
    BufferedReader socketReader = null;

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
            throw new InterruptedException();
        }
        return null;
    }

    public void upload() throws InterruptedException, IOException {
        boolean jePokracovanie = false;
        OutputStream outputStream = null;
        List<Integer> offsety = null;
        try {
            Socket communicator = new Socket("localhost", 20000);
            outputStream = communicator.getOutputStream();
            pw = new PrintWriter(outputStream);
            pw.println(file.getName());
            pw.println(file.length());
            pw.println(pocetSoketov);
            pw.flush();
            InputStream inputStream = communicator.getInputStream();
            InputStreamReader streamReader = new InputStreamReader(inputStream);
            socketReader = new BufferedReader(streamReader);
            String mod = socketReader.readLine();
            if (mod.equals("modPrerusene")) {
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
                    if (Thread.currentThread().isInterrupted()) {
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
                for (int k = 0; k < pocetSoketov; k++) {
                    rozdiel-=k*chunkSize;
                }
                progres.addAndGet(rozdiel);
                for (int i = 0; i < pocetSoketov - 1; i++) {
                    if (Thread.currentThread().isInterrupted() || (this.isCancelled())) {
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
                        shuttingDown(sokety, executor); //bolo prerusene get future, radsej vypnem sokety
                    } catch (ExecutionException ex) { //vnutorna vynimka, boli im zavreté sokety
                        throw new InterruptedException();
                    }
                }
            }
            System.out.println("subor je poslaný");
            executor.shutdown();
        } catch (IOException ex) {
        }
    }

    private void shuttingDown(Socket[] sokety, ExecutorService executor) throws IOException {
        if (fullCancel) {
            if (pw != null) {
                pw.println("cancel");
                pw.close();
            }
        }
        for (Socket sokety1 : sokety) { //zatvorim uloham sokety, prerusia sa
            try {
                sokety1.close();
            } catch (IOException ioe) {
                throw ioe;
            }
        }
        pw.close();  
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
        } catch (InterruptedException | ExecutionException | CancellationException ex) {
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
}
