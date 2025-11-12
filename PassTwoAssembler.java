package FinalPractice.A.Two;

import java.io.*;
import java.lang.foreign.GroupLayout;
import java.sql.Connection;
import java.util.*;

class Symbol {
    String name;
    int addr;

    Symbol(String name, int addr) {
        this.name = name;
        this.addr = addr;
    }
}

class Literal {
    String name;
    int addr;

    Literal(String name, int addr) {
        this.name = name;
        this.addr = addr;
    }
}

public class Pass2Assembler {

    public static void main(String[] args) {
        try {
            // ✅ Corrected file path (must use double backslashes or forward slashes)
            BufferedReader br = new BufferedReader(
                new FileReader("C:/Users/hp/Downloads/31143/LP-I/FinalPractice/A/Two/Intermediate.o")
            );

            String line; 
            // ----- SYMBOL TABLE -----
            // You can adjust addresses as per your pass 1 output
            List<Symbol> symtab = Arrays.asList(
                new Symbol("ONE", 205),
                new Symbol("TWO", 206)
            );

            // ----- LITERAL TABLE (if any) -----
            List<Literal> littab = new ArrayList<>();

            System.out.println("----- PASS 2 OUTPUT (Machine Code) -----\n");

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // skip empty lines
                String[] tokens = line.trim().split("\\s+");

                if (tokens.length == 0) continue;

                String token1 = tokens[0];

                // ---------- Assembler Directives ----------
                if (token1.contains("(AD")) {
                    // Skip assembler directives like START, END
                    continue;
                }

                // ---------- Declarative Statements ----------
                else if (token1.contains("(DL,02)")) {
                    // DS - reserve space, no machine code
                    continue;
                }

                else if (token1.contains("(DL,01)")) {
                    // DC - declare constant
                    String constant = tokens[1].substring(3, tokens[1].length() - 1);
                    System.out.println("00 00 " + constant);
                    continue; // skip further processing for this line
                }

                // ---------- Imperative Statements ----------
                else if (token1.contains("(IS")) {
                    String opcode = token1.substring(4, 6); // Extract opcode digits
                    String reg = "0";
                    String mem = "000";

                    // Parse register if present
                    for (String t : tokens) {
                        if (t.contains("(R")) {
                            reg = t.substring(3, t.length() - 1);
                        }
                    }

                    // Parse symbol or constant
                    for (String t : tokens) {
                        if (t.contains("(S")) {
                            // Extract the symbol number (e.g., (S,205))
                            int symbolAddress = Integer.parseInt(t.substring(3, t.length() - 1));
                            mem = String.valueOf(symbolAddress);
                        } else if (t.contains("(C")) {
                            mem = t.substring(3, t.length() - 1);
                        }
                    }

                    System.out.println(opcode + " " + reg + " " + mem);
                }
            }

            br.close();
            System.out.println("\n----- END OF PASS 2 -----");

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
