// ui/ConsoleUI.java
package ui;

import java.util.Scanner;

public class ConsoleUI {

    private final Scanner in = new Scanner(System.in);

    public void start() {
        while (true) {
            System.out.println("""
                === S-Emulator ===
                1) Load program   [לא ממומש עדיין]
                2) Show program   [דמו עובד]
                3) Expand         [לא ממומש עדיין]
                4) Run            [לא ממומש עדיין]
                5) History        [לא ממומש עדיין]
                6) Exit
                Choose [1-6]:""");

            String choice = in.nextLine().trim();
            switch (choice) {
                case "2" -> onShow();
                case "6" -> { System.out.println("Bye."); return; }
                case "1", "3", "4", "5" -> System.out.println("עדיין לא ממומש בשלב זה.");
                default -> System.out.println("בחירה לא חוקית.");
            }
        }
    }

    private void onShow() {
        // כאן אנחנו "מעמידים פנים" שכבר נטען XML ונפרס – באמצעות TempProgramFactory
        TempProgramFactory.TempProgram temp = TempProgramFactory.sample();
        String output = PrettyPrinter.show(temp);
        System.out.println(output);
    }
}
