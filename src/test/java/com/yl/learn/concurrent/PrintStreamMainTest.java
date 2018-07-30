package com.yl.learn.concurrent;

import com.yl.learn.concurrent.stream.PrintRunnable;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;

public class PrintStreamMainTest {

    public static void main(String[] args) throws IOException {
        PipedWriter out = new PipedWriter();
        PipedReader in = new PipedReader();

        out.connect(in);

        Thread printThread = new Thread(new PrintRunnable(in), "Print Thread");
        printThread.start();

        int receive = 0;
        try {
            while ((receive = System.in.read()) != -1) {
                out.write(receive);
            }
        }
        finally {
            out.close();
        }
    }

}
