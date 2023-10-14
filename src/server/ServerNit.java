package server;

import java.io.*;
import java.net.Socket;

public class ServerNit implements Runnable {
    private static final int ukupanBrojKarata = 20;
    private static final int maksimalanBrojRezervacija = 4;
    private static final int vipKarte = 5;

    private static final String meni = "Izaberite jednu od opcija:\n1.Broj preostalih karata\n2.Rezervisite kartu\n3.Registrujte se\n4.Prijavite se\nAko zelite da prekinete vezu, upisite 'Kraj'\n";
    private static final String meniRegistrovanogKorisnika = "1.Broj preostalih karata\n2.Rezervisite obicnu kartu\n3.Rezervisite VIP kartu\n4.Ponisti rezervaciju\nAko zelite da prekinete vezu, upisite 'Kraj'\n";
    private static final String putanjaRezervacijaObicnihKarata = "src/server/Rezervacije.txt";
    private static final String putanjaRezervacijaVIPKarata = "src/server/Rezervacije_VIP.txt";
    private static final String putanjaObicnogCopyFajla = "src/server/Rezervacije_copy.txt";
    private static final String putanjaVIPCopyFajla = "src/server/Rezervacije_VIP_copy.txt";
    private static final String tab = "     ";
    private static final String kraj = "Kraj";

    private boolean registrovanKorisnik = false;
    private int preostaliBrojMogucihRezervacija = 4;

    private BufferedReader tokOdKlijentaTekst = null;
    private PrintWriter tokKaKlijentuTekst = null;
    OutputStream tokKaKlijentuBajtovi = null;
    private final Socket soketZaKom;

    public ServerNit(Socket soket) { // Instanca za novu nit, novog korisnika
        this.soketZaKom = soket;
    }

    @Override
    public void run() {
        try {
            tokOdKlijentaTekst = new BufferedReader(new InputStreamReader(soketZaKom.getInputStream()));
            tokKaKlijentuTekst = new PrintWriter(soketZaKom.getOutputStream(), true);
            tokKaKlijentuBajtovi = soketZaKom.getOutputStream();
            meni(soketZaKom, tokOdKlijentaTekst, tokKaKlijentuTekst);
        } catch (IOException e) {
            System.out.println("Message: " + e.getMessage());
        } finally {
            if (soketZaKom != null) {
                try {
                    soketZaKom.close();
                } catch (IOException e) {
                    System.out.println("Message: " + e.getMessage());
                }
            }
            if (tokKaKlijentuTekst != null) {
                tokKaKlijentuTekst.close();
            }
            if (tokKaKlijentuBajtovi != null) {
                try {
                    tokKaKlijentuBajtovi.close();
                } catch (IOException e) {
                    System.out.println("Message: " + e.getMessage());
                }
            }
            if (tokOdKlijentaTekst != null) {
                try {
                    tokOdKlijentaTekst.close();
                } catch (IOException e) {
                    System.out.println("Message: " + e.getMessage());
                }
            }
        }
    }

    private void meni(Socket s, BufferedReader tokOdKlijentaTekst, PrintWriter tokKaKlijentuTekst) throws IOException {
        String izbor = "";
        if (s.isBound()) {
            tokKaKlijentuTekst.println("Uspesno ste se povezali\n" + meni);
            while (s.isBound()) {
                if (registrovanKorisnik) {
                    izbor = tokOdKlijentaTekst.readLine();
                    switch (izbor) {
                        case "1":
                            proveraSlobodnihKarata(tokKaKlijentuTekst);
                            break;
                        case "2":
                            rezervacijaKarata(tokKaKlijentuTekst, tokOdKlijentaTekst, putanjaRezervacijaObicnihKarata, false);
                            break;
                        case "3":
                            rezervacijaKarata(tokKaKlijentuTekst, tokOdKlijentaTekst, putanjaRezervacijaVIPKarata, true);
                            break;
                        case "4":
                            String izborBrisanja = "";
                            do {
                                tokKaKlijentuTekst
                                        .println("Da li zelite da obrisete:\n1.Obicnu rezervaciju\n2.VIP Rezervaciju\n");
                                izborBrisanja = tokOdKlijentaTekst.readLine();
                            } while (!izborBrisanja.equals("1") && !izborBrisanja.equals("2"));
                            if (izborBrisanja.equals("1")) {
                                brisanjeRezervacije(tokOdKlijentaTekst, tokKaKlijentuTekst, putanjaRezervacijaObicnihKarata,
                                        putanjaObicnogCopyFajla);
                            } else {
                                brisanjeRezervacije(tokOdKlijentaTekst, tokKaKlijentuTekst, putanjaRezervacijaVIPKarata,
                                        putanjaVIPCopyFajla);
                            }
                            break;
                        case "Kraj":
                            s.close();
                            break;

                        default:
                            tokKaKlijentuTekst.println("Izaberite neku od gore navedenih opcija\n");
                            break;
                    }

                } else {
                    izbor = tokOdKlijentaTekst.readLine();
                    switch (izbor) {
                        case "1":
                            proveraSlobodnihKarata(tokKaKlijentuTekst);
                            break;
                        case "2":
                            rezervacijaKarata(tokKaKlijentuTekst, tokOdKlijentaTekst, putanjaRezervacijaObicnihKarata, false);
                            break;
                        case "3":
                            registracijaKorisnika(tokOdKlijentaTekst, tokKaKlijentuTekst);
                            break;
                        case "4":
                            if (prijavaKorisnika(tokOdKlijentaTekst, tokKaKlijentuTekst)) {
                                registrovanKorisnik = true;
                            }
                            break;
                        case "Kraj":
                            s.close();
                            break;
                        default:
                            tokKaKlijentuTekst.println("Izaberite neku od gore navedenih opcija\n");
                            break;
                    }
                }
            }
        }
    }

    private void proveraSlobodnihKarata(PrintWriter tokKaKlijentuTekst) {
        if (registrovanKorisnik) {
            tokKaKlijentuTekst.println("Broj preostalih obicnih karata je: "
                    + brojPreostalihKarata(putanjaRezervacijaObicnihKarata, false) + "\nBroj preostalih VIP karata je: "
                    + brojPreostalihKarata(putanjaRezervacijaVIPKarata, true) + "\n" + meniRegistrovanogKorisnika);
        } else {
            tokKaKlijentuTekst.println("Broj preostalih obicnih karata je: "
                    + brojPreostalihKarata(putanjaRezervacijaObicnihKarata, false) + "\nBroj preostalih VIP karata je: "
                    + brojPreostalihKarata(putanjaRezervacijaVIPKarata, true) + "\n" + meni);
        }
    }

    private void rezervacijaKarata(PrintWriter tokKaKlijentuTekst, BufferedReader tokOdKlijentaTekst, String imeFajla,
                                   boolean VIP) throws IOException {
        if (preostaliBrojMogucihRezervacija > 0) {
            if (brojPreostalihKarata(imeFajla, VIP) > 0) {
                int brojKarataInt = 0;
                if (brojPreostalihKarata(imeFajla, VIP) < 4) {
                    if (preostaliBrojMogucihRezervacija < brojPreostalihKarata(imeFajla, VIP))
                        tokKaKlijentuTekst.println("Koliko karata zelite da rezervisete? Maksimalno mozete "
                                + preostaliBrojMogucihRezervacija + "\n");
                    else
                        tokKaKlijentuTekst.println("Koliko karata zelite da rezervisete? Maksimalno mozete "
                                + brojPreostalihKarata(imeFajla, VIP) + "\n");
                } else {
                    if (preostaliBrojMogucihRezervacija < 4)
                        tokKaKlijentuTekst.println("Koliko karata zelite da rezervisete? Maksimalno mozete "
                                + preostaliBrojMogucihRezervacija + " karte.\n");
                    else
                        tokKaKlijentuTekst.println("Koliko karata zelite da rezervisete? Maksimalno mozete "
                                + maksimalanBrojRezervacija + " karte.\n");
                }
                do {
                    String brojKarata = "";
                    brojKarata = tokOdKlijentaTekst.readLine();
                    if (brojKarata.isBlank()) {
                        tokKaKlijentuTekst.println("Poruka je prazna, molimo unesite broj\n");
                        continue;
                    }
                    if (daLiJeBroj(brojKarata)) {
                        brojKarataInt = Integer.parseInt(brojKarata);
                    } else {
                        tokKaKlijentuTekst.println("Molim Vas upisite broj\n");
                        continue;
                    }
                    if (brojPreostalihKarata(imeFajla, VIP) < 4) {
                        if (preostaliBrojMogucihRezervacija < brojPreostalihKarata(imeFajla, VIP)) {
                            if (brojKarataInt > preostaliBrojMogucihRezervacija) {
                                tokKaKlijentuTekst.println("Ne mozete da rezervisete " + brojKarataInt + ". Maksimalno mozete "
                                        + preostaliBrojMogucihRezervacija + " karte. Unesite ponovo\n");
                                continue;
                            }
                        } else {
                            if (brojKarataInt > brojPreostalihKarata(imeFajla, VIP)) {
                                tokKaKlijentuTekst
                                        .println("Ne mozete da rezervisete " + brojKarataInt + ". Maksimalno mozete "
                                                + brojPreostalihKarata(imeFajla, VIP) + " karte. Unesite ponovo\n");
                                continue;
                            }

                        }
                    } else {
                        if (preostaliBrojMogucihRezervacija < 4) {
                            if (brojKarataInt > preostaliBrojMogucihRezervacija) {
                                tokKaKlijentuTekst.println("Ne mozete da rezervisete " + brojKarataInt
                                        + ". Maksimalno mozete " + preostaliBrojMogucihRezervacija + " karte. Unesite ponovo\n");
                                continue;
                            }
                        } else {
                            if (brojKarataInt > 4) {
                                tokKaKlijentuTekst.println("Ne mozete da rezervisete " + brojKarataInt
                                        + ". Maksimalno mozete " + maksimalanBrojRezervacija + " karte. Unesite ponovo\n");
                                continue;
                            }
                        }
                    }

                    if (brojKarataInt < 1) {
                        tokKaKlijentuTekst.println("Ne mozete da rezervisete " + brojKarataInt
                                + ". Minimalno mozete 1 kartu. Unesite ponovo\n");
                        continue;
                    }
                    break;
                } while (true);
                tokKaKlijentuTekst.println("Unesite vase ime i prezime\n");
                String ime = "";
                ime = tokOdKlijentaTekst.readLine();
                String jmbg = "";
                do {
                    tokKaKlijentuTekst.println("Unesite vas JMBG\n");
                    jmbg = tokOdKlijentaTekst.readLine();
                } while (!proveraJMBG(jmbg));
                tokKaKlijentuTekst.println("Unesite vas mail\n");
                String mail = "";
                mail = tokOdKlijentaTekst.readLine();
                upisiRezervaciju(ime, jmbg, mail, imeFajla, brojKarataInt);
                tokKaKlijentuTekst.println("Rezervisali ste kartu");
                String brojKarata = String.valueOf(brojKarataInt);
                tokKaKlijentuTekst.println(ime + "\t" + jmbg + "\t" + mail + "\t" + brojKarata);
                if (registrovanKorisnik) {
                    tokKaKlijentuTekst.println(meniRegistrovanogKorisnika);
                } else {
                    tokKaKlijentuTekst.println(meni);
                }
                preostaliBrojMogucihRezervacija -= brojKarataInt;
            } else {
                if (registrovanKorisnik) {
                    tokKaKlijentuTekst.println(
                            "Nije preostalo vise slobodnih karata za rezervaciju\n" + meniRegistrovanogKorisnika);
                } else {
                    tokKaKlijentuTekst.println("Nije preostalo vise slobodnih karata za rezervaciju\n" + meni);
                }

            }
        } else {
            if (registrovanKorisnik) {
                tokKaKlijentuTekst
                        .println("Rezervisali ste 4 karte, ne mozete vise od toga\n" + meniRegistrovanogKorisnika);
            } else {
                tokKaKlijentuTekst.println("Rezervisali ste 4 karte, ne mozete vise od toga\n" + meni);
            }
        }
    }

    private void registracijaKorisnika(BufferedReader tokOdKlijentaTekst, PrintWriter tokKaKlijentuTekst)
            throws IOException {
        String username = "";
        do {
            tokKaKlijentuTekst.println("Unesite zeljeno korisnicko ime\n");
            username = tokOdKlijentaTekst.readLine();
        } while (!proveraImena(username));
        tokKaKlijentuTekst.println("Unesite zeljenu lozinku\n");
        String lozinka = "";
        lozinka = tokOdKlijentaTekst.readLine();
        tokKaKlijentuTekst.println("Unesite vase ime i prezime\n");
        String name = "";
        name = tokOdKlijentaTekst.readLine();
        String jmbg = "";
        do {
            tokKaKlijentuTekst.println("Unesite vas JMBG\n");
            jmbg = tokOdKlijentaTekst.readLine();
        } while (!proveraJMBG(jmbg));
        tokKaKlijentuTekst.println("Unesite e-mail adresu\n");
        String mail = "";
        mail = tokOdKlijentaTekst.readLine();
        upisKorisnikaUFajl(username, lozinka, name, jmbg, mail);
        tokKaKlijentuTekst.println("Uspesno ste se registrovali\n" + meni);
    }

    private boolean prijavaKorisnika(BufferedReader tokOdKlijentaTekst, PrintWriter tokKaKlijentuTekst)
            throws IOException {
        tokKaKlijentuTekst.println("Unesite vase korisnicko ime\n");
        String usernameZaPrijavu = "";
        usernameZaPrijavu = tokOdKlijentaTekst.readLine();
        tokKaKlijentuTekst.println("Unesite vasu lozinku\n");
        String lozinkaZaPrijavu = "";
        lozinkaZaPrijavu = tokOdKlijentaTekst.readLine();
        if (proveraPrijave(usernameZaPrijavu, lozinkaZaPrijavu)) {
            tokKaKlijentuTekst.println("Uspesno ste se prijavili\n" + meniRegistrovanogKorisnika);
            return true;
        } else {
            tokKaKlijentuTekst.println("Ime ili sifra su netacni\n" + meni);
            return false;
        }
    }

    private void brisanjeRezervacije(BufferedReader tokOdKlijentaTekst, PrintWriter tokKaKlijentuTekst, String imeFajla,
                                     String imeCopyFajla) throws IOException {
        String procitanaRezervacija = "";
        String imeProcitaneRezervacije = "";
        String ime = "";

        boolean kraj = false;

        int pozicijaRazmaka = 0;
        int brojac = 0;

        tokKaKlijentuTekst.println("Unesite ime na koje ste rezervisali karte!\n");
        ime = tokOdKlijentaTekst.readLine();
        synchronized (ServerNit.class) {
            BufferedReader br = new BufferedReader(new FileReader(imeFajla));
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(imeCopyFajla)));
            while (!kraj) {
                procitanaRezervacija = br.readLine();
                if (procitanaRezervacija == null)
                    kraj = true;
                else {
                    for (int i = 0; i < procitanaRezervacija.length(); i++) {
                        if (procitanaRezervacija.charAt(i) == ' ') {
                            pozicijaRazmaka = i;
                            break;
                        }
                    }
                    for (int i = pozicijaRazmaka + 1; i < procitanaRezervacija.length(); i++) {
                        if (procitanaRezervacija.charAt(i) == ' ') {
                            pozicijaRazmaka = i;
                            break;
                        }
                    }
                    imeProcitaneRezervacije = procitanaRezervacija.substring(0, pozicijaRazmaka);
                    if (!imeProcitaneRezervacije.equals(ime)) {
                        pw.println(procitanaRezervacija);
                    } else {
                        brojac++;
                    }
                }
            }
            if (br != null)
                br.close();
            if (pw != null)
                pw.close();
            br = new BufferedReader(new FileReader(imeCopyFajla));
            pw = new PrintWriter(new BufferedWriter(new FileWriter(imeFajla)));
            kraj = false;
            procitanaRezervacija = "";
            while (!kraj) {
                procitanaRezervacija = br.readLine();
                if (procitanaRezervacija == null)
                    kraj = true;
                else {
                    pw.println(procitanaRezervacija);
                }
            }
            if (br != null)
                br.close();
            if (pw != null)
                pw.close();
        }
        preostaliBrojMogucihRezervacija += brojac;
        tokKaKlijentuTekst.println("Rezervacije su obrisane\n" + meniRegistrovanogKorisnika);
    }

    private boolean proveraImena(String ime) throws IOException {
        for (int i = 0; i < ime.length(); i++) {
            if (ime.charAt(i) == ' ') {
                return false;
            }
        }
        BufferedReader br = null;
        synchronized (ServerNit.class) {
            br = new BufferedReader(new FileReader("src/server/Registrovani_Korisnici.txt"));
            boolean kraj = false;
            String pom = "";
            int pozicijaPrvogRazmaka = 0;
            while (!kraj) {
                pom = br.readLine();
                if (pom == null)
                    kraj = true;
                else {
                    for (int i = 0; i < pom.length(); i++) {
                        if (pom.charAt(i) == ' ') {
                            pozicijaPrvogRazmaka = i;
                            break;
                        }
                    }
                    pom = pom.substring(0, pozicijaPrvogRazmaka);
                    if (pom.equals(ime)) {
                        if (br != null)
                            br.close();
                        return false;
                    }
                }
            }
        }
        if (br != null)
            br.close();
        return true;
    }

    private boolean proveraJMBG(String jmbg) {
        if (jmbg.length() != 13)
            return false;
        for (int i = 0; i < jmbg.length(); i++) {
            if (!Character.isDigit(jmbg.charAt(i)))
                return false;
        }
        return true;
    }

    private void upisKorisnikaUFajl(String username, String password, String name, String jmbg, String mail) {
        PrintWriter pw = null;
        try {
            synchronized (ServerNit.class) {
                pw = new PrintWriter(new BufferedWriter(new FileWriter("src/server/Registrovani_Korisnici.txt", true)));
                pw.println(username + " " + password + " " + name + " " + jmbg + " " + mail);
            }

        } catch (Exception e) {
            System.out.println("Message: " + e.getMessage());
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    private boolean proveraPrijave(String username, String password) {

        String pomUsername = "";
        String pomPassword = "";
        String pom = "";
        BufferedReader br = null;

        for (int i = 0; i < username.length(); i++) {
            if (username.charAt(i) == ' ')
                return false;
        }
        for (int i = 0; i < password.length(); i++) {
            if (password.charAt(i) == ' ')
                return false;
        }

        int pozicijaPrvogRazmaka = 0, pozicijaDrugogRazmaka = 0;
        try {
            synchronized (ServerNit.class) {
                br = new BufferedReader(new FileReader("src/server/Registrovani_Korisnici.txt"));

                boolean kraj = false;
                while (!kraj) {
                    pom = br.readLine();
                    if (pom == null)
                        kraj = true;
                    else {
                        for (int i = 0; i < pom.length(); i++) {
                            if (pom.charAt(i) == ' ') {
                                pozicijaPrvogRazmaka = i;
                                break;
                            }
                        }
                        for (int i = pozicijaPrvogRazmaka + 1; i < pom.length(); i++) {
                            if (pom.charAt(i) == ' ') {
                                pozicijaDrugogRazmaka = i;
                                break;
                            }
                        }
                        pomUsername = pom.substring(0, pozicijaPrvogRazmaka);
                        pomPassword = pom.substring(pozicijaPrvogRazmaka + 1, pozicijaDrugogRazmaka);
                        if (username.equals(pomUsername) && password.equals(pomPassword)) {
                            return true;
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Message: " + e.getMessage());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                System.out.println("Message: " + e.getMessage());
            }
        }
        return false;
    }

    private void upisiRezervaciju(String ime, String jmbg, String mail, String imeFajla, int brojRezervacija) {
        PrintWriter pw = null;
        try {
            synchronized (ServerNit.class) {
                pw = new PrintWriter(new BufferedWriter(new FileWriter(imeFajla, true)));
                for (int i = 0; i < brojRezervacija; i++) {
                    pw.println(ime + " " + jmbg + " " + mail);
                }
            }
        } catch (Exception e) {
            System.out.println("Message: " + e.getMessage());
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    private int brojPreostalihKarata(String imeFajla, boolean VIP) {
        BufferedReader br = null;
        int brojac = 0;
        try {
            synchronized (ServerNit.class) {
                br = new BufferedReader(new FileReader(imeFajla));

                boolean kraj = false;

                while (!kraj) {
                    String pom = br.readLine();
                    if (pom == null)
                        kraj = true;
                    else
                        brojac++;
                }
            }
        } catch (Exception e) {
            System.out.println("Message: " + e.getMessage());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                System.out.println("Message: " + e.getMessage());
            }
        }
        if (!VIP) {
            return ukupanBrojKarata - brojac - vipKarte;
        } else {
            return vipKarte - brojac;
        }

    }

    private boolean daLiJeBroj(String broj) {
        for (int i = 0; i < broj.length(); i++) {
            if (!Character.isDigit(broj.charAt(i)))
                return false;
        }
        return true;
    }
}
