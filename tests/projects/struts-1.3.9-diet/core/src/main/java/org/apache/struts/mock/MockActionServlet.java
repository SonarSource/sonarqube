/*
 * $Id: MockActionServlet.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.action.ActionServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * <p>Mock <strong>ActionServlet</strong> object for low-level unit tests of
 * Struts controller components.  Coarser grained tests should be implemented
 * in terms of the Cactus framework, instead of the mock object classes.</p>
 *
 * <p><strong>WARNING</strong> - Only getter methods for servletContext and
 * servletConfig are provided, plus additional methods to configure this
 * object as necessary.  Methods for unsupported operations will throw
 * <code>UnsupportedOperationException</code>.</p>
 *
 * <p><strong>WARNING</strong> - Because unit tests operate in a single
 * threaded environment, no synchronization is performed.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-05-14 02:09:06 -0400 (Sat, 14 May 2005)
 *          $
 */
public class MockActionServlet extends ActionServlet {
    protected ServletContext servletContext;
    protected ServletConfig servletConfig;

    /**
     * <p>Constructor.</p>
     */
    public MockActionServlet(ServletContext servletContext,
        ServletConfig servletConfig) {
        this.servletContext = servletContext;
        this.servletConfig = servletConfig;
    }

    /**
     * <p>Constructor.</p>
     */
    public MockActionServlet() {
        ; // do nothing
    }

    /**
     * <p> Set property </p>
     *
     * @param servletContext
     */
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * <p> Get property </p>
     *
     * @return
     */
    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * <p> Set property
     *
     * @param servletConfig
     */
    public void setServletConfig(ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
    }

    /**
     * <p> Get property </p>
     *
     * @return
     */
    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    /**
     * <p> Expose as public so that test classes can exercise things which
     * retrieve messages. </p>
     */
    public void initInternal()
        throws ServletException {
        super.initInternal();
    }
}
