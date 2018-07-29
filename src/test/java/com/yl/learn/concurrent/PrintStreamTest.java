package com.yl.learn.concurrent;

import com.yl.learn.concurrent.stream.PrintRunnable;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;

public class PrintStreamTest extends TestCase {
    public void testPrintStream() throws IOException {
        PipedWriter out = new PipedWriter();
        PipedReader in = new PipedReader();

        out.connect(in);

        Thread printThread = new Thread(new PrintRunnable(in), "Print Thread");
        printThread.start();

        int receive = 0;
        try {
            out.write(receive);
        }
        finally {
            out.close();
        }
    }
}
