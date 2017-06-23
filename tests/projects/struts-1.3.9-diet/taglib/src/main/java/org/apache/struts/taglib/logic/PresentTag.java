/*
 * $Id: PresentTag.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.taglib.TagUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import java.security.Principal;

import java.util.StringTokenizer;

/**
 * Evalute the nested body content of this tag if the specified value is
 * present for this request.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class PresentTag extends ConditionalTagBase {
    public static final String ROLE_DELIMITER = ",";

    // ------------------------------------------------------ Protected Methods

    /**
     * Evaluate the condition that is being tested by this particular tag, and
     * return <code>true</code> if the nested body content of this tag should
     * be evaluated, or <code>false</code> if it should be skipped. This
     * method must be implemented by concrete subclasses.
     *
     * @throws JspException if a JSP exception occurs
     */
    protected boolean condition()
        throws JspException {
        return (condition(true));
    }

    /**
     * Evaluate the condition that is being tested by this particular tag, and
     * return <code>true</code> if the nested body content of this tag should
     * be evaluated, or <code>false</code> if it should be skipped. This
     * method must be implemented by concrete subclasses.
     *
     * @param desired Desired outcome for a true result
     * @throws JspException if a JSP exception occurs
     */
    protected boolean condition(boolean desired)
        throws JspException {
        // Evaluate the presence of the specified value
        boolean present = false;
        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();

        if (cookie != null) {
            present = this.isCookiePresent(request);
        } else if (header != null) {
            String value = request.getHeader(header);

            present = (value != null);
        } else if (name != null) {
            present = this.isBeanPresent();
        } else if (parameter != null) {
            String value = request.getParameter(parameter);

            present = (value != null);
        } else if (role != null) {
            StringTokenizer st =
                new StringTokenizer(role, ROLE_DELIMITER, false);

            while (!present && st.hasMoreTokens()) {
                present = request.isUserInRole(st.nextToken());
            }
        } else if (user != null) {
            Principal principal = request.getUserPrincipal();

            present = (principal != null) && user.equals(principal.getName());
        } else {
            JspException e =
                new JspException(messages.getMessage("logic.selector"));

            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        return (present == desired);
    }

    /**
     * Returns true if the bean given in the <code>name</code> attribute is
     * found.
     *
     * @since Struts 1.2
     */
    protected boolean isBeanPresent() {
        Object value = null;

        try {
            if (this.property != null) {
                value =
                    TagUtils.getInstance().lookup(pageContext, name,
                        this.property, scope);
            } else {
                value = TagUtils.getInstance().lookup(pageContext, name, scope);
            }
        } catch (JspException e) {
            value = null;
        }

        return (value != null);
    }

    /**
     * Returns true if the cookie is present in the request.
     *
     * @since Struts 1.2
     */
    protected boolean isCookiePresent(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return false;
        }

        for (int i = 0; i < cookies.length; i++) {
            if (this.cookie.equals(cookies[i].getName())) {
                return true;
            }
        }

        return false;
    }
}
