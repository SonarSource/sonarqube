/*
 * $Id: ForwardTag.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.taglib.logic;

import org.apache.struts.action.ActionForward;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Perform a forward or redirect to a page that is looked up in the
 * configuration information associated with our application.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class ForwardTag extends TagSupport {
    // ----------------------------------------------------------- Properties

    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(
            "org.apache.struts.taglib.logic.LocalStrings");

    /**
     * The logical name of the <code>ActionForward</code> entry to be looked
     * up.
     */
    protected String name = null;

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    // ------------------------------------------------------- Public Methods

    /**
     * Defer processing until the end of this tag is encountered.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        return (SKIP_BODY);
    }

    /**
     * Look up the ActionForward associated with the specified name, and
     * perform a forward or redirect to that path as indicated.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
        // Look up the desired ActionForward entry
        ActionForward forward = null;
        ModuleConfig config =
            TagUtils.getInstance().getModuleConfig(pageContext);

        if (config != null) {
            forward = (ActionForward) config.findForwardConfig(name);
        }

        if (forward == null) {
            JspException e =
                new JspException(messages.getMessage("forward.lookup", name));

            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        // Forward or redirect to the corresponding actual path
        String path = forward.getPath();

        path = config.getPrefix() + path;

        if (forward.getRedirect()) {
            this.doRedirect(path);
        } else {
            this.doForward(path);
        }

        // Skip the remainder of this page
        return (SKIP_PAGE);
    }

    /**
     * Forward to the given path converting exceptions to JspException.
     *
     * @param path The path to forward to.
     * @throws JspException
     * @since Struts 1.2
     */
    protected void doForward(String path)
        throws JspException {
        try {
            pageContext.forward(path);
        } catch (Exception e) {
            TagUtils.getInstance().saveException(pageContext, e);
            throw new JspException(messages.getMessage("forward.forward", name,
                    e.toString()));
        }
    }

    /**
     * Redirect to the given path converting exceptions to JspException.
     *
     * @param path The path to redirect to.
     * @throws JspException
     * @since Struts 1.2
     */
    protected void doRedirect(String path)
        throws JspException {
        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();

        HttpServletResponse response =
            (HttpServletResponse) pageContext.getResponse();

        try {
            if (path.startsWith("/")) {
                path = request.getContextPath() + path;
            }

            response.sendRedirect(response.encodeRedirectURL(path));
        } catch (Exception e) {
            TagUtils.getInstance().saveException(pageContext, e);
            throw new JspException(messages.getMessage("forward.redirect",
                    name, e.toString()));
        }
    }

    /**
     * Release all allocated resources.
     */
    public void release() {
        super.release();
        name = null;
    }
}
