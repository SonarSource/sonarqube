/*
 * $Id: ServletContextWriter.java 471754 2006-11-06 14:55:09Z husted $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts.util;

import javax.servlet.ServletContext;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A PrintWriter implementation that uses the logging facilities of a
 * <code>javax.servlet.ServletContext</code> to output its results.  Output
 * will be buffered until a newline character is output, <code>flush()</code>
 * is called, or until one of the <code>println()</code> methods is called.
 * Along the way, carriage return characters are skipped.
 *
 * @version $Rev: 471754 $ $Date: 2005-05-07 12:11:38 -0400 (Sat, 07 May 2005)
 *          $
 */
public class ServletContextWriter extends PrintWriter {
    // ------------------------------------------------------------- Properties

    /**
     * The buffer into which we accumulate lines to be logged.
     */
    protected StringBuffer buffer = new StringBuffer();

    /**
     * The servlet context with which we are associated.
     */
    protected ServletContext context = null;

    /**
     * The error state for this stream.
     */
    protected boolean error = false;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a ServletContextWriter associated with the specified
     * ServletContext instance.
     *
     * @param context The associated servlet context
     */
    public ServletContextWriter(ServletContext context) {
        super(new StringWriter());
        this.context = context;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Flush the stream and check for its error state.  <strong>IMPLEMENTATION
     * NOTE</strong> - our associated servlet context gives no indication of
     * problems with logging, so the only way this method will return
     * <code>true</code> is if <code>setError()</code> is called.
     */
    public boolean checkError() {
        flush();

        return (error);
    }

    /**
     * Close the stream.
     */
    public void close() {
        flush();
    }

    /**
     * Flush the stream.
     */
    public void flush() {
        if (buffer.length() > 0) {
            context.log(buffer.toString());
            buffer.setLength(0);
        }
    }

    /**
     * Print a boolean value.
     *
     * @param b The value to be printed
     */
    public void print(boolean b) {
        write(String.valueOf(b));
    }

    /**
     * Print a character value.
     *
     * @param c The value to be printed
     */
    public void print(char c) {
        write(c);
    }

    /**
     * Print a character array.
     *
     * @param c The character array to be printed
     */
    public void print(char[] c) {
        for (int i = 0; i < c.length; i++) {
            write(c[i]);
        }
    }

    /**
     * Print a double value.
     *
     * @param d The value to be printed
     */
    public void print(double d) {
        write(String.valueOf(d));
    }

    /**
     * Print a float value.
     *
     * @param f The value to be printed
     */
    public void print(float f) {
        write(String.valueOf(f));
    }

    /**
     * Print an integer value.
     *
     * @param i The value to be printed
     */
    public void print(int i) {
        write(String.valueOf(i));
    }

    /**
     * Print a long value.
     *
     * @param l The value to be printed
     */
    public void print(long l) {
        write(String.valueOf(l));
    }

    /**
     * Print an object.
     *
     * @param o The value to be printed
     */
    public void print(Object o) {
        write(o.toString());
    }

    /**
     * Print a String value.
     *
     * @param s The value to be printed
     */
    public void print(String s) {
        int len = s.length();

        for (int i = 0; i < len; i++) {
            write(s.charAt(i));
        }
    }

    /**
     * Terminate the current line and flush the buffer.
     */
    public void println() {
        flush();
    }

    /**
     * Print a boolean value and terminate the line.
     *
     * @param b The value to be printed
     */
    public void println(boolean b) {
        println(String.valueOf(b));
    }

    /**
     * Print a character value and terminate the line.
     *
     * @param c The value to be printed
     */
    public void println(char c) {
        write(c);
        println();
    }

    /**
     * Print a character array and terminate the line.
     *
     * @param c The character array to be printed
     */
    public void println(char[] c) {
        for (int i = 0; i < c.length; i++) {
            print(c[i]);
        }

        println();
    }

    /**
     * Print a double value and terminate the line.
     *
     * @param d The value to be printed
     */
    public void println(double d) {
        println(String.valueOf(d));
    }

    /**
     * Print a float value and terminate the line.
     *
     * @param f The value to be printed
     */
    public void println(float f) {
        println(String.valueOf(f));
    }

    /**
     * Print an integer value and terminate the line.
     *
     * @param i The value to be printed
     */
    public void println(int i) {
        println(String.valueOf(i));
    }

    /**
     * Print a long value and terminate the line.
     *
     * @param l The value to be printed
     */
    public void println(long l) {
        println(String.valueOf(l));
    }

    /**
     * Print an object and terminate the line.
     *
     * @param o The value to be printed
     */
    public void println(Object o) {
        println(o.toString());
    }

    /**
     * Print a String value and terminate the line.
     *
     * @param s The value to be printed
     */
    public void println(String s) {
        int len = s.length();

        for (int i = 0; i < len; i++) {
            print(s.charAt(i));
        }

        println();
    }

    /**
     * Set the error state for this stream.
     */
    public void setError() {
        this.error = true;
    }

    /**
     * Write a single character to this stream.
     *
     * @param c The character to be written
     */
    public void write(char c) {
        if (c == '\n') {
            flush();
        } else if (c != '\r') {
            buffer.append(c);
        }
    }

    /**
     * Write a single character to this stream.
     *
     * @param c The character to be written
     */
    public void write(int c) {
        write((char) c);
    }

    /**
     * Write an array of charaters to this stream.
     *
     * @param buf The character array to be written
     */
    public void write(char[] buf) {
        for (int i = 0; i < buf.length; i++) {
            write(buf[i]);
        }
    }

    /**
     * Write the specified subset of an array of characters to this stream.
     *
     * @param buf The character array from which to write
     * @param off The zero-relative starting offset to write
     * @param len The number of characters to write
     */
    public void write(char[] buf, int off, int len) {
        for (int i = off; i < len; i++) {
            write(buf[i]);
        }
    }

    /**
     * Write a String to this stream.
     *
     * @param s The string to be written
     */
    public void write(String s) {
        int len = s.length();

        for (int i = 0; i < len; i++) {
            write(s.charAt(i));
        }
    }

    /**
     * Write the specified portion of a String to this stream.
     *
     * @param s   The String from which to write
     * @param off The zero-relative starting offset to write
     * @param len The number of characters to write
     */
    public void write(String s, int off, int len) {
        for (int i = off; i < len; i++) {
            write(s.charAt(i));
        }
    }
}
