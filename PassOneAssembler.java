import java.io.*;
import java.util.*;

class NoMnemonicException extends Exception {
    public NoMnemonicException(String message) {
        super(message);
    }
}

class Pair<T, V> {
    public T first;
    public V second;

    public Pair(T first, V second) {
        this.first = first;
        this.second = second;
    }
}

class Literal {
    String value;
    Integer address; // -1 when not assigned

    Literal(String value, Integer address) {
        this.value = value;
        this.address = address;
    }
}

public class PassOneAssembler {
    private static final Map<String, Pair<String, Integer>> OPTAB = new HashMap<>();
    private static final Map<String, Integer> REGISTER = new HashMap<>();
    private static final Map<String, Integer> CONDITION = new HashMap<>();
    private static final Map<String, Pair<String, Integer>> SYMBOL_TABLE = new LinkedHashMap<>();

    // Literal structures
    private static final List<Literal> LITTAB = new ArrayList<>(); // all literals assigned addresses
    private static final List<Literal> PENDING_LITERALS = new ArrayList<>(); // literals waiting for LTORG/END
    private static final List<Integer> POOLTAB = new ArrayList<>(); // indexes into LITTAB (pool start indices)

    private static boolean isLiteral(String operand) {
        return operand != null && operand.startsWith("=");
    }

    // Normalize literal value removing = and optional quotes: =5 or ='A' or ="ABC"
    private static String extractLiteralValue(String operand) {
        if (!isLiteral(operand))
            return null;
        String lit = operand.substring(1).trim();
        if ((lit.startsWith("'") && lit.endsWith("'")) || (lit.startsWith("\"") && lit.endsWith("\""))) {
            lit = lit.substring(1, lit.length() - 1);
        }
        return lit;
    }

    // search both pending and assigned LITTAB for a literal value, return index in
    // combined space
    // We will treat returned index as 0-based index in the combined list (PENDING
    // first, then LITTAB).
    private static int getLiteralGlobalIndex(String literalValue) {
        // Search assigned LITTAB
        for (int i = 0; i < LITTAB.size(); i++) {
            if (LITTAB.get(i).value.equals(literalValue))
                return i; // index in LITTAB (0-based)
        }
        // Search pending
        for (int i = 0; i < PENDING_LITERALS.size(); i++) {
            if (PENDING_LITERALS.get(i).value.equals(literalValue))
                return LITTAB.size() + i;
        }
        return -1;
    }

    /**
     * When LTORG or END encountered:
     * - assign addresses to all pending literals (in order),
     * - move them into LITTAB,
     * - record pool start in POOLTAB
     * Returns number of literals placed (so caller can increment LC).
     */
    private static int processLTORG(int currentLC) {
        if (PENDING_LITERALS.isEmpty())
            return 0;
        POOLTAB.add(LITTAB.size()); // start index of this pool in final LITTAB
        int LC = currentLC;
        for (Literal l : PENDING_LITERALS) {
            l.address = LC;
            LITTAB.add(new Literal(l.value, l.address));
            LC++;
        }
        int placed = PENDING_LITERALS.size();
        PENDING_LITERALS.clear();
        return placed;
    }

    private static void initializeWithData() {
        // REGISTERS
        REGISTER.put("AREG", 1);
        REGISTER.put("BREG", 2);
        REGISTER.put("CREG", 3);
        REGISTER.put("DREG", 4);

        // CONDITION CODES
        CONDITION.put("LT", 1);
        CONDITION.put("LE", 2);
        CONDITION.put("EQ", 3);
        CONDITION.put("GT", 4);
        CONDITION.put("GE", 5);
        CONDITION.put("ANY", 6);

        // OPTAB
        OPTAB.put("STOP", new Pair<>("IS", 0));
        OPTAB.put("ADD", new Pair<>("IS", 1));
        OPTAB.put("MULTI", new Pair<>("IS", 2)); // keep as you had it
        OPTAB.put("SUB", new Pair<>("IS", 3));
        OPTAB.put("MOVER", new Pair<>("IS", 4));
        OPTAB.put("MOVEM", new Pair<>("IS", 5));
        OPTAB.put("COMP", new Pair<>("IS", 6));
        OPTAB.put("BC", new Pair<>("IS", 7));
        OPTAB.put("DIV", new Pair<>("IS", 8));
        OPTAB.put("READ", new Pair<>("IS", 9));
        OPTAB.put("PRINT", new Pair<>("IS", 10));

        // ASSEMBLER DIRECTIVES
        OPTAB.put("START", new Pair<>("AD", 1));
        OPTAB.put("END", new Pair<>("AD", 2));
        OPTAB.put("ORIGIN", new Pair<>("AD", 3));
        OPTAB.put("EQU", new Pair<>("AD", 4));
        OPTAB.put("LTORG", new Pair<>("AD", 5));

        // DECLARATIVE
        OPTAB.put("DC", new Pair<>("DL", 1));
        OPTAB.put("DS", new Pair<>("DL", 2));
    }

    /**
     * parseFile: read input assembly, write intermediate code, write symbol table
     * and literal table to files.
     *
     * @param inputAsmFilePath       input .asm file path
     * @param symbolTableFile        symbol table output file path
     * @param literalTableFile       literal table output file path
     * @param outputIntermediateFile intermediate output file path
     */
    private static void parseFile(String inputAsmFilePath, String symbolTableFile, String literalTableFile,
            String outputIntermediateFile) throws IOException, NoMnemonicException {
        try (BufferedReader inputAsmFileReader = new BufferedReader(new FileReader(new File(inputAsmFilePath)));
                BufferedWriter outputIntermediateWriter = new BufferedWriter(new FileWriter(new File(outputIntermediateFile)))) {

            String codeline;
            int LC = 100;
            boolean endFound = false;

            // Make sure POOLTAB contains initial pool start at 0
            POOLTAB.add(0);

            while ((codeline = inputAsmFileReader.readLine()) != null && !endFound) {
                codeline = codeline.trim();
                if (codeline.isEmpty())
                    continue;

                // split into at most 3 parts: label(optional), mnemonic, rest (operands)
                String[] parts = codeline.split("\\s+", 3);

                String label = null;
                int startIndex = 0;

                // Detect if first token is a label (not in OPTAB, REG, COND)
                if (parts.length > 0 && !OPTAB.containsKey(parts[0].toUpperCase())
                        && !REGISTER.containsKey(parts[0].toUpperCase())
                        && !CONDITION.containsKey(parts[0].toUpperCase())) {
                    label = parts[0];
                    startIndex = 1;
                }

                String mnemonic = null;
                String operandsPart = null;
                if (startIndex < parts.length) {
                    mnemonic = parts[startIndex].toUpperCase();
                    if (startIndex + 1 < parts.length)
                        operandsPart = parts[startIndex + 1];
                } else if (parts.length >= 2) {
                    mnemonic = parts[1].toUpperCase();
                    if (parts.length == 3)
                        operandsPart = parts[2];
                }

                // If label present, update symbol table (if new or forward referenced)
                if (label != null && !label.isEmpty()) {
                    Pair<String, Integer> existing = SYMBOL_TABLE.get(label);
                    if (existing == null || (existing.second == 0)) {
                        SYMBOL_TABLE.put(label, new Pair<>("S", LC));
                    }
                }

                if (mnemonic == null)
                    continue;

                // START directive
                if (mnemonic.equals("START")) {
                    if (operandsPart != null) {
                        try {
                            LC = Integer.parseInt(operandsPart);
                        } catch (NumberFormatException e) {
                            /* default keep LC */ }
                        outputIntermediateWriter.write(LC + " (AD,01) (C," + operandsPart + ")");
                    } else {
                        outputIntermediateWriter.write("(AD,01)");
                    }
                    outputIntermediateWriter.newLine();
                    continue;
                }

                // END directive: assign pending literals and write AD
                if (mnemonic.equals("END")) {
                    // assign pending literals at end
                    int added = processLTORG(LC);
                    LC += added;
                    outputIntermediateWriter.write("(AD,02)");
                    outputIntermediateWriter.newLine();
                    endFound = true;
                    break;
                }

                // LTORG directive: place pending literals at current LC
                if (mnemonic.equals("LTORG")) {
                    int added = processLTORG(LC);
                    LC += added;
                    outputIntermediateWriter.write(LC + " (AD,05)");
                    outputIntermediateWriter.newLine();
                    continue;
                }

                Pair<String, Integer> optabRow = OPTAB.get(mnemonic);
                if (optabRow == null) {
                    throw new NoMnemonicException("Mnemonic not found: " + mnemonic);
                }

                // Handle declarations DC/DS
                if (mnemonic.equals("DS") || mnemonic.equals("DC")) {
                    if (label != null && !label.isEmpty()) {
                        SYMBOL_TABLE.put(label, new Pair<>("S", LC));
                    }
                    // output the declarative
                    outputIntermediateWriter.write(LC + " (DL," + String.format("%02d", optabRow.second) + ") ");
                    if (operandsPart != null) {
                        String op = operandsPart.trim();
                        if (mnemonic.equals("DC")) {
                            // normalize constants: remove quotes if present
                            op = op.replaceAll("^['\"]|['\"]$", "");
                            outputIntermediateWriter.write("(C," + op + ")");
                        } else { // DS
                            outputIntermediateWriter.write("(C," + op + ")");
                        }
                    }
                    outputIntermediateWriter.newLine();

                    // update LC: DS reserves 'n' words, DC reserves 1
                    if (mnemonic.equals("DS")) {
                        int size = 1;
                        try {
                            size = Integer.parseInt(operandsPart.trim());
                        } catch (Exception e) {
                            size = 1;
                        }
                        LC += size;
                    } else { // DC
                        LC += 1;
                    }
                    continue;
                }

                // Normal imperative or other directive
                // We'll form encoded/intermediate line
                LC++;
                StringBuilder encodedLine = new StringBuilder();
                encodedLine.append(LC).append(" ");
                encodedLine.append("(").append(optabRow.first).append(",")
                        .append(String.format("%02d", optabRow.second)).append(") ");

                // handle operands: support "op1,op2" or single
                String op1 = null, op2 = null;
                if (operandsPart != null) {
                    String[] ops = operandsPart.split(",", 2);
                    op1 = ops[0].trim();
                    if (ops.length > 1)
                        op2 = ops[1].trim();
                }

                // operand 1 encoding
                if (op1 != null && !op1.isEmpty()) {
                    if (REGISTER.containsKey(op1.toUpperCase())) {
                        encodedLine.append("(R,").append(REGISTER.get(op1.toUpperCase())).append(") ");
                    } else if (CONDITION.containsKey(op1.toUpperCase())) {
                        encodedLine.append("(CC,").append(CONDITION.get(op1.toUpperCase())).append(") ");
                    } else if (isLiteral(op1)) {
                        String literalValue = extractLiteralValue(op1);
                        int gIndex = getLiteralGlobalIndex(literalValue);
                        if (gIndex == -1) {
                            // new pending literal
                            PENDING_LITERALS.add(new Literal(literalValue, -1));
                            gIndex = LITTAB.size() + PENDING_LITERALS.size() - 1;
                        }
                        // Use L index relative to entire LITTAB (1-based in output)
                        encodedLine.append("(L,").append(gIndex + 1).append(") ");
                    } else if (SYMBOL_TABLE.containsKey(op1)) {
                        Pair<String, Integer> sym = SYMBOL_TABLE.get(op1);
                        encodedLine.append("(").append(sym.first).append(",").append(sym.second).append(") ");
                    } else {
                        // immediate numeric constant?
                        try {
                            int val = Integer.parseInt(op1);
                            encodedLine.append("(C,").append(val).append(") ");
                        } catch (NumberFormatException e) {
                            // forward symbol reference: put placeholder in symbol table
                            SYMBOL_TABLE.putIfAbsent(op1, new Pair<>("S", 0));
                            encodedLine.append("(S,").append(0).append(") ");
                        }
                    }
                }

                // operand 2 encoding (similar)
                if (op2 != null && !op2.isEmpty()) {
                    if (REGISTER.containsKey(op2.toUpperCase())) {
                        encodedLine.append("(R,").append(REGISTER.get(op2.toUpperCase())).append(") ");
                    } else if (CONDITION.containsKey(op2.toUpperCase())) {
                        encodedLine.append("(CC,").append(CONDITION.get(op2.toUpperCase())).append(") ");
                    } else if (isLiteral(op2)) {
                        String literalValue = extractLiteralValue(op2);
                        int gIndex = getLiteralGlobalIndex(literalValue);
                        if (gIndex == -1) {
                            PENDING_LITERALS.add(new Literal(literalValue, -1));
                            gIndex = LITTAB.size() + PENDING_LITERALS.size() - 1;
                        }
                        encodedLine.append("(L,").append(gIndex + 1).append(") ");
                    } else if (SYMBOL_TABLE.containsKey(op2)) {
                        Pair<String, Integer> sym = SYMBOL_TABLE.get(op2);
                        encodedLine.append("(").append(sym.first).append(",").append(sym.second).append(") ");
                    } else {
                        try {
                            int val = Integer.parseInt(op2);
                            encodedLine.append("(C,").append(val).append(") ");
                        } catch (NumberFormatException e) {
                            SYMBOL_TABLE.putIfAbsent(op2, new Pair<>("S", 0));
                            encodedLine.append("(S,").append(0).append(") ");
                        }
                    }
                }

                outputIntermediateWriter.write(encodedLine.toString().trim());
                outputIntermediateWriter.newLine();
            } // while

            // After loop if END wasn't encountered explicitly, process pending literals
            if (!PENDING_LITERALS.isEmpty()) {
                int added = processLTORG(LC);
                // Optionally update LC further if you plan to use it later
            }
        }

        // write symbol table and literal table to provided files (outside main try so
        // we can catch IO separately)
        try (BufferedWriter symbolWriter = new BufferedWriter(new FileWriter(new File(symbolTableFile)));
                BufferedWriter literalWriter = new BufferedWriter(new FileWriter(new File(literalTableFile)))) {

            symbolWriter.write("SymbolTable (Name -> Address)\n");
            for (Map.Entry<String, Pair<String, Integer>> e : SYMBOL_TABLE.entrySet()) {
                symbolWriter.write(e.getKey() + " -> " + e.getValue().second + "\n");
            }

            literalWriter.write("LITTAB (Index -> Literal, Address)\n");
            for (int i = 0; i < LITTAB.size(); i++) {
                Literal lit = LITTAB.get(i);
                literalWriter.write((i + 1) + " -> =" + lit.value + " , " + lit.address + "\n");
            }

            // Write POOLTAB
            literalWriter.write("\nPOOLTAB (pool start indices in LITTAB)\n");
            for (int p = 0; p < POOLTAB.size(); p++) {
                literalWriter.write("Pool " + p + " start index = " + (POOLTAB.get(p) + 1) + "\n");
            }
        }
    }

    public static void main(String[] args) {
        initializeWithData();
        // Example call: adapt the filenames
        try {
            parseFile("shri.txt", "SymbolTable.txt", "LiteralTable.txt", "IntermediateFile.txt");
            System.out.println("Pass 1 completed. Check IntermediateFile.txt, SymbolTable.txt, LiteralTable.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
