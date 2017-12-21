package debug;

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
            throw ex;
        }
        if (cancel.equals("cancel")) {
            server.setJePrerusene(true);
        }
        try { //koncim, zatvaram
            bufferedReader.close(); //zatvori stream aj socket
        } catch (IOException ex) {
            throw ex;
        }
        return null;
    }
}
