/*
 * $Id: CompareTagBase.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import java.lang.reflect.InvocationTargetException;

/**
 * Abstract base class for comparison tags.  Concrete subclasses need only
 * define values for desired1 and desired2.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public abstract class CompareTagBase extends ConditionalTagBase {
    // ----------------------------------------------------- Instance Variables

    /**
     * We will do a double/float comparison.
     */
    protected static final int DOUBLE_COMPARE = 0;

    /**
     * We will do a long/int comparison.
     */
    protected static final int LONG_COMPARE = 1;

    /**
     * We will do a String comparison.
     */
    protected static final int STRING_COMPARE = 2;

    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(
            "org.apache.struts.taglib.logic.LocalStrings");

    // ------------------------------------------------------------ Properties

    /**
     * The value to which the variable specified by other attributes of this
     * tag will be compared.
     */
    public String value = null;

    public String getValue() {
        return (this.value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Release all allocated resources.
     */
    public void release() {
        super.release();
        value = null;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Evaluate the condition that is being tested by this particular tag, and
     * return <code>true</code> if the nested body content of this tag should
     * be evaluated, or <code>false</code> if it should be skipped. This
     * method must be implemented by concrete subclasses.
     *
     * @throws JspException if a JSP exception occurs
     */
    protected abstract boolean condition()
        throws JspException;

    /**
     * Evaluate the condition that is being tested by this particular tag, and
     * return <code>true</code> if the nested body content of this tag should
     * be evaluated, or <code>false</code> if it should be skipped. This
     * method must be implemented by concrete subclasses.
     *
     * @param desired1 First desired value for a true result (-1, 0, +1)
     * @param desired2 Second desired value for a true result (-1, 0, +1)
     * @throws JspException if a JSP exception occurs
     */
    protected boolean condition(int desired1, int desired2)
        throws JspException {
        // Acquire the value and determine the test type
        int type = -1;
        double doubleValue = 0.0;
        long longValue = 0;

        if ((type < 0) && (value.length() > 0)) {
            try {
                doubleValue = Double.parseDouble(value);
                type = DOUBLE_COMPARE;
            } catch (NumberFormatException e) {
                ;
            }
        }

        if ((type < 0) && (value.length() > 0)) {
            try {
                longValue = Long.parseLong(value);
                type = LONG_COMPARE;
            } catch (NumberFormatException e) {
                ;
            }
        }

        if (type < 0) {
            type = STRING_COMPARE;
        }

        // Acquire the unconverted variable value
        Object variable = null;

        if (cookie != null) {
            Cookie[] cookies =
                ((HttpServletRequest) pageContext.getRequest()).getCookies();

            if (cookies == null) {
                cookies = new Cookie[0];
            }

            for (int i = 0; i < cookies.length; i++) {
                if (cookie.equals(cookies[i].getName())) {
                    variable = cookies[i].getValue();

                    break;
                }
            }
        } else if (header != null) {
            variable =
                ((HttpServletRequest) pageContext.getRequest()).getHeader(header);
        } else if (name != null) {
            Object bean =
                TagUtils.getInstance().lookup(pageContext, name, scope);

            if (property != null) {
                if (bean == null) {
                    JspException e =
                        new JspException(messages.getMessage("logic.bean", name));

                    TagUtils.getInstance().saveException(pageContext, e);
                    throw e;
                }

                try {
                    variable = PropertyUtils.getProperty(bean, property);
                } catch (InvocationTargetException e) {
                    Throwable t = e.getTargetException();

                    if (t == null) {
                        t = e;
                    }

                    TagUtils.getInstance().saveException(pageContext, t);
                    throw new JspException(messages.getMessage(
                            "logic.property", name, property, t.toString()));
                } catch (Throwable t) {
                    TagUtils.getInstance().saveException(pageContext, t);
                    throw new JspException(messages.getMessage(
                            "logic.property", name, property, t.toString()));
                }
            } else {
                variable = bean;
            }
        } else if (parameter != null) {
            variable = pageContext.getRequest().getParameter(parameter);
        } else {
            JspException e =
                new JspException(messages.getMessage("logic.selector"));

            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        if (variable == null) {
            variable = ""; // Coerce null to a zero-length String
        }

        // Perform the appropriate comparison
        int result = 0;

        if (type == DOUBLE_COMPARE) {
            try {
                double doubleVariable = Double.parseDouble(variable.toString());

                if (doubleVariable < doubleValue) {
                    result = -1;
                } else if (doubleVariable > doubleValue) {
                    result = +1;
                }
            } catch (NumberFormatException e) {
                result = variable.toString().compareTo(value);
            }
        } else if (type == LONG_COMPARE) {
            try {
                long longVariable = Long.parseLong(variable.toString());

                if (longVariable < longValue) {
                    result = -1;
                } else if (longVariable > longValue) {
                    result = +1;
                }
            } catch (NumberFormatException e) {
                result = variable.toString().compareTo(value);
            }
        } else {
            result = variable.toString().compareTo(value);
        }

        // Normalize the result
        if (result < 0) {
            result = -1;
        } else if (result > 0) {
            result = +1;
        }

        // Return true if the result matches either desired value
        return ((result == desired1) || (result == desired2));
    }
}
