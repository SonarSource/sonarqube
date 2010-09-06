/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.collections;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Runtime exception thrown from functors.
 * If required, a root cause error can be wrapped within this one.
 * 
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
public class FunctorException extends RuntimeException {
    
    /**
     * Does JDK support nested exceptions
     */
    private static final boolean JDK_SUPPORTS_NESTED;
    
    static {
        boolean flag = false;
        try {
            Throwable.class.getDeclaredMethod("getCause", new Class[0]);
            flag = true;
        } catch (NoSuchMethodException ex) {
            flag = false;
        }
        JDK_SUPPORTS_NESTED = flag;
    }
    
    /**
     * Root cause of the exception
     */
    private final Throwable rootCause;

    /**
     * Constructs a new <code>FunctorException</code> without specified
     * detail message.
     */
    public FunctorException() {
        super();
        this.rootCause = null;
    }

    /**
     * Constructs a new <code>FunctorException</code> with specified
     * detail message.
     *
     * @param msg  the error message.
     */
    public FunctorException(String msg) {
        super(msg);
        this.rootCause = null;
    }

    /**
     * Constructs a new <code>FunctorException</code> with specified
     * nested <code>Throwable</code> root cause.
     *
     * @param rootCause  the exception or error that caused this exception
     *                   to be thrown.
     */
    public FunctorException(Throwable rootCause) {
        super((rootCause == null ? null : rootCause.getMessage()));
        this.rootCause = rootCause;
    }

    /**
     * Constructs a new <code>FunctorException</code> with specified
     * detail message and nested <code>Throwable</code> root cause.
     *
     * @param msg        the error message.
     * @param rootCause  the exception or error that caused this exception
     *                   to be thrown.
     */
    public FunctorException(String msg, Throwable rootCause) {
        super(msg);
        this.rootCause = rootCause;
    }

    /**
     * Gets the cause of this throwable.
     * 
     * @return  the cause of this throwable, or <code>null</code>
     */
    public Throwable getCause() {
        return rootCause;
    }

    /**
     * Prints the stack trace of this exception to the standard error stream.
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Prints the stack trace of this exception to the specified stream.
     *
     * @param out  the <code>PrintStream</code> to use for output
     */
    public void printStackTrace(PrintStream out) {
        synchronized (out) {
            PrintWriter pw = new PrintWriter(out, false);
            printStackTrace(pw);
            // Flush the PrintWriter before it's GC'ed.
            pw.flush();
        }
    }

    /**
     * Prints the stack trace of this exception to the specified writer.
     *
     * @param out  the <code>PrintWriter</code> to use for output
     */
    public void printStackTrace(PrintWriter out) {
        synchronized (out) {
            super.printStackTrace(out);
            if (rootCause != null && JDK_SUPPORTS_NESTED == false) {
                out.print("Caused by: ");
                rootCause.printStackTrace(out);
            }
        }
    }

}
