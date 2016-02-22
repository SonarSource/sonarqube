/*
 * $Id: PlugIn.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.config.ModuleConfig;

import javax.servlet.ServletException;

/**
 * <p>A <strong>PlugIn</strong> is a configuration wrapper for a
 * module-specific resource or service that needs to be notified about
 * application startup and application shutdown events (corresponding to when
 * the container calls <code>init</code> and <code>destroy</code> on the
 * corresponding {@link ActionServlet} instance). <code>PlugIn</code> objects
 * can be configured in the <code>struts-config.xml</code> file, without the
 * need to subclass {@link ActionServlet} simply to perform application
 * lifecycle activities.</p>
 *
 * <p>Implementations of this interface must supply a zero-argument
 * constructor for use by {@link ActionServlet}. Configuration can be
 * accomplished by providing standard JavaBeans property setter methods, which
 * will all have been called before the <code>init()</code> method is
 * invoked.</p>
 *
 * <p>This interface can be applied to any class, including an Action
 * subclass. </p>
 *
 * @version $Rev: 471754 $ $Date: 2005-05-14 01:09:32 -0400 (Sat, 14 May 2005)
 *          $
 * @since Struts 1.1
 */
public interface PlugIn {
    /**
     * <p>Receive notification that our owning module is being shut down.</p>
     */
    void destroy();

    /**
     * <p>Receive notification that the specified module is being started
     * up.</p>
     *
     * @param servlet ActionServlet that is managing all the modules in this
     *                web application
     * @param config  ModuleConfig for the module with which this plug-in is
     *                associated
     * @throws ServletException if this <code>PlugIn</code> cannot be
     *                          successfully initialized
     */
    void init(ActionServlet servlet, ModuleConfig config)
        throws ServletException;
}
