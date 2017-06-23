/*
 * $Id: TokenProcessor.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.Globals;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * TokenProcessor is responsible for handling all token related functionality.
 * The methods in this class are synchronized to protect token processing from
 * multiple threads.  Servlet containers are allowed to return a different
 * HttpSession object for two threads accessing the same session so it is not
 * possible to synchronize on the session.
 *
 * @since Struts 1.1
 */
public class TokenProcessor {
    /**
     * The singleton instance of this class.
     */
    private static TokenProcessor instance = new TokenProcessor();

    /**
     * The timestamp used most recently to generate a token value.
     */
    private long previous;

    /**
     * Protected constructor for TokenProcessor.  Use TokenProcessor.getInstance()
     * to obtain a reference to the processor.
     */
    protected TokenProcessor() {
        super();
    }

    /**
     * Retrieves the singleton instance of this class.
     */
    public static TokenProcessor getInstance() {
        return instance;
    }

    /**
     * <p>Return <code>true</code> if there is a transaction token stored in
     * the user's current session, and the value submitted as a request
     * parameter with this action matches it.  Returns <code>false</code>
     * under any of the following circumstances:</p>
     *
     * <ul>
     *
     * <li>No session associated with this request</li>
     *
     * <li>No transaction token saved in the session</li>
     *
     * <li>No transaction token included as a request parameter</li>
     *
     * <li>The included transaction token value does not match the transaction
     * token in the user's session</li>
     *
     * </ul>
     *
     * @param request The servlet request we are processing
     */
    public synchronized boolean isTokenValid(HttpServletRequest request) {
        return this.isTokenValid(request, false);
    }

    /**
     * Return <code>true</code> if there is a transaction token stored in the
     * user's current session, and the value submitted as a request parameter
     * with this action matches it.  Returns <code>false</code>
     *
     * <ul>
     *
     * <li>No session associated with this request</li> <li>No transaction
     * token saved in the session</li>
     *
     * <li>No transaction token included as a request parameter</li>
     *
     * <li>The included transaction token value does not match the transaction
     * token in the user's session</li>
     *
     * </ul>
     *
     * @param request The servlet request we are processing
     * @param reset   Should we reset the token after checking it?
     */
    public synchronized boolean isTokenValid(HttpServletRequest request,
        boolean reset) {
        // Retrieve the current session for this request
        HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        // Retrieve the transaction token from this session, and
        // reset it if requested
        String saved =
            (String) session.getAttribute(Globals.TRANSACTION_TOKEN_KEY);

        if (saved == null) {
            return false;
        }

        if (reset) {
            this.resetToken(request);
        }

        // Retrieve the transaction token included in this request
        String token = request.getParameter(Globals.TOKEN_KEY);

        if (token == null) {
            return false;
        }

        return saved.equals(token);
    }

    /**
     * Reset the saved transaction token in the user's session.  This
     * indicates that transactional token checking will not be needed on the
     * next request that is submitted.
     *
     * @param request The servlet request we are processing
     */
    public synchronized void resetToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return;
        }

        session.removeAttribute(Globals.TRANSACTION_TOKEN_KEY);
    }

    /**
     * Save a new transaction token in the user's current session, creating a
     * new session if necessary.
     *
     * @param request The servlet request we are processing
     */
    public synchronized void saveToken(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String token = generateToken(request);

        if (token != null) {
            session.setAttribute(Globals.TRANSACTION_TOKEN_KEY, token);
        }
    }

    /**
     * Generate a new transaction token, to be used for enforcing a single
     * request for a particular transaction.
     *
     * @param request The request we are processing
     */
    public synchronized String generateToken(HttpServletRequest request) {
        HttpSession session = request.getSession();

        return generateToken(session.getId());
    }

    /**
     * Generate a new transaction token, to be used for enforcing a single
     * request for a particular transaction.
     *
     * @param id a unique Identifier for the session or other context in which
     *           this token is to be used.
     */
    public synchronized String generateToken(String id) {
        try {
            long current = System.currentTimeMillis();

            if (current == previous) {
                current++;
            }

            previous = current;

            byte[] now = new Long(current).toString().getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");

            md.update(id.getBytes());
            md.update(now);

            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Convert a byte array to a String of hexadecimal digits and return it.
     *
     * @param buffer The byte array to be converted
     */
    private String toHex(byte[] buffer) {
        StringBuffer sb = new StringBuffer(buffer.length * 2);

        for (int i = 0; i < buffer.length; i++) {
            sb.append(Character.forDigit((buffer[i] & 0xf0) >> 4, 16));
            sb.append(Character.forDigit(buffer[i] & 0x0f, 16));
        }

        return sb.toString();
    }
}
