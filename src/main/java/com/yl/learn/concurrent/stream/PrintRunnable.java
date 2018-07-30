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
                char c = (char)receive;
                if(c == ' ') {
                    //
                }
                else {
                    byte[] bytes = String.valueOf(c).getBytes();
                    System.out.print(new String(bytes, "utf-8"));
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
