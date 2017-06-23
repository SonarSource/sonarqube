/*
 * $Id: SetOriginalURI.java 471754 2006-11-06 14:55:09Z husted $
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
import org.apache.struts.chain.commands.AbstractSetOriginalURI;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.chain.contexts.ServletActionContext;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Set the servlet path.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-06-04 07:58:46 -0700 (Sat, 04 Jun 2005)
 *          $
 */
public class SetOriginalURI extends AbstractSetOriginalURI {
    // ------------------------------------------------------- Protected Methods
    protected void setOriginalURI(ActionContext context) {
        ServletActionContext swcontext = (ServletActionContext) context;
        HttpServletRequest request = swcontext.getRequest();

        request.setAttribute(Globals.ORIGINAL_URI_KEY, request.getServletPath());
    }
}
