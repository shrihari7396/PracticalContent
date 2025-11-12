package FinalPractice.B.Three;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MemoryPlacementSimulator {

    // ---------- PROCESS CLASS ----------
    private static class Process {
        public int memoryId;
        public String name;
        public boolean isAllocated;
        public int sizeRequired;

        // Constructor for input
        public Process(int i, Scanner sc) {
            System.out.printf("Enter %d th Process info:- \n", i);
            System.out.print("Enter process name: ");
            this.name = sc.nextLine();
            System.out.print("Enter size Required IN Memory: ");
            this.sizeRequired = sc.nextInt();
            sc.nextLine();
            this.memoryId = -1;
            this.isAllocated = false;
        }

        // Copy constructor
        public Process(Process p) {
            this.name = p.name;
            this.sizeRequired = p.sizeRequired;
            this.memoryId = -1;
            this.isAllocated = false;
        }

        @Override
        public String toString() {
            return "Process [memoryId=" + memoryId + ", name=" + name + ", isAllocated=" + isAllocated
                    + ", sizeRequired=" + sizeRequired + "]";
        }
    }

    // ---------- MEMORY BLOCK CLASS ----------
    private static class MemoryBlock {
        public int index;
        public int size;

        // Constructor for input
        MemoryBlock(int index, Scanner sc) {
            System.out.printf("Enter %d th Memory Block info:- \n", index);
            System.out.print("Enter Size: ");
            this.size = sc.nextInt();
            sc.nextLine();
            this.index = index;
        }

        // Copy constructor
        MemoryBlock(MemoryBlock b) {
            this.index = b.index;
            this.size = b.size;
        }

        @Override
        public String toString() {
            return "MemoryBlock [index=" + index + ", size=" + size + "]";
        }
    }

    // ---------- GLOBAL DATA ----------
    private static final List<Process> processes = new ArrayList<>();
    private static final List<MemoryBlock> memoryBlocks = new ArrayList<>();

    // ---------- MAIN METHOD ----------
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter Number of Processes: ");
        int numberOfProcess = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter Number of Memory Blocks: ");
        int numberOfMemoryBlocks = sc.nextInt();
        sc.nextLine();

        System.out.println("\n--- Enter Process Information ---");
        for (int i = 0; i < numberOfProcess; i++) {
            processes.add(new Process(i + 1, sc));
            System.out.println();
        }

        System.out.println("--- Enter Memory Block Information ---");
        for (int i = 0; i < numberOfMemoryBlocks; i++) {
            memoryBlocks.add(new MemoryBlock(i + 1, sc));
            System.out.println();
        }

        // Menu-driven loop
        while (true) {
            Menu();
            int choice = sc.nextInt();

            // Deep copies for independent simulation
            List<Process> processCopy = new ArrayList<>();
            for (Process p : processes) processCopy.add(new Process(p));

            List<MemoryBlock> memoryCopy = new ArrayList<>();
            for (MemoryBlock b : memoryBlocks) memoryCopy.add(new MemoryBlock(b));

            switch (choice) {
                case 1 -> {
                    System.out.println("\n===== FIRST FIT ALLOCATION =====");
                    firstFit(processCopy, memoryCopy);
                }
                case 2 -> {
                    System.out.println("\n===== NEXT FIT ALLOCATION =====");
                    nextFit(processCopy, memoryCopy);
                }
                case 3 -> {
                    System.out.println("\n===== BEST FIT ALLOCATION =====");
                    bestFit(processCopy, memoryCopy);
                }
                case 4 -> {
                    System.out.println("\n===== WORST FIT ALLOCATION =====");
                    worstFit(processCopy, memoryCopy);
                }
                case 5 -> {
                    System.out.println("Exiting...");
                    sc.close();
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // ---------- MENU ----------
    private static void Menu() {
        System.out.println("\nMemory Placement Strategies:- ");
        System.out.println("1. First Fit");
        System.out.println("2. Next Fit");
        System.out.println("3. Best Fit");
        System.out.println("4. Worst Fit");
        System.out.println("5. Exit");
        System.out.print("Enter your choice: ");
    }

    // ---------- FIRST FIT ----------
    private static void firstFit(List<Process> processes, List<MemoryBlock> memoryBlocks) {
        for (Process process : processes) {
            if (process.memoryId != -1) continue;

            for (MemoryBlock block : memoryBlocks) {
                if (block.size >= process.sizeRequired) {
                    block.size -= process.sizeRequired;
                    process.memoryId = block.index;
                    process.isAllocated = true;
                    break;
                }
            }
        }

        displayResults(processes, memoryBlocks);
    }

    // ---------- NEXT FIT ----------
    private static void nextFit(List<Process> processes, List<MemoryBlock> memoryBlocks) {
        int prevIndex = 0;
        int n = memoryBlocks.size();

        for (Process process : processes) {
            if (process.memoryId != -1) continue;

            boolean allocated = false;
            int count = 0;
            int i = prevIndex;

            while (count < n) {
                MemoryBlock block = memoryBlocks.get(i);
                if (block.size >= process.sizeRequired) {
                    block.size -= process.sizeRequired;
                    process.memoryId = block.index;
                    process.isAllocated = true;
                    prevIndex = i;
                    allocated = true;
                    break;
                }
                i = (i + 1) % n;
                count++;
            }
        }

        displayResults(processes, memoryBlocks);
    }

    // ---------- BEST FIT ----------
    private static void bestFit(List<Process> processes, List<MemoryBlock> memoryBlocks) {
        for (Process process : processes) {
            if (process.memoryId != -1) continue;

            int bestIndex = -1;
            int minRemaining = Integer.MAX_VALUE;

            for (int i = 0; i < memoryBlocks.size(); i++) {
                MemoryBlock block = memoryBlocks.get(i);
                if (block.size >= process.sizeRequired && (block.size - process.sizeRequired) < minRemaining) {
                    minRemaining = block.size - process.sizeRequired;
                    bestIndex = i;
                }
            }

            if (bestIndex != -1) {
                MemoryBlock chosen = memoryBlocks.get(bestIndex);
                chosen.size -= process.sizeRequired;
                process.memoryId = chosen.index;
                process.isAllocated = true;
            }
        }

        displayResults(processes, memoryBlocks);
    }

    // ---------- WORST FIT ----------
    private static void worstFit(List<Process> processes, List<MemoryBlock> memoryBlocks) {
        for (Process process : processes) {
            if (process.memoryId != -1) continue;

            int worstIndex = -1;
            int maxRemaining = -1;

            for (int i = 0; i < memoryBlocks.size(); i++) {
                MemoryBlock block = memoryBlocks.get(i);
                if (block.size >= process.sizeRequired && (block.size - process.sizeRequired) > maxRemaining) {
                    maxRemaining = block.size - process.sizeRequired;
                    worstIndex = i;
                }
            }

            if (worstIndex != -1) {
                MemoryBlock chosen = memoryBlocks.get(worstIndex);
                chosen.size -= process.sizeRequired;
                process.memoryId = chosen.index;
                process.isAllocated = true;
            }
        }

        displayResults(processes, memoryBlocks);
    }

    // ---------- DISPLAY RESULTS ----------
    private static void displayResults(List<Process> processes, List<MemoryBlock> memoryBlocks) {
        System.out.println("\n================ Allocation Summary ================");
        System.out.printf("%-10s %-15s %-15s %-15s%n", "Process", "Size Required", "Allocated Block", "Status");
        System.out.println("---------------------------------------------------");
        for (Process p : processes) {
            String block = (p.memoryId == -1) ? "-" : "Block " + p.memoryId;
            String status = p.isAllocated ? "Allocated" : "Not Allocated";
            System.out.printf("%-10s %-15d %-15s %-15s%n", p.name, p.sizeRequired, block, status);
        }

        System.out.println("\nMemory Block Info (After Allocation):");
        System.out.printf("%-10s %-15s%n", "Block", "Remaining Size");
        System.out.println("-------------------------------------");
        for (MemoryBlock b : memoryBlocks) {
            System.out.printf("%-10s %-15d%n", "B" + b.index, b.size);
        }
        System.out.println("====================================================\n");
    }
}
