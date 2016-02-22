/*
 * $Id: BaseTag.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.taglib.html;

import org.apache.struts.Globals;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.RequestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import java.io.IOException;

import java.util.StringTokenizer;

/**
 * Renders an HTML <base> element with an href attribute pointing to the
 * absolute location of the enclosing JSP page. This tag is only valid when
 * nested inside a head tag body. The presence of this tag allows the browser
 * to resolve relative URL's to images, CSS stylesheets  and other resources
 * in a manner independent of the URL used to call the ActionServlet.
 *
 * @version $Rev: 471754 $ $Date: 2005-09-20 02:29:01 -0400 (Tue, 20 Sep 2005)
 *          $
 */
public class BaseTag extends TagSupport {
    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(Constants.Package
            + ".LocalStrings");
    protected final String REF_SITE = "site";
    protected final String REF_PAGE = "page";

    /**
     * The server name to use instead of request.getServerName().
     */
    protected String server = null;

    /**
     * The target window for this base reference.
     */
    protected String target = null;

    /**
     * The reference to which the base will created.
     */
    protected String ref = REF_PAGE;

    /**
     * Gets the reference to which the base will be created
     */
    public String getRef() {
        return (this.ref);
    }

    /**
     * Sets the reference to which the base will be created.
     *
     * @param ref Either "page" to render the base as the jsp path located, or
     *            "site" as the application's context
     */
    public void setRef(String ref) {
        if (ref == null) {
            throw new IllegalArgumentException("Ref attribute cannot be null");
        }

        ref = ref.toLowerCase();

        if (ref.equals(REF_PAGE) || ref.equals(REF_SITE)) {
            this.ref = ref;
        } else {
            throw new IllegalArgumentException("Ref attribute must either be "
                + "'" + REF_PAGE + "' or '" + REF_SITE + "'");
        }
    }

    public String getTarget() {
        return (this.target);
    }

    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Process the start of this tag.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();
        String serverName =
            (this.server == null) ? request.getServerName() : this.server;

        int port = request.getServerPort();
        String headerHost = request.getHeader("Host");

        if ((serverName == null) && (headerHost != null)) {
            StringTokenizer tokenizer = new StringTokenizer(headerHost, ":");

            serverName = tokenizer.nextToken();

            if (tokenizer.hasMoreTokens()) {
                String portS = tokenizer.nextToken();

                try {
                    port = Integer.parseInt(portS);
                } catch (Exception e) {
                    port = 80;
                }
            } else {
                port = 80;
            }
        }

        String baseTag =
            renderBaseElement(request.getScheme(), serverName, port,
                request.getRequestURI());

        JspWriter out = pageContext.getOut();

        try {
            out.write(baseTag);
        } catch (IOException e) {
            pageContext.setAttribute(Globals.EXCEPTION_KEY, e,
                PageContext.REQUEST_SCOPE);
            throw new JspException(messages.getMessage("common.io", e.toString()));
        }

        return EVAL_BODY_INCLUDE;
    }

    /**
     * Render a fully formed HTML &lt;base&gt; element and return it as a
     * String.
     *
     * @param scheme     The scheme used in the url (ie. http or https).
     * @param serverName
     * @param port
     * @param uri        The portion of the url from the protocol name up to
     *                   the query string.
     * @return String An HTML &lt;base&gt; element.
     * @since Struts 1.1
     */
    protected String renderBaseElement(String scheme, String serverName,
        int port, String uri) {
        StringBuffer tag = new StringBuffer("<base href=\"");

        if (ref.equals(REF_SITE)) {
            StringBuffer contextBase =
                new StringBuffer(((HttpServletRequest) pageContext.getRequest())
                    .getContextPath());

            contextBase.append("/");
            tag.append(RequestUtils.createServerUriStringBuffer(scheme,
                    serverName, port, contextBase.toString()).toString());
        } else {
            tag.append(RequestUtils.createServerUriStringBuffer(scheme,
                    serverName, port, uri).toString());
        }

        tag.append("\"");

        if (this.target != null) {
            tag.append(" target=\"");
            tag.append(this.target);
            tag.append("\"");
        }

        if (TagUtils.getInstance().isXhtml(this.pageContext)) {
            tag.append(" />");
        } else {
            tag.append(">");
        }

        return tag.toString();
    }

    /**
     * Returns the server.
     *
     * @return String
     */
    public String getServer() {
        return this.server;
    }

    /**
     * Sets the server.
     *
     * @param server The server to set
     */
    public void setServer(String server) {
        this.server = server;
    }
}
