/*
 * $Id: ActionServletWrapper.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.action;

import org.apache.struts.upload.MultipartRequestHandler;

import java.io.Serializable;

/**
 * <p>Provide a wrapper around an {@link ActionServlet} to expose only those
 * methods needed by other objects. When used with an {@link ActionForm},
 * subclasses must be careful that they do not return an object with public
 * getters and setters that could be exploited by automatic population of
 * properties.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-05-07 12:11:38 -0400 (Sat, 07 May 2005)
 *          $
 * @since Struts 1.0.1
 */
public class ActionServletWrapper implements Serializable {
    /**
     * <p>The servlet instance to which we are attached.</p>
     */
    protected transient ActionServlet servlet = null;

    /**
     * <p>Create object and set <code>servlet</code> property.</p>
     *
     * @param servlet <code>ActionServlet</code> to wrap
     */
    public ActionServletWrapper(ActionServlet servlet) {
        super();
        this.servlet = servlet;
    }

    /**
     * <p>Set servlet to a <code>MultipartRequestHandler</code>.</p>
     *
     * @param object The MultipartRequestHandler
     */
    public void setServletFor(MultipartRequestHandler object) {
        object.setServlet(this.servlet);

        // :FIXME: Should this be based on an "setServlet"
        // interface or introspection for a setServlet method?
        // Or, is it safer to just add the types we want as we want them?
    }
}
