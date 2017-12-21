package projekt;

import debug.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

class Ukladac implements Callable<Integer> {

    private final int start;
    private final Socket clientSocket;
    private final File serverFile;
    private final Semaphore semaphore;
    private final File prerusenieFile;
    private int read;
    private InputStream in = null;
    private final byte[] fileBytes = new byte[65532];
    private int zapisane = 0;
    private boolean jeZapisanePrerusenie = false;
    private int chunkSize;

    public Ukladac(int start, int chunkSize, Socket clientSocket, File serverFile, Semaphore semaphore, File prerusenieFile) {
        this.start = start;
        this.serverFile = serverFile;
        this.clientSocket = clientSocket;
        this.semaphore = semaphore;
        this.prerusenieFile = prerusenieFile;
        this.chunkSize = chunkSize;
    }

    public void writeToInterruptFile() throws InterruptedException, IOException { //niekedy posledny nezapise, neochyti sa vynimka pri preruseni
        System.out.println("ukladac 38");
        BufferedWriter writer = null;
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            System.out.println("ukladac 42");
            throw e;
        }
        try {
            writer = new BufferedWriter(new FileWriter(prerusenieFile.getCanonicalPath(), true)); //pokracujem v zapise
            System.out.println("ukladac 47");
            int offset = start + zapisane;
            writer.write(String.valueOf(offset)); // co uz je priebezne ulozene + start = novy offset pre pripad prerusenia
            writer.newLine();
            writer.flush();
            System.out.println("ukladac 51");
            jeZapisanePrerusenie = true;
        } catch (IOException ioe) {
            System.out.println("ukladac 59");
            throw ioe;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ioe2) {
                    System.out.println("ukladac 66");
                    throw ioe2;
                }
            }
            semaphore.release();
        }
    }

    @Override
    public Integer call() throws IOException, InterruptedException {
        RandomAccessFile raf = null;
        try {
            try {
                raf = new RandomAccessFile(serverFile, "rw"); //nemusim jeho operacie synchronizovat nad suborom lebo sa neprekryvaju zapisovane casti
            } catch (FileNotFoundException ex) {
                System.out.println("ukladac 98");
            }
            try {
                in = clientSocket.getInputStream();
            } catch (IOException ex) {
                System.out.println("ukladac 104");
                throw ex;
            }
            try {
                raf.seek(start);
                while (((read = in.read(fileBytes)) >= 0) && (!Thread.currentThread().isInterrupted())) {
                    try {
                        zapisane += read;  //priebezne co sa uz zapisalo
                        raf.write(fileBytes, 0, read);
                    } catch (IOException ex) {
                        System.out.println("ukladac 122");
                        raf.close();
                        writeToInterruptFile();
                        throw ex;
                    }
                }
            } catch (IOException zz) {
                System.out.println("ukladac 99");
                if (!jeZapisanePrerusenie) {
                    writeToInterruptFile();
                }
                throw zz;
            }

        } catch (IOException ioexp) {
            System.out.println("ukldaca 107");
            if (!jeZapisanePrerusenie) {
                writeToInterruptFile();
                throw ioexp;
            }
        } finally {
            raf.close(); //ukoncim raf, teraz sa nic neviaze na subor, mozno ho ukoncit
            if (!jeZapisanePrerusenie) {
                writeToInterruptFile();
            }
            try {
                in.close();
            } catch (IOException ex) {
                System.out.println("ukladac 143");
                if (!jeZapisanePrerusenie) {
                    writeToInterruptFile();
                }
                throw ex;
            }
        }
        return start + zapisane;
    }
}
