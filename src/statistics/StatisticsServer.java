package statistics;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatisticsServer {

    private static final int SERVER_PORT = 80;

    public static void main(String[] args) {
        startServer();
    }

    private static void startServer() {
        try {
            ServerSocket ss = new ServerSocket(SERVER_PORT);
            System.out.println("Server running...");

            while (true) {
                Socket socket = ss.accept();
                TransactionManager tm = new TransactionManager(socket);
                new Thread(tm).start();
            }
        } catch (IOException ex) {
            Logger.getLogger(StatisticsServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
