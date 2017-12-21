package projekt;

import debug.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.Callable;

public class CancelListener implements Callable<Void> {

    private final BufferedReader bufferedReader;
    private final Server server;

    CancelListener(BufferedReader bufferedReader, Server server) {
        this.bufferedReader = bufferedReader;
        this.server = server;
    }

    @Override
    public Void call() throws IOException {
        String cancel = new String();
        try {
            cancel = bufferedReader.readLine(); //blokuje
        } catch (IOException ex) {
            System.out.println("canceler 31");
            throw ex;
        }
        if (cancel.equals("cancel")) {
            System.out.println("canceler 41");
            server.setJePrerusene(true);
        }
        try { //koncim, zatvaram
            bufferedReader.close(); //zatvori stream aj socket
        } catch (IOException ex) {
            System.out.println("canceler 47");
            throw ex;
        }
        System.out.println("canceler 42");
        return null;
    }
}
