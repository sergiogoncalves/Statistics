package statistics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class StatisticsClient {

    public static void main(String[] args) throws IOException {
        try (Socket s = new Socket("localhost", 80);
                OutputStream os = s.getOutputStream();
                PrintWriter out = new PrintWriter(os, true);
                InputStream is = s.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);) {

            // Gera uma transação e envia para o servidor
            double amount = (Math.random() * 1000);
            long now = System.currentTimeMillis();
            long timestamp = (now - Math.round(Math.random() * 120000));
            //System.out.println("amount: " + amount);
            //System.out.println("timestamp: " + timestamp);
            //System.out.println("difference: " + (now - timestamp));

            out.println("POST /transactions HTTP/1.1");
            out.println("");
            out.println("{");
            out.println("  \"amount\": " + amount + ",");
            out.println("  \"timestamp\": " + timestamp);
            out.println("}");
            out.println("");

            // Recebe a resposta do servidor e imprime na saída padrão
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}
