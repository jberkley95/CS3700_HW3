import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author John Berkley
 * CPP Class: CS3700
 * Date Created: Oct 07, 2018
 */
public class SockMatching {
    private String[] colors = {"Red", "Green", "Blue", "Orange"};
    private static Stack<Sock> toBeDestroyed = new Stack<>();
    private static int socksFinished = 0;
    private static int totalSocks = 0;

    public static void main(String[] args) throws InterruptedException {
        Phaser ph1 = new Phaser();
        Phaser ph2 = new Phaser(1);
        Phaser ph3 = new Phaser();
        final int numThreads = 4;

        ArrayList<Sock> redSocks = new ArrayList<>();
        ArrayList<Sock> greenSocks = new ArrayList<>();
        ArrayList<Sock> blueSocks = new ArrayList<>();
        ArrayList<Sock> orangeSocks = new ArrayList<>();
        ArrayList<ArrayList<Sock>> socksList = new ArrayList<>(Arrays.asList(
                redSocks,
                greenSocks,
                blueSocks,
                orangeSocks
        ));

        for (int i = 0; i < numThreads; i++) {
            new SockMatching().generatingSocksThread(ph1, ph2, ph3, socksList.get(i), i);
        }

        new SockMatching().matchingSocksThread(ph1, ph2, ph3, socksList);

        ph2.arriveAndAwaitAdvance();
        System.out.println("Red Socks Remaining: " + redSocks.size());
        System.out.println("Green Socks Remaining: " + greenSocks.size());
        System.out.println("Blue Socks Remaining: " + blueSocks.size());
        System.out.println("Orange Socks Remaining: " + orangeSocks.size());
        System.out.println("Total Inside Queue: 0");
    }

    private void generatingSocksThread(Phaser ph1, Phaser ph2, Phaser ph3, ArrayList<Sock> socks, int color) {
        // Thread registers themselves to the Phaser
        ph1.register();


        new Thread(new Runnable() {
            @Override
            public void run() {
                int socksToBeGenerated = ThreadLocalRandom.current().nextInt(0, 100) + 1;

                for (int i = 0; i < socksToBeGenerated; i++) {
                    socks.add(new Sock(colors[color]));
                    socksFinished++;
                    System.out.println(colors[color] + " Sock: Produced " + (i + 1) + " out of " + socksToBeGenerated + " " + colors[color] + " socks.");
                    if (socks.size() >= 2) {
                        ph1.arrive();
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ph3.awaitAdvance(ph3.getPhase());
                    }
                }
                ph1.arriveAndDeregister();
            }
        }).start();
    }

    private void matchingSocksThread(Phaser ph1, Phaser ph2, Phaser ph3, ArrayList<ArrayList<Sock>> socks) {
        ph2.register();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean matching = true;
                while (matching) {
                    matching = false;
                    ph1.awaitAdvance(ph1.getPhase());

                    for (int i = 0; i < 4; i++) {
                        if (socks.get(i).size() >= 2) {
                            matching = true;
                            toBeDestroyed.push(socks.get(i).remove(0));
                            System.out.println("Matching Thread: Send "+ colors[i] + " Socks to Washer. Total socks in existance " + (socks.get(0).size() + socks.get(1).size() + socks.get(2).size() + socks.get(3).size() - 1) + ". Total inside queue " + (toBeDestroyed.size() - 1));
                            socks.get(i).remove(0);

                            if (toBeDestroyed.size() >= 2) {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                new SockMatching().washingSocksThread(ph1, ph2, ph3, i);

                                ph3.awaitAdvance(ph3.getPhase());
                            }
                        }
                    }
                    if (socks.get(0).size() < 2 && socks.get(1).size() < 2 && socks.get(2).size() < 2 && socks.get(3).size() < 2) {
                        matching = false;
                    }


                }
                ph2.arriveAndDeregister();
            }
        }).start();
    }

    private void washingSocksThread(Phaser ph1, Phaser ph2, Phaser ph3, int color) {
        ph3.register();

        new Thread(new Runnable() {
            @Override
            public void run() {

                //sock.removeSockPair();
                if (toBeDestroyed.size() > 0) {
                    toBeDestroyed.pop();
                    System.out.println("Washing Thread: Destroyed " + colors[color] + " socks");
                }

                // Notify finished destroying
                ph3.arriveAndDeregister();
            }
        }).start();
    }
}
