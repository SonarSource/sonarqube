/*
 * $Id: MessagesTag.java 471754 2006-11-06 14:55:09Z husted $
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
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import java.util.Iterator;

/**
 * Custom tag that iterates the elements of a message collection. It defaults
 * to retrieving the messages from <code>Globals.ERROR_KEY</code>, but if the
 * message attribute is set to true then the messages will be retrieved from
 * <code>Globals.MESSAGE_KEY</code>. This is an alternative to the default
 * <code>ErrorsTag</code>.
 *
 * @version $Rev: 471754 $ $Date: 2005-11-08 23:50:53 -0500 (Tue, 08 Nov 2005)
 *          $
 * @since Struts 1.1
 */
public class MessagesTag extends BodyTagSupport {
    /**
     * The message resources for this package.
     */
    protected static MessageResources messageResources =
        MessageResources.getMessageResources(Constants.Package
            + ".LocalStrings");

    /**
     * Iterator of the elements of this error collection, while we are
     * actually running.
     */
    protected Iterator iterator = null;

    /**
     * Whether or not any error messages have been processed.
     */
    protected boolean processed = false;

    /**
     * The name of the scripting variable to be exposed.
     */
    protected String id = null;

    /**
     * The servlet context attribute key for our resources.
     */
    protected String bundle = null;

    /**
     * The session attribute key for our locale.
     */
    protected String locale = Globals.LOCALE_KEY;

    /**
     * The request attribute key for our error messages (if any).
     */
    protected String name = Globals.ERROR_KEY;

    /**
     * The name of the property for which error messages should be returned,
     * or <code>null</code> to return all errors.
     */
    protected String property = null;

    /**
     * The message resource key for errors header.
     */
    protected String header = null;

    /**
     * The message resource key for errors footer.
     */
    protected String footer = null;

    /**
     * If this is set to 'true', then the <code>Globals.MESSAGE_KEY</code>
     * will be used to retrieve the messages from scope.
     */
    protected String message = null;

    public String getId() {
        return (this.id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBundle() {
        return (this.bundle);
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public String getLocale() {
        return (this.locale);
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProperty() {
        return (this.property);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getHeader() {
        return (this.header);
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getFooter() {
        return (this.footer);
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public String getMessage() {
        return (this.message);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Construct an iterator for the specified collection, and begin looping
     * through the body once per element.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        // Initialize for a new request.
        processed = false;

        // Were any messages specified?
        ActionMessages messages = null;

        // Make a local copy of the name attribute that we can modify.
        String name = this.name;

        if ((message != null) && "true".equalsIgnoreCase(message)) {
            name = Globals.MESSAGE_KEY;
        }

        try {
            messages =
                TagUtils.getInstance().getActionMessages(pageContext, name);
        } catch (JspException e) {
            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        // Acquire the collection we are going to iterate over
        this.iterator =
            (property == null) ? messages.get() : messages.get(property);

        // Store the first value and evaluate, or skip the body if none
        if (!this.iterator.hasNext()) {
            return SKIP_BODY;
        }

        // process the first message
        processMessage((ActionMessage) iterator.next());

        if ((header != null) && (header.length() > 0)) {
            String headerMessage =
                TagUtils.getInstance().message(pageContext, bundle, locale,
                    header);

            if (headerMessage != null) {
                TagUtils.getInstance().write(pageContext, headerMessage);
            }
        }

        // Set the processed variable to true so the
        // doEndTag() knows processing took place
        processed = true;

        return (EVAL_BODY_TAG);
    }

    /**
     * Make the next collection element available and loop, or finish the
     * iterations if there are no more elements.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doAfterBody() throws JspException {
        // Render the output from this iteration to the output stream
        if (bodyContent != null) {
            TagUtils.getInstance().writePrevious(pageContext,
                bodyContent.getString());
            bodyContent.clearBody();
        }

        // Decide whether to iterate or quit
        if (iterator.hasNext()) {
            processMessage((ActionMessage) iterator.next());

            return (EVAL_BODY_TAG);
        } else {
            return (SKIP_BODY);
        }
    }

    /**
     * Process a message.
     */
    private void processMessage(ActionMessage report)
        throws JspException {
        String msg = null;

        if (report.isResource()) {
            msg = TagUtils.getInstance().message(pageContext, bundle, locale,
                    report.getKey(), report.getValues());

            if (msg == null) {
                String bundleName = (bundle == null) ? "default" : bundle;

                msg = messageResources.getMessage("messagesTag.notfound",
                        report.getKey(), bundleName);
            }
        } else {
            msg = report.getKey();
        }

        if (msg == null) {
            pageContext.removeAttribute(id);
        } else {
            pageContext.setAttribute(id, msg);
        }
    }

    /**
     * Clean up after processing this enumeration.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
        if (processed && (footer != null) && (footer.length() > 0)) {
            String footerMessage =
                TagUtils.getInstance().message(pageContext, bundle, locale,
                    footer);

            if (footerMessage != null) {
                TagUtils.getInstance().write(pageContext, footerMessage);
            }
        }

        return EVAL_PAGE;
    }

    /**
     * Release all allocated resources.
     */
    public void release() {
        super.release();
        iterator = null;
        processed = false;
        id = null;
        bundle = null;
        locale = Globals.LOCALE_KEY;
        name = Globals.ERROR_KEY;
        property = null;
        header = null;
        footer = null;
        message = null;
    }
}
