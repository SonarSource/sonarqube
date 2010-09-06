package com.test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for simple App.
 */
public class AppTest {

    @Test
    public void testApp() throws UnsupportedEncodingException {
      App app = new App();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PrintWriter pw = new PrintWriter(out);
      app.test(pw);
      pw.flush();
      String helloWorld = out.toString("Shift_JIS");
      assertTrue(helloWorld.length() > 0);
    }
}
