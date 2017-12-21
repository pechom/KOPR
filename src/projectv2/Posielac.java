package projectv2;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.concurrent.Callable;

class Posielac implements Callable<Integer> {

    private final int start;
    private final int chunkSize;
    private RandomAccessFile raf;
    private final Socket socket;
    private final File file;
    private final ProgressSwingWorker worker;

    public Posielac(int start, int chunkSize, Socket socket, File file, ProgressSwingWorker worker) {
        this.start = start;
        this.chunkSize = chunkSize;
        this.socket = socket;
        this.file = file;
        this.worker = worker;
    }

    @Override
    public Integer call() throws Exception {
        byte[] fileBytes = new byte[65532];
        int zostatok = 0;
        raf = new RandomAccessFile(file, "r"); // kedze sa neprekryvaju, nemusim synchronizovat nad file
        int read = 0;
        OutputStream os = null;
        try {
            os = socket.getOutputStream();
        } catch (IOException ex) {
            System.out.println("posielac 40");
            throw ex;
        }
        raf.seek(start);
        int i = 1;
        while (read < chunkSize) {
            raf.read(fileBytes);
            try {
                if (!socket.isClosed()) {
                    os.write(fileBytes);
                    os.flush();
                } else {
                    throw new InterruptedException();
                }
            } catch (IOException ex) {
                System.out.println("posielac 56");
                throw ex;
            }
            try {
                worker.lock.writeLock().lockInterruptibly();
                try {
                    worker.getProgres().addAndGet(fileBytes.length); //data pre progressbar, konkurentne zo vsetkych vlaken
                } finally {
                    worker.lock.writeLock().unlock();
                }
            } catch (InterruptedException e) {
                System.out.println("posielac 68");
                throw e;
            }
            i++;
            read = i * fileBytes.length;
            zostatok = chunkSize - (i - 1) * fileBytes.length;
        }
        raf.read(fileBytes, 0, zostatok);
        System.out.println("posielac 76");
        try {
            os.write(fileBytes, 0, zostatok);
        } catch (IOException ex) {
            System.out.println("posielac 79");
            throw ex;
        }
        try {
            worker.lock.writeLock().lockInterruptibly();
            try {
                worker.getProgres().addAndGet(zostatok); //data pre progressbar, konkurentne zo vsetkych vlaken
            } finally {
                worker.lock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            System.out.println("posielac 91");
            throw e;
        }
        try {
            socket.close();
        } catch (IOException ex) {
            System.out.println("posielac 99");
            throw ex;
        }
        return start + chunkSize; //co je uz zapisane
    }
}
