package debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

public class Server {

    private volatile boolean jePrerusene; //ci sa teraz prerusilo

    public static void main(String args[]) {
        Server server = new Server();
        server.run(server);

    }

    private void run(Server server) {
        boolean on = true;
        ServerSocket serverSocket;
        ServerSocket communicationSocket;
        File prerusenieFile = null;
        File serverFile = null;
        Semaphore semaphore = null;
        int i = 0;
        int fileSize = 0;
        int chunkSize = 0;
        ExecutorService executor = null;
        ExecutorService executorForListener = null;
        int pocetSoketov = 0;
        List<Future<Integer>> futures = null;
        List<Integer> offsety = null;
        BufferedReader fileReader = null;
        PrintWriter pw = null;
        OutputStream outputStream = null;
        boolean jePokracovanie = false;
        BufferedReader socketReader = null;
        Future listenerFuture = null;
        Ukladac ukladac = null;
        int lastSize = 0;
        Socket clientCommunicator = null;
        try {
            serverSocket = new ServerSocket(22000);
            communicationSocket = new ServerSocket(20000); // na spravy mimo samotnych dat
            while (on) {
                if (i == 0) { //pociatocna komunikacia
                    jePrerusene = false;
                    clientCommunicator = communicationSocket.accept(); //skusim najst subor pre pokracovanie a klientovi poslat udaje ak som nasiel
                    InputStream inputStream = clientCommunicator.getInputStream();
                    InputStreamReader streamReader = new InputStreamReader(inputStream);
                    socketReader = new BufferedReader(streamReader);
                    String serverFileName = socketReader.readLine();
                    serverFile = new File("D:\\serverSpace\\" + serverFileName);//prisiel mi nazov suboru od klienta
                    fileSize = Integer.parseInt(socketReader.readLine()); //poslem si z klienta
                    prerusenieFile = new File(serverFile.getCanonicalPath() + ".txt"); //textak s datami prerusenia
                    if (prerusenieFile.exists()) {//existuje, bol preruseny, vytiahnem data a nastavim nove offsety, pocet vlaken
                        socketReader.readLine(); //precitam pocet soketov naprazdno
                        String line;
                        fileReader = new BufferedReader(new FileReader(prerusenieFile.getCanonicalPath()));
                        offsety = new ArrayList<>();
                        while ((line = fileReader.readLine()) != null) {
                            offsety.add(Integer.parseInt(line));
                        }
                        pocetSoketov = offsety.size();//stary pocet soketov
                        fileReader.close();
                        Collections.sort(offsety); //su od najmensieho
                        outputStream = clientCommunicator.getOutputStream();
                        pw = new PrintWriter(outputStream);
                        pw.println("modPrerusene");
                        pw.println(pocetSoketov); //posielam stary pocet soketov
                        for (int j = 0; j < offsety.size(); j++) {
                            pw.println(offsety.get(j)); //posielam stare offsety klientovi
                        }
                        pw.flush();
                        prerusenieFile.delete();
                        jePokracovanie = true;
                    } else {//do suboru ulozim pocet vlaken, offsety dam na nuly
                        outputStream = clientCommunicator.getOutputStream();
                        pw = new PrintWriter(outputStream);
                        pw.println("modNovy"); //novy subor, nie pokracovanie, offsety budu nuly
                        pw.flush();
                        pocetSoketov = Integer.parseInt(socketReader.readLine());
                        jePokracovanie = false;
                    }
                    semaphore = new Semaphore(1);
                    executor = Executors.newFixedThreadPool(pocetSoketov);
                    futures = new LinkedList<>();
                    chunkSize = (int) (fileSize / pocetSoketov);
                    lastSize = (int) (fileSize - chunkSize * (pocetSoketov - 1));
                    executorForListener = Executors.newFixedThreadPool(1);
                    CancelListener cancelListener = new CancelListener(socketReader, server); //zistuje ci bolo poslane cancel
                    listenerFuture = executorForListener.submit(cancelListener);
                }
                Socket clientSocket = serverSocket.accept(); //prisiel socket, metoda blokuje kym nepride socket
                if (!jePokracovanie) {
                    if (!clientSocket.isClosed()) {
                        if (i == pocetSoketov - 1) {
                            ukladac = new Ukladac(i * chunkSize, lastSize, clientSocket, serverFile, semaphore, prerusenieFile);
                        } else {
                            ukladac = new Ukladac(i * chunkSize, chunkSize, clientSocket, serverFile, semaphore, prerusenieFile);
                        }
                        Future future = executor.submit(ukladac);
                        futures.add(future);
                    } else {
                        i = pocetSoketov - 1;
                    }

                } else if (!clientSocket.isClosed()) {
                    if (i == pocetSoketov - 1) {
                        ukladac = new Ukladac(offsety.get(i), (i * chunkSize) + lastSize - offsety.get(i), clientSocket, serverFile, semaphore, prerusenieFile);
                    } else {
                        ukladac = new Ukladac(offsety.get(i), (i * chunkSize) + chunkSize - offsety.get(i), clientSocket, serverFile, semaphore, prerusenieFile);
                    }
                    Future future = executor.submit(ukladac);
                    futures.add(future);
                } else {
                    i = pocetSoketov - 1;
                }
                i++;
                if (i == pocetSoketov) {
                    i = 0; //pojde novy cyklus s novym suborom
                    for (Future<Integer> futurel : futures) {
                        try {
                            futurel.get();
                            //co sa uz zapisalo
                        } catch (InterruptedException ex) {
                            executor.shutdown(); // get future sa mi pri niektorej prerusil, skusim este ukoncit ostatne
                        } catch (ExecutionException ex) { //vnutorna vynimka z ukladaca
                            futurel.cancel(true); //ukoncim ulohe vlakno, uloha je uz skoncena, zatvorila si socket
                        }
                    }
                    System.out.println("subor je zapisany");
                    executor.shutdown();
                    try {
                        socketReader.close(); //zatvara stream aj socket, uloha zomrie
                    } catch (IOException ex) {
                    }
                    if (jePrerusene) { //ak sa ukoncili klientove sokety - skoncil, tak idem zistit ci predtym neposlal spravu na cancel, ak ano tak vymazem subory
                        prerusenieFile.delete(); // sice sa prerusilo ale odstranim subor aj prerusovaci subor
                        serverFile.delete();
                    }
                    try {
                        listenerFuture.get();
                    } catch (InterruptedException | ExecutionException ex) {
                    }
                }
            }
        } catch (IOException ex) {
            executor.shutdown();
        }
    }

    /**
     * @return the jePrerusene
     */
    public boolean isJePrerusene() {
        return jePrerusene;
    }

    /**
     * @param jePrerusene the jePrerusene to set
     */
    public void setJePrerusene(boolean jePrerusene) {
        this.jePrerusene = jePrerusene;
    }
}
