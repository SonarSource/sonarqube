/*
 * $Id: SelectAction.java 508312 2007-02-16 04:56:54Z pbenedict $
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
package org.apache.struts.chain.commands.servlet;

import org.apache.struts.chain.Constants;
import org.apache.struts.chain.commands.AbstractSelectAction;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.chain.contexts.ServletActionContext;
import org.apache.struts.config.ModuleConfig;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Cache the <code>ActionConfig</code> instance for the action to be used
 * for processing this request.</p>
 *
 * @version $Rev: 508312 $ $Date: 2005-05-07 12:11:38 -0400 (Sat, 07 May 2005)
 *          $
 */
public class SelectAction extends AbstractSelectAction {
    // ------------------------------------------------------- Protected Methods
    protected String getPath(ActionContext context) {
        ServletActionContext saContext = (ServletActionContext) context;
        HttpServletRequest request = saContext.getRequest();
        String path = null;
        boolean extension = false;

        // For prefix matching, match on the path info
        path = (String) request.getAttribute(Constants.INCLUDE_PATH_INFO);

        if ((path == null) || (path.length() == 0)) {
            path = request.getPathInfo();
        }

        // For extension matching, match on the servlet path
        if ((path == null) || (path.length() == 0)) {
            path =
                (String) request.getAttribute(Constants.INCLUDE_SERVLET_PATH);

            if ((path == null) || (path.length() == 0)) {
                path = request.getServletPath();
            }

            if ((path == null) || (path.length() == 0)) {
                throw new IllegalArgumentException(
                    "No path information in request");
            }

            extension = true;
        }

        // Strip the module prefix and extension (if any)
        ModuleConfig moduleConfig = saContext.getModuleConfig();
        String prefix = moduleConfig.getPrefix();

        if (!path.startsWith(prefix)) {
            throw new IllegalArgumentException("Path does not start with '"
                + prefix + "'");
        }

        path = path.substring(prefix.length());

        if (extension) {
            int slash = path.lastIndexOf("/");
            int period = path.lastIndexOf(".");

            if ((period >= 0) && (period > slash)) {
                path = path.substring(0, period);
            }
        }

        return (path);
    }
}
