package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {
        ServerSocket ss = null;
        Socket s = null;
        try {
            ss = new ServerSocket(3333);
            while (true) {
                s = ss.accept();
                System.out.println("Novi korisnik se povezao " + s.getInetAddress().getHostAddress());
                ServerNit noviKlijent = new ServerNit(s);
                new Thread(noviKlijent).start();
            }
        } catch (IOException e) {
            System.out.println("Message: " + e.getMessage());
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    System.out.println("Message: " + e.getMessage());
                }
            }
        }
    }
}
