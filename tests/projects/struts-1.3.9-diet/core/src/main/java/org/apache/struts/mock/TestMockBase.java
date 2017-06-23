/*
 * $Id: TestMockBase.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.mock;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionFormBean;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.config.ControllerConfig;
import org.apache.struts.config.FormPropertyConfig;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.ModuleConfigFactory;

/**
 * <p>Convenience base class for unit tests of the <code>org.apache.struts.util</code>
 * package, and others that require a runtime environment similar to what the
 * Struts controller servlet sets up.  The <code>setUp()</code> method
 * establishes a consistent basic environment for the various tests.  The only
 * tests included in this class are simple validations that the basic
 * environment was set up correctly.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-08-14 17:24:39 -0400 (Sun, 14 Aug 2005)
 *          $
 */
public class TestMockBase extends TestCase {
    // ----------------------------------------------------- Instance Variables
    protected ModuleConfig moduleConfig = null;
    protected ModuleConfig moduleConfig2 = null;
    protected ModuleConfig moduleConfig3 = null;
    protected MockServletConfig config = null;
    protected MockServletContext context = null;
    protected MockPageContext page = null;
    protected MockPrincipal principal = null;
    protected MockHttpServletRequest request = null;
    protected MockHttpServletResponse response = null;
    protected MockHttpSession session = null;

    // ----------------------------------------------------------------- Basics
    public TestMockBase(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.awtui.TestRunner.main(new String[] { TestMockBase.class.getName() });
    }

    public static Test suite() {
        return (new TestSuite(TestMockBase.class));
    }

    // ----------------------------------------------------- Setup and Teardown
    public void setUp() {
        // Set up the servlet API objects for a test scenario
        context = new MockServletContext();
        config = new MockServletConfig(context);
        session = new MockHttpSession(context);
        request = new MockHttpServletRequest(session);
        principal =
            new MockPrincipal("username", new String[] { "admin", "manager" });
        request.setUserPrincipal(principal);
        response = new MockHttpServletResponse();
        page = new MockPageContext(config, request, response);

        // Set up application configurations for our supported modules
        setUpDefaultApp();
        setUpSecondApp();
        setUpThirdApp();

        // NOTE - we do not initialize the request attribute
        // for the selected module so that fallbacks to the
        // default module can be tested.  To select a module,
        // tests should set the request attribute Globals.MODULE_KEY
        // to the ModuleConfig instance for the selected module
    }

    protected void setUpDefaultApp() {
        ActionFormBean formBean = null;
        ActionMapping mapping = null;

        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();

        moduleConfig = factoryObject.createModuleConfig("");

        context.setAttribute(Globals.MODULE_KEY, moduleConfig);

        // Forward "external" to "http://jakarta.apache.org/"
        moduleConfig.addForwardConfig(new ActionForward("external",
                "http://jakarta.apache.org/", false));

        // Forward "foo" to "/bar.jsp"
        moduleConfig.addForwardConfig(new ActionForward("foo", "/bar.jsp", false));

        // Forward "relative1" to "relative.jsp" non-context-relative
        moduleConfig.addForwardConfig(new ActionForward("relative1",
                "relative.jsp", false));

        // Forward "relative2" to "relative.jsp" context-relative
        moduleConfig.addForwardConfig(new ActionForward("relative2",
                "relative.jsp", false));

        // Form Bean "static" is a standard ActionForm subclass
        formBean =
            new ActionFormBean("static", "org.apache.struts.mock.MockFormBean");
        moduleConfig.addFormBeanConfig(formBean);

        // Action "/static" uses the "static" form bean in request scope
        mapping = new ActionMapping();
        mapping.setInput("/static.jsp");
        mapping.setName("static");
        mapping.setPath("/static");
        mapping.setScope("request");
        mapping.setType("org.apache.struts.mock.MockAction");
        moduleConfig.addActionConfig(mapping);

        // Form Bean "dynamic" is a DynaActionForm with the same properties
        formBean =
            new ActionFormBean("dynamic",
                "org.apache.struts.action.DynaActionForm");
        formBean.addFormPropertyConfig(new FormPropertyConfig(
                "booleanProperty", "boolean", "false"));
        formBean.addFormPropertyConfig(new FormPropertyConfig(
                "stringProperty", "java.lang.String", null));
        moduleConfig.addFormBeanConfig(formBean);

        // Action "/dynamic" uses the "dynamic" form bean in session scope
        mapping = new ActionMapping();
        mapping.setInput("/dynamic.jsp");
        mapping.setName("dynamic");
        mapping.setPath("/dynamic");
        mapping.setScope("session");
        mapping.setType("org.apache.struts.mock.MockAction");
        moduleConfig.addActionConfig(mapping);

        // Form Bean "/dynamic0" is a DynaActionForm with initializers
        formBean =
            new ActionFormBean("dynamic0",
                "org.apache.struts.action.DynaActionForm");
        formBean.addFormPropertyConfig(new FormPropertyConfig(
                "booleanProperty", "boolean", "true"));
        formBean.addFormPropertyConfig(new FormPropertyConfig(
                "stringProperty", "java.lang.String", "String Property"));
        formBean.addFormPropertyConfig(new FormPropertyConfig("intArray1",
                "int[]", "{1,2,3}", 4)); // 4 should be ignored
        formBean.addFormPropertyConfig(new FormPropertyConfig("intArray2",
                "int[]", null, 5)); // 5 should be respected
        formBean.addFormPropertyConfig(new FormPropertyConfig("principal",
                "org.apache.struts.mock.MockPrincipal", null));
        formBean.addFormPropertyConfig(new FormPropertyConfig("stringArray1",
                "java.lang.String[]", "{aaa,bbb,ccc}", 2)); // 2 should be ignored
        formBean.addFormPropertyConfig(new FormPropertyConfig("stringArray2",
                "java.lang.String[]", null, 3)); // 3 should be respected
        moduleConfig.addFormBeanConfig(formBean);

        // Action "/dynamic0" uses the "dynamic0" form bean in request scope
        mapping = new ActionMapping();
        mapping.setName("dynamic0");
        mapping.setPath("/dynamic0");
        mapping.setScope("request");
        mapping.setType("org.apache.struts.mock.MockAction");
        moduleConfig.addActionConfig(mapping);

        // Action "/noform" has no form bean associated with it
        mapping = new ActionMapping();
        mapping.setPath("/noform");
        mapping.setType("org.apache.struts.mock.MockAction");
        moduleConfig.addActionConfig(mapping);

        // Configure global forward declarations
        moduleConfig.addForwardConfig(new ForwardConfig("moduleForward",
                "/module/forward", false)); // No redirect, same module

        moduleConfig.addForwardConfig(new ForwardConfig("moduleRedirect",
                "/module/redirect", true)); // Redirect, same module

        moduleConfig.addForwardConfig(new ForwardConfig("contextForward",
                "/forward", false, // No redirect
                "/context")); // Specify module

        moduleConfig.addForwardConfig(new ForwardConfig("contextRedirect",
                "/redirect", true, // Redirect
                "/context")); // Specify module

        moduleConfig.addForwardConfig(new ForwardConfig("moduleNoslash",
                "module/noslash", false)); // No redirect, same module

        moduleConfig.addForwardConfig(new ForwardConfig("contextNoslash",
                "noslash", false, // No redirect
                "/context")); // Specify module
    }

    protected void setUpSecondApp() {
        ActionFormBean formBean = null;
        ActionMapping mapping = null;

        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();

        moduleConfig2 = factoryObject.createModuleConfig("/2");

        context.setAttribute(Globals.MODULE_KEY + "/2", moduleConfig2);

        // Forward "external" to "http://jakarta.apache.org/"
        moduleConfig2.addForwardConfig(new ActionForward("external",
                "http://jakarta.apache.org/", false));

        // Forward "foo" to "/baz.jsp" (different from default)
        moduleConfig2.addForwardConfig(new ActionForward("foo", "/baz.jsp",
                false));

        // Forward "relative1" to "relative.jsp" non-context-relative
        moduleConfig2.addForwardConfig(new ActionForward("relative1",
                "relative.jsp", false));

        // Forward "relative2" to "relative.jsp" context-relative
        moduleConfig2.addForwardConfig(new ActionForward("relative2",
                "relative.jsp", false));

        // Form Bean "static" is a standard ActionForm subclass (same as default)
        formBean =
            new ActionFormBean("static", "org.apache.struts.mock.MockFormBean");
        moduleConfig2.addFormBeanConfig(formBean);

        // Action "/static" uses the "static" form bean in request scope (same as default)
        mapping = new ActionMapping();
        mapping.setInput("/static.jsp");
        mapping.setName("static");
        mapping.setPath("/static");
        mapping.setScope("request");
        mapping.setType("org.apache.struts.mock.MockAction");
        moduleConfig2.addActionConfig(mapping);

        // Form Bean "dynamic2" is a DynaActionForm with the same properties
        formBean =
            new ActionFormBean("dynamic2",
                "org.apache.struts.action.DynaActionForm");
        formBean.addFormPropertyConfig(new FormPropertyConfig(
                "booleanProperty", "boolean", "false"));
        formBean.addFormPropertyConfig(new FormPropertyConfig(
                "stringProperty", "java.lang.String", null));
        moduleConfig2.addFormBeanConfig(formBean);

        // Action "/dynamic2" uses the "dynamic2" form bean in session scope
        mapping = new ActionMapping();
        mapping.setInput("/dynamic2.jsp");
        mapping.setName("dynamic2");
        mapping.setPath("/dynamic2");
        mapping.setScope("session");
        mapping.setType("org.apache.struts.mock.MockAction");
        moduleConfig2.addActionConfig(mapping);

        // Action "/noform" has no form bean associated with it (same as default)
        mapping = new ActionMapping();
        mapping.setPath("/noform");
        mapping.setType("org.apache.struts.mock.MockAction");
        moduleConfig2.addActionConfig(mapping);

        // Configure global forward declarations
        moduleConfig2.addForwardConfig(new ForwardConfig("moduleForward",
                "/module/forward", false)); // No redirect, same module

        moduleConfig2.addForwardConfig(new ForwardConfig("moduleRedirect",
                "/module/redirect", true)); // Redirect, same module

        moduleConfig2.addForwardConfig(new ForwardConfig("contextForward",
                "/forward", false, // No redirect
                "/context")); // Specify module

        moduleConfig2.addForwardConfig(new ForwardConfig("contextRedirect",
                "/redirect", true, // Redirect
                "/context")); // Specify module

        moduleConfig2.addForwardConfig(new ForwardConfig("moduleNoslash",
                "module/noslash", false)); // No redirect, same module

        moduleConfig2.addForwardConfig(new ForwardConfig("contextNoslash",
                "noslash", false, // No redirect
                "/context")); // Specify module
    }

    // Set up third app for testing URL mapping
    protected void setUpThirdApp() {
        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();

        moduleConfig3 = factoryObject.createModuleConfig("/3");

        context.setAttribute(Globals.MODULE_KEY + "/3", moduleConfig3);

        // Instantiate the controller configuration for this app
        ControllerConfig controller = new ControllerConfig();

        moduleConfig3.setControllerConfig(controller);

        // Configure the properties we will be testing
        controller.setForwardPattern("/forwarding$M$P");
        controller.setInputForward(true);
        controller.setPagePattern("/paging$M$P");

        // Configure global forward declarations
        moduleConfig3.addForwardConfig(new ForwardConfig("moduleForward",
                "/module/forward", false)); // No redirect, same module

        moduleConfig3.addForwardConfig(new ForwardConfig("moduleRedirect",
                "/module/redirect", true)); // Redirect, same module

        moduleConfig3.addForwardConfig(new ForwardConfig("contextForward",
                "/forward", false, // No redirect
                "/context")); // Specify module

        moduleConfig3.addForwardConfig(new ForwardConfig("contextRedirect",
                "/redirect", true, // Redirect
                "/context")); // Specify module

        moduleConfig3.addForwardConfig(new ForwardConfig("moduleNoslash",
                "module/noslash", false)); // No redirect, same module

        moduleConfig3.addForwardConfig(new ForwardConfig("contextNoslash",
                "noslash", false, // No redirect
                "/context")); // specify module
    }

    public void tearDown() {
        moduleConfig3 = null;
        moduleConfig2 = null;
        moduleConfig = null;
        config = null;
        context = null;
        page = null;
        principal = null;
        request = null;
        response = null;
        session = null;
    }

    // ------------------------------------------------------- Individual Tests
    // Test the basic setup of the mock object environment
    public void testUtilBaseEnvironment() {
        // Validate the servlet API objects and inter-relationships
        assertNotNull("config is present", config);
        assertNotNull("context is present", context);
        assertNotNull("page is present", page);
        assertNotNull("principal is present", principal);
        assertNotNull("request is present", request);
        assertNotNull("response is present", response);
        assertNotNull("session is present", session);
        assertEquals("page-->config", config, page.getServletConfig());
        assertEquals("config-->context", context, config.getServletContext());
        assertEquals("page-->context", context, page.getServletContext());
        assertEquals("page-->request", request, page.getRequest());
        assertEquals("page-->response", response, page.getResponse());
        assertEquals("page-->session", session, page.getSession());
        assertEquals("request-->principal", principal,
            request.getUserPrincipal());
        assertEquals("request-->session", session, request.getSession());
        assertEquals("session-->context", context, session.getServletContext());

        // Validate the configuration for the default module
        assertNotNull("moduleConfig is present", moduleConfig);
        assertEquals("context-->moduleConfig", moduleConfig,
            context.getAttribute(Globals.MODULE_KEY));

        // Validate the configuration for the second module
        assertNotNull("moduleConfig2 is present", moduleConfig2);
        assertEquals("context-->moduleConfig2", moduleConfig2,
            context.getAttribute(Globals.MODULE_KEY + "/2"));
    }
}
