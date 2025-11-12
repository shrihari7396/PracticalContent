package FinalPractice.B.one;

import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class ProducerConsumer {
    private static int in = 0, out = 0;
    private static int BUFFER_SIZE;
    private static int ITEMS_PER_PRODUCER;

    private static int[] buffer;
    private static final Object mutex = new Object();
    private static final Random rand = new Random();

    private static Semaphore empty, full;
    private static final AtomicInteger producedCount = new AtomicInteger(0);
    private static final AtomicInteger consumedCount = new AtomicInteger(0);

    private static class Producer implements Runnable {
        private final String name;

        Producer(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            for (int i = 0; i < ITEMS_PER_PRODUCER; i++) {
                int data = rand.nextInt(1000);
                try {
                    empty.acquire();
                    synchronized (mutex) {
                        buffer[in] = data;
                        System.out.printf("Producer-%s ---> Produced value %d at index %d\n", name, data, in);
                        in = (in + 1) % BUFFER_SIZE;
                        producedCount.incrementAndGet();
                    }
                    full.release();
                    Thread.sleep(rand.nextInt(200) + 100);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    private static class Consumer implements Runnable {
        private final String name;

        Consumer(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            for (int i = 0; i < ITEMS_PER_PRODUCER; i++) {
                try {
                    full.acquire();
                    synchronized (mutex) {
                        int data = buffer[out];
                        System.out.printf("Consumer-%s ---> Consumed value %d from index %d\n", name, data, out);
                        out = (out + 1) % BUFFER_SIZE;
                        consumedCount.incrementAndGet();
                    }
                    empty.release();
                    Thread.sleep(rand.nextInt(400) + 200);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        int numProducers = 4;
        int numConsumers = 4;
        BUFFER_SIZE = 5;
        ITEMS_PER_PRODUCER = 10;

        buffer = new int[BUFFER_SIZE];
        empty = new Semaphore(BUFFER_SIZE);
        full = new Semaphore(0);

        System.out.println("=== Producer-Consumer Problem (Semaphores + Synchronization) ===");
        System.out.printf("Buffer size = %d | Producers = %d | Consumers = %d\n\n", BUFFER_SIZE, numProducers,
                numConsumers);

        Thread[] producers = new Thread[numProducers];
        Thread[] consumers = new Thread[numConsumers];

        for (int i = 0; i < numProducers; i++)
            producers[i] = new Thread(new Producer(String.valueOf(i + 1)));

        for (int i = 0; i < numConsumers; i++)
            consumers[i] = new Thread(new Consumer(String.valueOf(i + 1)));

        for (Thread t : producers)
            t.start();
        for (Thread t : consumers)
            t.start();

        try {
            for (Thread t : producers)
                t.join();
            for (Thread t : consumers)
                t.join();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        System.out.println("\nAll data produced and consumed successfully!");
        System.out.printf("Total Produced: %d | Total Consumed: %d\n", producedCount.get(), consumedCount.get());
    }
}
