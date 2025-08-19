package ui;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private final ProgramGateway gw;
    private final Scanner sc = new Scanner(System.in);

    public ConsoleUI(ProgramGateway gw) { this.gw = gw; }

    public void start() {
        while (true) {
            System.out.println("""
                === S-Emulator ===
                1) Load program
                2) Show program
                3) Expand
                4) Run
                5) History
                6) Exit
                Choose [1-6]:""");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1" -> onLoad();
                case "2" -> System.out.println(gw.show());
                case "3" -> onExpand();
                case "4" -> onRun();
                case "5" -> onHistory();
                case "6" -> { System.out.println("Bye."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void onLoad() {
        System.out.print("Enter XML path: ");
        var res = gw.load(Path.of(sc.nextLine().trim()));
        System.out.println(res.message());
    }

    private void onExpand() {
        int level = askInt("Expansion level (0..): ");
        System.out.println(gw.expand(level));
    }

    private void onRun() {
        int level = askInt("Run level (0..): ");
        System.out.print("Inputs CSV (e.g., 4,7,0): ");
        var res = gw.run(level, sc.nextLine().trim());
        System.out.printf("y = %d%nc ycles = %d%n", res.y(), res.cycles());
    }

    private void onHistory() {
        List<ProgramGateway.RunResult> hist = gw.history();
        if (hist.isEmpty()) { System.out.println("No runs yet."); return; }
        for (var h : hist) {
            System.out.printf("#%d | level=%d | inputs=%s | y=%d | cycles=%d%n",
                    h.runNo(), h.level(), h.inputsCsv(), h.y(), h.cycles());
        }
    }

    private int askInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Please enter a number."); }
        }
    }
}
