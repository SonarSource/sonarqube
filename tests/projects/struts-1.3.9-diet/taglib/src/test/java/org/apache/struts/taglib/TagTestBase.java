/*
 * $Id: $
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
package org.apache.struts.taglib;

import junit.framework.TestCase;

import org.apache.struts.Globals;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.impl.ModuleConfigImpl;
import org.apache.struts.mock.MockHttpServletRequest;
import org.apache.struts.mock.MockHttpServletResponse;
import org.apache.struts.mock.MockHttpSession;
import org.apache.struts.mock.MockPageContext;
import org.apache.struts.mock.MockServletConfig;
import org.apache.struts.mock.MockServletContext;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.MessageResourcesFactory;
import org.apache.struts.util.PropertyMessageResources;

public class TagTestBase extends TestCase {
    protected TagUtils tagutils = TagUtils.getInstance();
    protected MockServletConfig servletConfig;
    protected MockServletContext servletContext;
    protected MockHttpServletRequest request;
    protected MockPageContext pageContext;
    protected ModuleConfig moduleConfig;
    protected ModuleConfig moduleConfig2;
    protected ModuleConfig moduleConfig3;

    public TagTestBase() {
        super();
    }

    public TagTestBase(String theName) {
        super(theName);
    }

    /**
     * Helper method that creates/configures a basic configuration of Mock
     * Objects.
     *
     *
     * PageContext ServletConfig ServletContext HttpServletRequest HttpSession
     * HttpServletResponse
     *
     * "/myapp", "/foo", null, null,
     */
    public void setUp() {
        // -- default Module
        this.moduleConfig = new ModuleConfigImpl("");
        this.moduleConfig.addForwardConfig(new ForwardConfig("foo", "/bar.jsp",
                false));
        this.moduleConfig.addForwardConfig(new ForwardConfig("relative1",
                "relative.jsp", false));
        this.moduleConfig.addForwardConfig(new ForwardConfig("relative2",
                "relative.jsp", false));
        this.moduleConfig.addForwardConfig(new ForwardConfig("external",
                "http://struts.apache.org/", false));

        // -- module "/2"
        this.moduleConfig2 = new ModuleConfigImpl("/2");
        this.moduleConfig2.addForwardConfig(new ForwardConfig("foo",
                "/baz.jsp", false));
        this.moduleConfig2.addForwardConfig(new ForwardConfig("relative1",
                "relative.jsp", false));
        this.moduleConfig2.addForwardConfig(new ForwardConfig("relative2",
                "relative.jsp", false));
        this.moduleConfig2.addForwardConfig(new ForwardConfig("external",
                "http://struts.apache.org/", false));

        // -- module "/3"
        this.moduleConfig3 = new ModuleConfigImpl("/3");

        // -- configure the ServletContext
        this.servletContext = new MockServletContext();
        this.servletContext.setAttribute(Globals.MODULE_KEY, moduleConfig);
        this.servletContext.setAttribute(Globals.MODULE_KEY + "/2",
            moduleConfig2);
        this.servletContext.setAttribute(Globals.MODULE_KEY + "/3",
            moduleConfig3);

        // -- configure the ServletConfig
        this.servletConfig = new MockServletConfig();
        this.servletConfig.setServletContext(servletContext);

        // -- configure the request
        this.request = new MockHttpServletRequest(new MockHttpSession());

        pageContext =
            new MockPageContext(servletConfig, request,
                new MockHttpServletResponse());
    }

    public void tearDown() {
        this.moduleConfig = null;
        this.moduleConfig2 = null;
        this.moduleConfig3 = null;
        this.pageContext = null;
        this.request = null;
    }

    protected void putBundleInScope(int scope, boolean returnNull) {
        MessageResourcesFactory factory =
            MessageResourcesFactory.createFactory();
        MessageResources messageResources =
            new PropertyMessageResources(factory,
                "org.apache.struts.taglib.sample");

        messageResources.setReturnNull(returnNull);
        pageContext.setAttribute(Globals.MESSAGES_KEY, messageResources, scope);
    }
}
