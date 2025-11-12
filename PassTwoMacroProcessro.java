package FinalPractice.A.Four;

import java.io.*;
import java.util.*;

public class PassTwoMacroProcessor {

    // ---------- DATA STRUCTURES ----------
    private static final Map<String, Integer> MNT = new HashMap<>(); // Macro name + MDT index
    private static final List<String> MDT = new ArrayList<>();       // Macro body
    private static final List<String> ALA = new ArrayList<>();       // Formal arguments

    public static void main(String[] args1) {
        try {
            // ---------- LOAD TABLES ----------
            loadMNT("MNT.txt");
            loadMDT("MDT.txt");
            loadALA("ALA.txt");

            // ---------- PROCESS SOURCE ----------
            BufferedReader srcReader = new BufferedReader(new FileReader("Source.txt"));
            BufferedWriter outWriter = new BufferedWriter(new FileWriter("ExpandedCode.txt"));

            String line;
            System.out.println("----- PASS TWO MACRO PROCESSOR OUTPUT -----\n");

            while ((line = srcReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] tokens = line.split("\\s+", 2);
                String name = tokens[0];

                if (MNT.containsKey(name)) {
                    // It's a macro call
                    int mdtIndex = MNT.get(name); // Get MDT start index
                    List<String> actualArgs = new ArrayList<>();

                    // Split arguments from call (e.g., A, B)
                    if (tokens.length > 1) {
                        String[] args = tokens[1].split(",");
                        for (String arg : args)
                            actualArgs.add(arg.trim());
                    }

                    expandMacro(mdtIndex, actualArgs, outWriter);
                } else {
                    // Normal statement
                    outWriter.write(line + "\n");
                    System.out.println(line);
                }
            }

            srcReader.close();
            outWriter.close();

            System.out.println("\n----- MACRO EXPANSION COMPLETE -----");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- EXPAND MACRO ----------
    private static void expandMacro(int startIndex, List<String> actualArgs, BufferedWriter outWriter) throws IOException {
        for (int i = startIndex; i < MDT.size(); i++) {
            String bodyLine = MDT.get(i).trim();

            if (bodyLine.equalsIgnoreCase("MEND"))
                break;

            // Replace positional parameters (#1, #2, etc.) with actual args
            for (int j = 0; j < actualArgs.size(); j++) {
                bodyLine = bodyLine.replace("#" + (j + 1), actualArgs.get(j));
            }

            outWriter.write(bodyLine + "\n");
            System.out.println(bodyLine);
        }
    }

    // ---------- LOAD TABLES ----------
    private static void loadMNT(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty() || line.startsWith("MACRO_NAME")) continue;
            String[] parts = line.trim().split("\\s+");
            MNT.put(parts[0], Integer.parseInt(parts[1]));
        }
        br.close();
    }

    private static void loadMDT(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            MDT.add(line.trim());
        }
        br.close();
    }

    private static void loadALA(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            ALA.add(line.trim());
        }
        br.close();
    }
}
