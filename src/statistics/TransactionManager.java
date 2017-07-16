package statistics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionManager implements Runnable {
    /** Limit time to storage transactions: 60 seconds */
    public static int TRANSACTION_TIME_LIMIT = 60000;

    private static final List<Transaction> TRANSACTIONS = new LinkedList<>();
    private final Socket socket;

    public TransactionManager(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                OutputStream os = socket.getOutputStream();
                PrintWriter out = new PrintWriter(os)) {
            String row = br.readLine();

            if (row != null) {

                String[] command = row.split(" ");

                if (null == command[0]) {
                    doError(out);
                } else {
                    switch (command[0]) {
                        case "GET":
                            if ("/statistics".equals(command[1])) {
                                doGet(out);
                            } else {
                                doError(out);
                            }
                            break;
                        case "POST":
                            if ("/transactions".equals(command[1])) {
                                doPost(br, out);
                            } else {
                                doError(out);
                            }
                            break;
                        default:
                            doError(out);
                            break;
                    }
                }
            }
            socket.close();

        } catch (IOException ex) {
            Logger.getLogger(TransactionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void doGet(PrintWriter out) {
        Statistics statistics = calculateStatistics();

        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: application/javascript");
        out.println("");
        out.println("{");
        out.println("    \"sum\": " + String.valueOf(statistics.getSum()) + ",");
        out.println("    \"avg\": " + String.valueOf(statistics.getAvg()) + ",");
        out.println("    \"max\": " + String.valueOf(statistics.getMax()) + ",");
        out.println("    \"min\": " + String.valueOf(statistics.getMin()) + ",");
        out.println("    \"count\": " + String.valueOf(statistics.getCount()));
        out.println("}");
        out.flush();
    }

    private void doError(PrintWriter out) {
        out.println("HTTP/1.1 404 OK");
        out.println("Content-Type: text/html");
        out.println("");
        out.flush();
    }

    private Statistics calculateStatistics() {
        long now = System.currentTimeMillis();
        synchronized (TRANSACTIONS) {
            // Remove as transações vencidas
            int i = 0;
            while (i < TRANSACTIONS.size()) {
                Transaction t = TRANSACTIONS.get(i);
                if (now > t.getTimestamp() + TRANSACTION_TIME_LIMIT) {
                    TRANSACTIONS.remove(i);
                } else {
                    i++;
                }
            }
            //Calculate of statstcs
            // System.out.println("Valid transactions (amount, timestamp, difference):");
            double sum = 0;
            double max = Double.MIN_VALUE;
            double min = Double.MAX_VALUE;
            for (Transaction t : TRANSACTIONS) {
                // System.out.println(t.getAmount() + ", " + t.getTimestamp() + ", " + (now - t.getTimestamp()));
                sum += t.getAmount();
                if (t.getAmount() > max) {
                    max = t.getAmount();
                }
                if (t.getAmount() < min) {
                    min = t.getAmount();
                }
            }

            Statistics statistics = new Statistics();
            statistics.setSum(sum);
            statistics.setCount(TRANSACTIONS.size());
            statistics.setAvg(sum / (double) statistics.getCount());
            statistics.setMax(max);
            statistics.setMin(min);

            return statistics;
        }
    }

    private void doPost(BufferedReader br, PrintWriter out) throws IOException {
        //Termina a leitura do cabeçalho
        String line;
        do {
            line = br.readLine();
        } while (line != null && !line.equals(""));

        //Lê o corpo da requisição
        double amount = 0.0;
        long timestamp = 0;
        while ((line = br.readLine()) != null && !line.equals("")) {
            line = line.trim();
            if (line.startsWith("\"amount\"")) {
                line = line.split((":"))[1].trim();
                line = line.replaceAll(",", "");
                amount = Double.valueOf(line);
            } else if (line.startsWith("\"timestamp\"")) {
                line = line.split((":"))[1].trim();
                line = line.replaceAll(",", "");
                timestamp = Long.valueOf(line);
            }
        }

        Transaction transaction = new Transaction(amount, timestamp);
        if (System.currentTimeMillis() > transaction.getTimestamp() + TRANSACTION_TIME_LIMIT) {
            
            out.println("HTTP/1.1 204 OK");
            out.println("Content-Type: text/html");
            out.println("");
            out.flush();
        } else {
            // Adiciona a transação
            synchronized (TRANSACTIONS) {
                TRANSACTIONS.add(transaction);
            }

            out.println("HTTP/1.1 201 OK");
            out.println("Content-Type: text/html");
            out.println("");
            out.flush();
        }
    }
}
