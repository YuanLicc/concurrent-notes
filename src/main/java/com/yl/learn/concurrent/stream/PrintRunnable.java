package com.yl.learn.concurrent.stream;

import java.io.IOException;
import java.io.PipedReader;

public class PrintRunnable implements Runnable {

    private PipedReader in;

    public PrintRunnable(PipedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        int receive = 0;
        try {
            while ((receive = in.read()) != -1) {
                System.out.print(receive);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
