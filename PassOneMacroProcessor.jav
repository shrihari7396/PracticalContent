package FinalPractice.A.Three;

import java.io.*;
import java.util.*;

public class PassOneMacroProccessor {

    // ---------------- New Data Structure ----------------
    private static class Param {
        public String name;
        public boolean hasDefault;
        public String defaultValue;
        Param(String name, boolean hasDefault, String defaultValue) {
            this.name = name;
            this.hasDefault = hasDefault;
            this.defaultValue = defaultValue;
        }
    }

    private static final Map<String, Pair<Integer, Pair<Integer, Integer>>> MNT = new HashMap<>();
    private static final List<String> MDT = new ArrayList<>();
    private static final List<Param> ALA = new ArrayList<>(); // unified param table

    // ---------------- Display Tables ----------------
    private static void displayAllData(BufferedWriter writer) throws IOException {
        writer.write("\n--------------------------------- MNT TABLE ---------------------------------\n");
        writer.write(String.format("%-15s | %-10s | %-10s | %-10s\n", "Macro", "MDTIndex", "StartALA", "EndALA"));
        for (Map.Entry<String, Pair<Integer, Pair<Integer, Integer>>> it : MNT.entrySet()) {
            writer.write(String.format("%-15s | %-10d | %-10d | %-10d\n",
                    it.getKey(), it.getValue().first, it.getValue().second.first, it.getValue().second.second));
        }

        writer.write("\n--------------------------------- MDT TABLE ---------------------------------\n");
        int index = 0;
        for (String line : MDT)
            writer.write(String.format("%-3d : %s\n", index++, line));

        writer.write("\n--------------------------------- ALA TABLE ---------------------------------\n");
        index = 0;
        for (Param p : ALA)
            writer.write(String.format("%-3d : %-10s | hasDefault: %-5s | default: %-10s\n",
                    index++, p.name, p.hasDefault, p.defaultValue));
    }

    // ---------------- Main Logic ----------------
    public static void main(String[] args1) throws IOException {
        BufferedReader reader = new BufferedReader(
                new FileReader("C:/Users/hp/Downloads/31143/LP-I/FinalPractice/A/Three/Input.asm"));
        BufferedWriter writer = new BufferedWriter(
                new FileWriter("C:/Users/hp/Downloads/31143/LP-I/FinalPractice/A/Three/Output.asm"));

        boolean inMacro = false, firstLine = false;
        String macroName = null, line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] split = line.split("\\s+");

            if (split[0].equalsIgnoreCase("MACRO")) {
                inMacro = true;
                firstLine = true;
                continue;
            } else if (firstLine) {
                macroName = split[0].trim();
                String arguments = (split.length > 1) ? split[1].trim() : "";
                String[] args = arguments.isEmpty() ? new String[0] : arguments.split(",");

                int start = ALA.size();

                for (String it : args) {
                    it = it.trim();
                    if (it.isEmpty()) continue;

                    if (it.contains("=")) {
                        String[] kv = it.split("=");
                        String key = kv[0].trim();
                        String val = (kv.length > 1) ? kv[1].trim() : "-";
                        ALA.add(new Param(key, true, val));
                    } else {
                        ALA.add(new Param(it, false, "-"));
                    }
                }

                int end = ALA.size() - 1;
                MNT.put(macroName, new Pair<>(MDT.size(), new Pair<>(start, end)));

                firstLine = false;
            } else if (inMacro) {
                if (split[0].equalsIgnoreCase("MEND")) {
                    MDT.add("MEND");
                    inMacro = false;
                    macroName = null;
                    continue;
                }

                String temp = line;
                for (int i = 0; i < ALA.size(); i++) {
                    if (temp.contains(ALA.get(i).name)) {
                        temp = temp.replace(ALA.get(i).name, "(P," + i + ")");
                    }
                }
                MDT.add(temp);
            }
        }

        displayAllData(writer);
        reader.close();
        writer.close();
        System.out.println("✅ Pass One Completed Successfully.");
    }

    // Simple Pair Helper (unchanged)
    private static class Pair<T, V> {
        T first;
        V second;
        Pair(T first, V second) {
            this.first = first;
            this.second = second;
        }
    }
}
    