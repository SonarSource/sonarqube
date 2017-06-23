/*
 * $Id: ResourceTag.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.taglib.bean;

import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Define a scripting variable based on the contents of the specified web
 * application resource.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class ResourceTag extends TagSupport {
    // ------------------------------------------------------------- Properties

    /**
     * Buffer size to use when reading the input stream.
     */
    protected static final int BUFFER_SIZE = 256;

    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(
            "org.apache.struts.taglib.bean.LocalStrings");

    /**
     * The name of the scripting variable that will be exposed as a page scope
     * attribute.
     */
    protected String id = null;

    /**
     * Return an InputStream to the specified resource if this is non-null.
     */
    protected String input = null;

    /**
     * The module-relative URI of the resource whose contents are to be
     * exposed.
     */
    protected String name = null;

    public String getId() {
        return (this.id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInput() {
        return (this.input);
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Retrieve the required property and expose it as a scripting variable.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        // Acquire an input stream to the specified resource
        InputStream stream =
            pageContext.getServletContext().getResourceAsStream(name);

        if (stream == null) {
            JspException e =
                new JspException(messages.getMessage("resource.get", name));

            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        // If we are returning an InputStream, do so and return
        if (input != null) {
            pageContext.setAttribute(id, stream);

            return (SKIP_BODY);
        }

        // Accumulate the contents of this resource into a StringBuffer
        try {
            StringBuffer sb = new StringBuffer();
            InputStreamReader reader = new InputStreamReader(stream);
            char[] buffer = new char[BUFFER_SIZE];
            int n = 0;

            while (true) {
                n = reader.read(buffer);

                if (n < 1) {
                    break;
                }

                sb.append(buffer, 0, n);
            }

            reader.close();
            pageContext.setAttribute(id, sb.toString());
        } catch (IOException e) {
            TagUtils.getInstance().saveException(pageContext, e);
            throw new JspException(messages.getMessage("resource.get", name));
        }

        return (SKIP_BODY);
    }

    /**
     * Release all allocated resources.
     */
    public void release() {
        super.release();
        id = null;
        input = null;
        name = null;
    }
}
