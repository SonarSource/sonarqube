/*
 * $Id: SelectModule.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.Globals;
import org.apache.struts.chain.Constants;
import org.apache.struts.chain.commands.AbstractSelectModule;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.chain.contexts.ServletActionContext;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Cache the <code>ModuleConfig</code> and <code>MessageResources</code>
 * instances for the sub-application module to be used for processing this
 * request.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-06-04 10:58:46 -0400 (Sat, 04 Jun 2005)
 *          $
 */
public class SelectModule extends AbstractSelectModule {
    // ------------------------------------------------------- Protected Methods
    protected String getPrefix(ActionContext context) {
        // Identify the URI from which we will match a module prefix
        ServletActionContext sacontext = (ServletActionContext) context;
        HttpServletRequest request = sacontext.getRequest();
        String uri =
            (String) request.getAttribute(Constants.INCLUDE_SERVLET_PATH);

        if (uri == null) {
            uri = request.getServletPath();
        }

        if (uri == null) {
            throw new IllegalArgumentException("No path information in request");
        }

        // Identify the module prefix for the current module
        String prefix = ""; // Initialize to default prefix
        String[] prefixes =
            (String[]) sacontext.getApplicationScope().get(Globals.MODULE_PREFIXES_KEY);
        int lastSlash = 0;

        while (prefix.equals("") && ((lastSlash = uri.lastIndexOf("/")) > 0)) {
            uri = uri.substring(0, lastSlash);

            for (int i = 0; i < prefixes.length; i++) {
                if (uri.equals(prefixes[i])) {
                    prefix = prefixes[i];

                    break;
                }
            }
        }

        return (prefix);
    }
}
