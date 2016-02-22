/*
 * $Id: MockMultipartRequestHandler.java 471754 2006-11-06 14:55:09Z husted $
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

package org.apache.struts.mock;

import java.util.Enumeration;
import java.util.Hashtable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.MultipartRequestHandler;

/**
 * <p>Mock <strong>MultipartRequestHandler</strong> object for unit tests.</p>
 *
 * @version $Rev: 471754 $
 */
public class MockMultipartRequestHandler implements MultipartRequestHandler {

    /** mock ActionServlet instance. */
    private ActionServlet servlet;

    /** mock ActionMapping instance. */
    private ActionMapping mapping = new ActionMapping();

    /** request elements. */
    private Hashtable elements;

    /**
     * Convienience method to set a reference to a mock
     * ActionServlet instance.
     * @param servlet Mock servlet instance.
     */
    public void setServlet(ActionServlet servlet) {
        this.servlet = servlet;
    }

    /**
     * Convienience method to set a reference to a mock
     * ActionMapping instance.
     * @param mapping Mock action mapping instance.
     */
    public void setMapping(ActionMapping mapping) {
        this.mapping = mapping;
    }

    /**
     * Get the mock ActionServlet instance.
     * @return The mock servlet instance.
     */
    public ActionServlet getServlet() {
        return this.servlet;
    }

    /**
     * Get the ActionMapping instance for this mock request.
     * @return The mock action mapping instance.
     */
    public ActionMapping getMapping() {
        return this.mapping;
    }

    /**
      * <p>Mock parsing of the ServletInputStream.</p>
      *
      * <p>Constructs a <code>Hashtable</code> of elements
      *    from the HttpServletRequest's parameters - no
      *    <code>FormFile</code> elements are created.</p>
      * @param request Mock request instance.
      * @throws ServletException If there is a problem with
      * processing the request.
      */
    public void handleRequest(HttpServletRequest request) throws ServletException {
        elements = new Hashtable();
        Enumeration enumer = request.getParameterNames();
        while (enumer.hasMoreElements()) {
            String key = enumer.nextElement().toString();
            elements.put(key, request.getParameter(key));
        }
    }

    /**
     * This method is called on to retrieve all the text
     * input elements of the request.
     *
     * @return A Hashtable where the keys and values are the names and
     *  values of the request input parameters
     */
    public Hashtable getTextElements() {
        return this.elements;
    }

    /**
     * <p>This method is called on to retrieve all the FormFile
     * input elements of the request.</p>
     *
     * @return This mock implementation returns an empty
     *    <code>Hashtable</code>
     */
    public Hashtable getFileElements() {
        return new Hashtable();
    }

    /**
     * This method returns all elements of a multipart request.
     * @return This mock implementation returns a Hashtable where
     *   the keys are input names and values are either Strings
     *   (no FormFile elements)
     */
    public Hashtable getAllElements() {
        return this.elements;
    }

    /**
     * Mock <code>rollback()</code> method does nothing.
     */
    public void rollback() {
        // ignore
    }

    /**
     * Mock <code>finish()</code> method does nothing.
     */
    public void finish() {
        // ignore
    }

}
