/*
 * $Id: TestRequestUtils.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.util;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.action.RequestProcessor;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.mock.MockFormBean;
import org.apache.struts.mock.MockPrincipal;
import org.apache.struts.mock.TestMockBase;

import java.net.MalformedURLException;

/**
 * <p>Unit tests for <code>org.apache.struts.util.RequestUtils</code>.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-08-14 17:24:39 -0400 (Sun, 14 Aug 2005)
 *          $
 */
public class TestRequestUtils extends TestMockBase {
    // ----------------------------------------------------------------- Basics
    public TestRequestUtils(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.awtui.TestRunner.main(new String[] {
                TestRequestUtils.class.getName()
            });
    }

    public static Test suite() {
        return (new TestSuite(TestRequestUtils.class));
    }

    // ----------------------------------------------------- Instance Variables
    // ----------------------------------------------------- Setup and Teardown
    public void setUp() {
        super.setUp();
    }

    public void tearDown() {
        super.tearDown();
    }

    // ------------------------------------------------------- Individual Tests
    // ---------------------------------------------------------- absoluteURL()
    public void testAbsoluteURL() {
        request.setPathElements("/myapp", "/action.do", null, null);

        String url = null;

        try {
            url = RequestUtils.absoluteURL(request, "/foo/bar.jsp").toString();
            assertEquals("absoluteURL is correct",
                "http://localhost:8080/myapp/foo/bar.jsp", url);
        } catch (MalformedURLException e) {
            fail("Threw MalformedURLException: " + e);
        }
    }

    // ------------------------------------------------------------ actionURL()
    // Default application -- extension mapping
    public void testActionURL1() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig);
        request.setPathElements("/myapp", "/foo.do", null, null);

        String url =
            RequestUtils.actionURL(request,
                moduleConfig.findActionConfig("/dynamic"), "*.do");

        assertNotNull("URL was returned", url);
        assertEquals("URL value", "/dynamic.do", url);
    }

    // Second application -- extension mapping
    public void testActionURL2() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig2);
        request.setPathElements("/myapp", "/2/foo.do", null, null);

        String url =
            RequestUtils.actionURL(request,
                moduleConfig2.findActionConfig("/dynamic2"), "*.do");

        assertNotNull("URL was returned", url);
        assertEquals("URL value", "/2/dynamic2.do", url);
    }

    // Default application -- path mapping
    public void testActionURL3() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig);
        request.setPathElements("/myapp", "/do/foo", null, null);

        String url =
            RequestUtils.actionURL(request,
                moduleConfig.findActionConfig("/dynamic"), "/do/*");

        assertNotNull("URL was returned", url);
        assertEquals("URL value", "/do/dynamic", url);
    }

    // ----------------------------------------------------- createActionForm()
    // Default module -- No ActionForm should be created
    public void testCreateActionForm1a() {
        request.setPathElements("/myapp", "/noform.do", null, null);

        ActionMapping mapping =
            (ActionMapping) moduleConfig.findActionConfig("/noform");

        assertNotNull("Found /noform mapping", mapping);

        ActionForm form =
            RequestUtils.createActionForm(request, mapping, moduleConfig, null);

        assertNull("No ActionForm returned", form);
    }

    // Second module -- No ActionForm should be created
    public void testCreateActionForm1b() {
        request.setPathElements("/myapp", "/2/noform.do", null, null);

        ActionMapping mapping =
            (ActionMapping) moduleConfig2.findActionConfig("/noform");

        assertNotNull("Found /noform mapping", mapping);

        ActionForm form =
            RequestUtils.createActionForm(request, mapping, moduleConfig2, null);

        assertNull("No ActionForm returned", form);
    }

    // Default module -- Standard ActionForm should be created
    public void testCreateActionForm2a() {
        request.setPathElements("/myapp", "/static.do", null, null);

        ActionMapping mapping =
            (ActionMapping) moduleConfig.findActionConfig("/static");

        assertNotNull("Found /static mapping", mapping);
        assertNotNull("Mapping has non-null name", mapping.getName());
        assertEquals("Mapping has correct name", "static", mapping.getName());
        assertNotNull("AppConfig has form bean " + mapping.getName(),
            moduleConfig.findFormBeanConfig(mapping.getName()));

        ActionForm form =
            RequestUtils.createActionForm(request, mapping, moduleConfig, null);

        assertNotNull("ActionForm returned", form);
        assertTrue("ActionForm of correct type", form instanceof MockFormBean);
    }

    // Second module -- Standard ActionForm should be created
    public void testCreateActionForm2b() {
        request.setPathElements("/myapp", "/2/static.do", null, null);

        ActionMapping mapping =
            (ActionMapping) moduleConfig2.findActionConfig("/static");

        assertNotNull("Found /static mapping", mapping);
        assertNotNull("Mapping has non-null name", mapping.getName());
        assertEquals("Mapping has correct name", "static", mapping.getName());
        assertNotNull("AppConfig has form bean " + mapping.getName(),
            moduleConfig.findFormBeanConfig(mapping.getName()));

        ActionForm form =
            RequestUtils.createActionForm(request, mapping, moduleConfig2, null);

        assertNotNull("ActionForm returned", form);
        assertTrue("ActionForm of correct type", form instanceof MockFormBean);
    }

    // Default module -- Dynamic ActionForm should be created
    public void testCreateActionForm3a() {
        request.setPathElements("/myapp", "/dynamic.do", null, null);

        ActionMapping mapping =
            (ActionMapping) moduleConfig.findActionConfig("/dynamic");

        assertNotNull("Found /dynamic mapping", mapping);
        assertNotNull("Mapping has non-null name", mapping.getName());
        assertEquals("Mapping has correct name", "dynamic", mapping.getName());
        assertNotNull("AppConfig has form bean " + mapping.getName(),
            moduleConfig.findFormBeanConfig(mapping.getName()));

        ActionForm form =
            RequestUtils.createActionForm(request, mapping, moduleConfig, null);

        assertNotNull("ActionForm returned", form);
        assertTrue("ActionForm of correct type", form instanceof DynaActionForm);
    }

    // Second module -- Dynamic ActionForm should be created
    public void testCreateActionForm3b() {
        request.setPathElements("/myapp", "/2/dynamic2.do", null, null);

        ActionMapping mapping =
            (ActionMapping) moduleConfig2.findActionConfig("/dynamic2");

        assertNotNull("Found /dynamic2 mapping", mapping);
        assertNotNull("Mapping has non-null name", mapping.getName());
        assertEquals("Mapping has correct name", "dynamic2", mapping.getName());
        assertNotNull("AppConfig has form bean " + mapping.getName(),
            moduleConfig2.findFormBeanConfig(mapping.getName()));

        ActionForm form =
            RequestUtils.createActionForm(request, mapping, moduleConfig2, null);

        assertNotNull("ActionForm returned", form);
        assertTrue("ActionForm of correct type", form instanceof DynaActionForm);
    }

    // Default module -- Dynamic ActionForm with initializers
    public void testCreateActionForm4a() {
        // Retrieve an appropriately configured DynaActionForm instance
        request.setPathElements("/myapp", "/dynamic0.do", null, null);

        ActionMapping mapping =
            (ActionMapping) moduleConfig.findActionConfig("/dynamic0");

        assertNotNull("Found /dynamic0 mapping", mapping);
        assertNotNull("Mapping has non-null name", mapping.getName());
        assertEquals("Mapping has correct name", "dynamic0", mapping.getName());
        assertNotNull("AppConfig has form bean " + mapping.getName(),
            moduleConfig.findFormBeanConfig(mapping.getName()));

        ActionForm form =
            RequestUtils.createActionForm(request, mapping, moduleConfig, null);

        assertNotNull("ActionForm returned", form);
        assertTrue("ActionForm of correct type", form instanceof DynaActionForm);

        // Validate the property values
        DynaActionForm dform = (DynaActionForm) form;
        Boolean booleanProperty = (Boolean) dform.get("booleanProperty");

        assertTrue("booleanProperty is true", booleanProperty.booleanValue());

        String stringProperty = (String) dform.get("stringProperty");

        assertEquals("stringProperty is correct", "String Property",
            stringProperty);

        Object value = null;

        value = dform.get("intArray1");
        assertNotNull("intArray1 exists", value);
        assertTrue("intArray1 is int[]", value instanceof int[]);

        int[] intArray1 = (int[]) value;

        assertEquals("intArray1 length is correct", 3, intArray1.length);
        assertEquals("intArray1[0] value is correct", 1, intArray1[0]);
        assertEquals("intArray1[1] value is correct", 2, intArray1[1]);
        assertEquals("intArray1[2] value is correct", 3, intArray1[2]);

        value = dform.get("intArray2");
        assertNotNull("intArray2 exists", value);
        assertTrue("intArray2 is int[]", value instanceof int[]);

        int[] intArray2 = (int[]) value;

        assertEquals("intArray2 length is correct", 5, intArray2.length);
        assertEquals("intArray2[0] value is correct", 0, intArray2[0]);
        assertEquals("intArray2[1] value is correct", 0, intArray2[1]);
        assertEquals("intArray2[2] value is correct", 0, intArray2[2]);
        assertEquals("intArray2[3] value is correct", 0, intArray2[3]);
        assertEquals("intArray2[4] value is correct", 0, intArray2[4]);

        value = dform.get("principal");
        assertNotNull("principal exists", value);
        assertTrue("principal is MockPrincipal", value instanceof MockPrincipal);

        value = dform.get("stringArray1");
        assertNotNull("stringArray1 exists", value);
        assertTrue("stringArray1 is int[]", value instanceof String[]);

        String[] stringArray1 = (String[]) value;

        assertEquals("stringArray1 length is correct", 3, stringArray1.length);
        assertEquals("stringArray1[0] value is correct", "aaa", stringArray1[0]);
        assertEquals("stringArray1[1] value is correct", "bbb", stringArray1[1]);
        assertEquals("stringArray1[2] value is correct", "ccc", stringArray1[2]);

        value = dform.get("stringArray2");
        assertNotNull("stringArray2 exists", value);
        assertTrue("stringArray2 is int[]", value instanceof String[]);

        String[] stringArray2 = (String[]) value;

        assertEquals("stringArray2 length is correct", 3, stringArray2.length);
        assertEquals("stringArray2[0] value is correct", new String(),
            stringArray2[0]);
        assertEquals("stringArray2[1] value is correct", new String(),
            stringArray2[1]);
        assertEquals("stringArray2[2] value is correct", new String(),
            stringArray2[2]);

        // Different form beans should get different property value instances
        Object value1 = null;
        DynaActionForm dform1 =
            (DynaActionForm) RequestUtils.createActionForm(request, mapping,
                moduleConfig, null);

        value = dform.get("principal");
        value1 = dform1.get("principal");
        assertEquals("Different form beans get equal instance values", value,
            value1);
        assertTrue("Different form beans get different instances 1",
            value != value1);

        value = dform.get("stringArray1");
        value1 = dform1.get("stringArray1");
        assertTrue("Different form beans get different instances 2",
            value != value1);

        dform1.set("stringProperty", "Different stringProperty value");
        value = dform.get("stringProperty");
        value1 = dform1.get("stringProperty");
        assertTrue("Different form beans get different instances 3",
            value != value1);
    }

    // ----------------------------------------------------------- forwardURL()
    // Default module (default forwardPattern)
    public void testForwardURL1() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig);
        request.setPathElements("/myapp", "/action.do", null, null);

        ForwardConfig forward = null;
        String result = null;

        // redirect=false, module=null
        forward = moduleConfig.findForwardConfig("moduleForward");
        assertNotNull("moduleForward found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("moduleForward computed", result);
        assertEquals("moduleForward value", "/module/forward", result);

        // redirect=true, module=null
        forward = moduleConfig.findForwardConfig("moduleRedirect");
        assertNotNull("moduleRedirect found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("moduleRedirect computed", result);
        assertEquals("moduleRedirect value", "/module/redirect", result);

        // redirect=false, module=/context
        forward = moduleConfig.findForwardConfig("contextForward");
        assertNotNull("contextForward found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("contextForward computed", result);
        assertEquals("contextForward value", "/context/forward", result);

        // redirect=true, module=/context
        forward = moduleConfig.findForwardConfig("contextRedirect");
        assertNotNull("contextRedirect found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("contextRedirect computed", result);
        assertEquals("contextRedirct value", "/context/redirect", result);

        // noslash, module=null
        forward = moduleConfig.findForwardConfig("moduleNoslash");
        assertNotNull("moduleNoslash found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("moduleNoslash computed", result);
        assertEquals("moduleNoslash value", "/module/noslash", result);

        // noslash, module=/
        forward = moduleConfig.findForwardConfig("contextNoslash");
        assertNotNull("contextNoslash found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("contextNoslash computed", result);
        assertEquals("contextNoslash value", "/context/noslash", result);
    }

    // Second module (default forwardPattern)
    public void testForwardURL2() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig2);
        request.setPathElements("/myapp", "/2/action.do", null, null);

        ForwardConfig forward = null;
        String result = null;

        // redirect=false, module=null
        forward = moduleConfig2.findForwardConfig("moduleForward");
        assertNotNull("moduleForward found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("moduleForward computed", result);
        assertEquals("moduleForward value", "/2/module/forward", result);

        // redirect=true, module=null
        forward = moduleConfig2.findForwardConfig("moduleRedirect");
        assertNotNull("moduleRedirect found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("moduleRedirect computed", result);
        assertEquals("moduleRedirect value", "/2/module/redirect", result);

        // redirect=false, module=/context
        forward = moduleConfig2.findForwardConfig("contextForward");
        assertNotNull("contextForward found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("contextForward computed", result);
        assertEquals("contextForward value", "/context/forward", result);

        // redirect=true, module=/context
        forward = moduleConfig2.findForwardConfig("contextRedirect");
        assertNotNull("contextRedirect found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("contextRedirect computed", result);
        assertEquals("contextRedirct value", "/context/redirect", result);

        // noslash, module=null
        forward = moduleConfig2.findForwardConfig("moduleNoslash");
        assertNotNull("moduleNoslash found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("moduleNoslash computed", result);
        assertEquals("moduleNoslash value", "/2/module/noslash", result);

        // noslash, module=/
        forward = moduleConfig2.findForwardConfig("contextNoslash");
        assertNotNull("contextNoslash found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("contextNoslash computed", result);
        assertEquals("contextNoslash value", "/context/noslash", result);
    }

    // Third module (custom forwardPattern)
    public void testForwardURL3() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig3);
        request.setPathElements("/myapp", "/3/action.do", null, null);

        ForwardConfig forward = null;
        String result = null;

        // redirect=false, module=null
        forward = moduleConfig3.findForwardConfig("moduleForward");
        assertNotNull("moduleForward found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("moduleForward computed", result);
        assertEquals("moduleForward value", "/forwarding/3/module/forward",
            result);

        // redirect=true, module=null
        forward = moduleConfig3.findForwardConfig("moduleRedirect");
        assertNotNull("moduleRedirect found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("moduleRedirect computed", result);
        assertEquals("moduleRedirect value", "/forwarding/3/module/redirect",
            result);

        // redirect=false, module=/context
        forward = moduleConfig3.findForwardConfig("contextForward");
        assertNotNull("contextForward found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("contextForward computed", result);
        assertEquals("contextForward value", "/forwarding/context/forward",
            result);

        // redirect=true, module=/context
        forward = moduleConfig3.findForwardConfig("contextRedirect");
        assertNotNull("contextRedirect found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("contextRedirect computed", result);
        assertEquals("contextRedirct value", "/forwarding/context/redirect",
            result);

        // noslash, module=null
        forward = moduleConfig3.findForwardConfig("moduleNoslash");
        assertNotNull("moduleNoslash found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("moduleNoslash computed", result);
        assertEquals("moduleNoslash value", "/forwarding/3/module/noslash",
            result);

        // noslash, module=/
        forward = moduleConfig3.findForwardConfig("contextNoslash");
        assertNotNull("contextNoslash found", forward);
        result = RequestUtils.forwardURL(request, forward, null);
        assertNotNull("contextNoslash computed", result);
        assertEquals("contextNoslash value", "/forwarding/context/noslash",
            result);
    }

    // Cross module forwards
    public void testForwardURLa() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig);
        request.setPathElements("/myapp", "/action.do", null, null);

        ForwardConfig forward = null;
        String result = null;

        // redirect=false, contextRelative=false, link to module 3
        forward = moduleConfig3.findForwardConfig("moduleForward");
        assertNotNull("moduleForward found", forward);
        result = RequestUtils.forwardURL(request, forward, moduleConfig3);
        assertNotNull("moduleForward computed", result);
        assertEquals("moduleForward value", "/forwarding/3/module/forward",
            result);

        // redirect=true, contextRelative=false, link to module 3
        forward = moduleConfig3.findForwardConfig("moduleRedirect");
        assertNotNull("moduleRedirect found", forward);
        result = RequestUtils.forwardURL(request, forward, moduleConfig3);
        assertNotNull("moduleRedirect computed", result);
        assertEquals("moduleRedirect value", "/forwarding/3/module/redirect",
            result);

        // redirect=false, module=/context
        forward = moduleConfig3.findForwardConfig("contextForward");
        assertNotNull("contextForward found", forward);
        result = RequestUtils.forwardURL(request, forward, moduleConfig3);
        assertNotNull("contextForward computed", result);
        assertEquals("contextForward value", "/forwarding/context/forward",
            result);

        // redirect=true, module=/context
        forward = moduleConfig3.findForwardConfig("contextRedirect");
        assertNotNull("contextRedirect found", forward);
        result = RequestUtils.forwardURL(request, forward, moduleConfig3);
        assertNotNull("contextRedirect computed", result);
        assertEquals("contextRedirct value", "/forwarding/context/redirect",
            result);

        // noslash, contextRelative=false, link to module 3
        forward = moduleConfig3.findForwardConfig("moduleNoslash");
        assertNotNull("moduleNoslash found", forward);
        result = RequestUtils.forwardURL(request, forward, moduleConfig3);
        assertNotNull("moduleNoslash computed", result);
        assertEquals("moduleNoslash value", "/forwarding/3/module/noslash",
            result);

        // noslash, module=/
        forward = moduleConfig3.findForwardConfig("contextNoslash");
        assertNotNull("contextNoslash found", forward);
        result = RequestUtils.forwardURL(request, forward, moduleConfig3);
        assertNotNull("contextNoslash computed", result);
        assertEquals("contextNoslash value", "/forwarding/context/noslash",
            result);
    }

    // ----------------------------------------------------------- requestURL()
    public void testRequestURL() {
        request.setPathElements("/myapp", "/foo.do", null, null);

        String url = null;

        try {
            url = RequestUtils.requestURL(request).toString();
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("URL was returned", url);
        assertEquals("URL value", "http://localhost:8080/myapp/foo.do", url);
    }

    // ---------------------------------------------------- selectApplication()
    // Map to the default module -- direct
    public void testSelectApplication1a() {
        request.setPathElements("/myapp", "/noform.do", null, null);
        ModuleUtils.getInstance().selectModule(request, context);

        ModuleConfig moduleConfig =
            (ModuleConfig) request.getAttribute(Globals.MODULE_KEY);

        assertNotNull("Selected a module", moduleConfig);
        assertEquals("Selected correct module", "", moduleConfig.getPrefix());

        // FIXME - check module resources?
    }

    // Map to the second module -- direct
    public void testSelectApplication1b() {
        String[] prefixes = { "/1", "/2" };

        context.setAttribute(Globals.MODULE_PREFIXES_KEY, prefixes);
        request.setPathElements("/myapp", "/2/noform.do", null, null);

        ModuleUtils.getInstance().selectModule(request, context);

        ModuleConfig moduleConfig =
            (ModuleConfig) request.getAttribute(Globals.MODULE_KEY);

        assertNotNull("Selected a module", moduleConfig);
        assertEquals("Selected correct module", "/2", moduleConfig.getPrefix());

        // FIXME - check module resources?
    }

    // Map to the default module -- include
    public void testSelectApplication2a() {
        request.setPathElements("/myapp", "/2/noform.do", null, null);
        request.setAttribute(RequestProcessor.INCLUDE_SERVLET_PATH, "/noform.do");
        ModuleUtils.getInstance().selectModule(request, context);

        ModuleConfig moduleConfig =
            (ModuleConfig) request.getAttribute(Globals.MODULE_KEY);

        assertNotNull("Selected an application", moduleConfig);
        assertEquals("Selected correct application", "",
            moduleConfig.getPrefix());

        // FIXME - check application resources?
    }

    // Map to the second module -- include
    public void testSelectApplication2b() {
        String[] prefixes = { "/1", "/2" };

        context.setAttribute(Globals.MODULE_PREFIXES_KEY, prefixes);
        request.setPathElements("/myapp", "/noform.do", null, null);
        request.setAttribute(RequestProcessor.INCLUDE_SERVLET_PATH,
            "/2/noform.do");
        ModuleUtils.getInstance().selectModule(request, context);

        ModuleConfig moduleConfig =
            (ModuleConfig) request.getAttribute(Globals.MODULE_KEY);

        assertNotNull("Selected a module", moduleConfig);
        assertEquals("Selected correct module", "/2", moduleConfig.getPrefix());

        // FIXME - check application resources?
    }

    // ------------------------------------------------------------ serverURL()
    // Basic test on values in mock objects
    public void testServerURL() {
        String url = null;

        try {
            url = RequestUtils.serverURL(request).toString();
        } catch (MalformedURLException e) {
            fail("Threw MalformedURLException: " + e);
        }

        assertNotNull("serverURL is present", url);
        assertEquals("serverURL value", "http://localhost:8080", url);
    }
}
