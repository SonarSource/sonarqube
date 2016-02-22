/*
 * $Id: ActionForward.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.config.ForwardConfig;

/**
 * <p>An <strong>ActionForward</strong> represents a destination to which the
 * controller, RequestProcessor, might be directed to perform a
 * RequestDispatcher.forward or HttpServletResponse.sendRedirect to, as a
 * result of processing activities of an Action class. Instances of this class
 * may be created dynamically as necessary, or configured in association with
 * an ActionMapping instance for named lookup of potentially multiple
 * destinations for a particular mapping instance.</p>
 *
 * <p>An ActionForward has the following minimal set of properties. Additional
 * properties can be provided as needed by subclassses.</p>
 *
 * <ul>
 *
 * <li><strong>contextRelative</strong> - Should the path value be interpreted
 * as context-relative (instead of module-relative, if it starts with a '/'
 * character? [false]</li>
 *
 * <li><strong>name</strong> - Logical name by which this instance may be
 * looked up in relationship to a particular ActionMapping. </li>
 *
 * <li><strong>path</strong> - Module-relative or context-relative URI to
 * which control should be forwarded, or an absolute or relative URI to which
 * control should be redirected.</li>
 *
 * <li><strong>redirect</strong> - Set to true if the controller servlet
 * should call HttpServletResponse.sendRedirect() on the associated path;
 * otherwise false. [false]</li>
 *
 * </ul>
 *
 * <p>Since Struts 1.1 this class extends ForwardConfig and inherits the
 * contextRelative property.
 *
 * <p><strong>NOTE</strong> - This class would have been deprecated and
 * replaced by org.apache.struts.config.ForwardConfig except for the fact that
 * it is part of the public API that existing applications are using.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-08-14 17:24:39 -0400 (Sun, 14 Aug 2005)
 *          $
 */
public class ActionForward extends ForwardConfig {
    /**
     * <p>Construct a new instance with default values.</p>
     */
    public ActionForward() {
        this(null, false);
    }

    /**
     * <p>Construct a new instance with the specified path.</p>
     *
     * @param path Path for this instance
     */
    public ActionForward(String path) {
        this(path, false);
    }

    /**
     * <p>Construct a new instance with the specified <code>path</code> and
     * <code>redirect</code> flag.</p>
     *
     * @param path     Path for this instance
     * @param redirect Redirect flag for this instance
     */
    public ActionForward(String path, boolean redirect) {
        super();
        setName(null);
        setPath(path);
        setRedirect(redirect);
    }

    /**
     * <p>Construct a new instance with the specified <code>name</code>,
     * <code>path</code> and <code>redirect</code> flag.</p>
     *
     * @param name     Name of this instance
     * @param path     Path for this instance
     * @param redirect Redirect flag for this instance
     */
    public ActionForward(String name, String path, boolean redirect) {
        super();
        setName(name);
        setPath(path);
        setRedirect(redirect);
    }

    /**
     * <p>Construct a new instance with the specified values.</p>
     *
     * @param name     Name of this forward
     * @param path     Path to which control should be forwarded or
     *                 redirected
     * @param redirect Should we do a redirect?
     * @param module   Module prefix, if any
     */
    public ActionForward(String name, String path, boolean redirect,
        String module) {
        super();
        setName(name);
        setPath(path);
        setRedirect(redirect);
        setModule(module);
    }

    /**
     * <p>Construct a new instance based on the values of another
     * ActionForward.</p>
     *
     * @param copyMe An ActionForward instance to copy
     * @since Struts 1.2.1
     */
    public ActionForward(ActionForward copyMe) {
        this(copyMe.getName(), copyMe.getPath(), copyMe.getRedirect(),
            copyMe.getModule());
    }
}
