/*
 * $Id: MessagesPresentTag.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.taglib.TagUtils;

import javax.servlet.jsp.JspException;

import java.util.Iterator;

/**
 * Evalute to <code>true</code> if an <code>ActionMessages</code> class or a
 * class that can be converted to an <code>ActionMessages</code> class is in
 * request scope under the specified key and there is at least one message in
 * the class or for the property specified.
 *
 * @version $Rev: 471754 $ $Date: 2005-11-19 12:36:20 -0500 (Sat, 19 Nov 2005)
 *          $
 * @since Struts 1.1
 */
public class MessagesPresentTag extends ConditionalTagBase {
    /**
     * If this is set to 'true', then the <code>Globals.MESSAGE_KEY</code>
     * will be used to retrieve the messages from scope.
     */
    protected String message = null;

    public MessagesPresentTag() {
        name = Globals.ERROR_KEY;
    }

    public String getMessage() {
        return (this.message);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Evaluate the condition that is being tested by this particular tag, and
     * return <code>true</code> if the nested body content of this tag should
     * be evaluated, or <code>false</code> if it should be skipped.
     *
     * @throws JspException if a JSP exception occurs
     */
    protected boolean condition()
        throws JspException {
        return (condition(true));
    }

    /**
     * Evaluate the condition that is being tested by this particular tag, and
     * return <code>true</code> if there is at least one message in the class
     * or for the property specified.
     *
     * @param desired Desired outcome for a true result
     * @throws JspException if a JSP exception occurs
     */
    protected boolean condition(boolean desired)
        throws JspException {
        ActionMessages am = null;

        String key = name;

        if ((message != null) && "true".equalsIgnoreCase(message)) {
            key = Globals.MESSAGE_KEY;
        }

        try {
            am = TagUtils.getInstance().getActionMessages(pageContext, key);
        } catch (JspException e) {
            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        Iterator iterator = (property == null) ? am.get() : am.get(property);

        return (iterator.hasNext() == desired);
    }

    /**
     * Release all allocated resources.
     */
    public void release() {
        super.release();
        name = Globals.ERROR_KEY;
        message = null;
    }
}
