package klijent;

import server.ServerNit;

import java.io.*;
import java.net.Socket;

public class Klijent {
    private static final String fajlPutanja = "src/klijent/Rezervacija.txt";
    private static Socket s = null; //Soket za konekciju
    private static PrintStream tokKaServeruTekst = null; //Izlazni kanal za komunikaciju

    public static void main(String[] args) {
        try {
            s = new Socket("localhost", 3333);
            tokKaServeruTekst = new PrintStream(s.getOutputStream());
            meni();
            tokKaServeruTekst.close();
            System.out.println("Konekcija je prekinuta");
            s.close();
        } catch (IOException e) {
            System.out.println("Message: " + e.getMessage());
        }
    }

    private static void meni()
            throws IOException {
        BufferedReader tokOdServeraTekst = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedReader ulazSaTastature = new BufferedReader(new InputStreamReader(System.in));
        String izbor = "";
        String procitanTekst = "";
        while (s.isBound()) {
            if (izbor.equals("Kraj")) {
                if (tokOdServeraTekst != null)
                    tokOdServeraTekst.close();
                break;
            }
            while (true) {
                procitanTekst = tokOdServeraTekst.readLine();
                if (procitanTekst.equals(""))
                    break;
                System.out.println(procitanTekst);
                if (procitanTekst.contains("Rezervisali ste kartu"))
                    primanjeFajla(tokOdServeraTekst);
            }

            izbor = ulazSaTastature.readLine();
            tokKaServeruTekst.println(izbor);
        }
    }

    private static void primanjeFajla(BufferedReader tokOdServeraTekst) throws IOException {
        PrintWriter pw = null;
        String rezervacija = tokOdServeraTekst.readLine();
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(fajlPutanja)));
            pw.println(rezervacija);
        } catch (Exception e) {
            System.out.println("Message: " + e.getMessage());
        } finally {
            if (pw != null) {
                pw.close();
            }
            System.out.println("Fajl o potvrdi rezervacije je sacuvan na putanji " + fajlPutanja);
        }
    }
}
