/*
 * $Id: TestActionServlet.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.action;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.ExceptionConfig;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.config.FormPropertyConfig;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.ModuleConfigFactory;
import org.apache.struts.util.MessageResources;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

import java.util.List;

/**
 * Suite of unit tests for the <code>org.apache.struts.action.ActionServlet</code>
 * class.
 */
public class TestActionServlet extends TestCase {
    // ----------------------------------------------------- Instance Variables

    /**
     * The ModuleConfig we'll use.
     */
    protected ModuleConfig moduleConfig = null;

    /**
     * The common form bean we'll use.
     */
    protected FormBeanConfig baseFormBean = null;

    /**
     * The common exception config we'll use.
     */
    protected ExceptionConfig baseException = null;

    /**
     * The common action config we'll use.
     */
    protected ActionMapping baseAction = null;

    /**
     * The common action forward we'll use.
     */
    protected ActionForward baseForward = null;

    /**
     * The ActionServlet we'll test.
     */
    protected ActionServlet actionServlet = null;

    // ------------------------------------------ Constructors, suite, and main

    /**
     * Defines the testcase name for JUnit.
     *
     * @param theName the testcase's name.
     */
    public TestActionServlet(String theName) {
        super(theName);
    }

    /**
     * Start the tests.
     *
     * @param theArgs the arguments. Not used
     */
    public static void main(String[] theArgs) {
        junit.awtui.TestRunner.main(new String[] {
                TestActionServlet.class.getName()
            });
    }

    /**
     * @return a test suite (<code>TestSuite</code>) that includes all methods
     *         starting with "test"
     */
    public static Test suite() {
        // All methods starting with "test" will be executed in the test suite.
        return new TestSuite(TestActionServlet.class);
    }

    // ------------------------------------------------- setUp() and tearDown()

    /**
     * Set up instance variables required by this test case.
     */
    public void setUp() throws Exception {
        actionServlet = new ActionServlet();
        actionServlet.initInternal();

        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();

        moduleConfig = factoryObject.createModuleConfig("");

        // Setup the base form
        baseFormBean = new FormBeanConfig();
        baseFormBean.setName("baseForm");
        baseFormBean.setType("org.apache.struts.action.DynaActionForm");

        // Set up id, name, and score
        FormPropertyConfig property = new FormPropertyConfig();

        property.setName("id");
        property.setType("java.lang.String");
        baseFormBean.addFormPropertyConfig(property);

        property = new FormPropertyConfig();
        property.setName("name");
        property.setType("java.lang.String");
        baseFormBean.addFormPropertyConfig(property);

        property = new FormPropertyConfig();
        property.setName("score");
        property.setType("java.lang.String");
        baseFormBean.addFormPropertyConfig(property);

        // Setup the exception handler
        baseException = new ExceptionConfig();
        baseException.setType("java.lang.NullPointerException");
        baseException.setKey("msg.exception.npe");

        // Setup the forward config
        baseForward = new ActionForward("success", "/succes.jsp", false);

        // Setup the action config
        baseAction = new ActionMapping();
        baseAction.setPath("/index");
        baseAction.setType("org.apache.struts.actions.DummyAction");
        baseAction.setName("someForm");
        baseAction.setInput("/input.jsp");
        baseAction.addForwardConfig(new ActionForward("next", "/next.jsp", false));
        baseAction.addForwardConfig(new ActionForward("prev", "/prev.jsp", false));

        ExceptionConfig exceptionConfig = new ExceptionConfig();

        exceptionConfig.setType("java.sql.SQLException");
        exceptionConfig.setKey("msg.exception.sql");
        baseAction.addExceptionConfig(exceptionConfig);

        // Nothing is registered to our module config until they are needed
    }

    /**
     * Tear down instance variables required by this test case.
     */
    public void tearDown() {
        moduleConfig = null;
    }

    // ----------------------------- initInternal() and destroyInternal() tests

    /**
     * Verify that we can initialize and destroy our internal message
     * resources object.
     */
    public void testInitDestroyInternal() {
        ActionServlet servlet = new ActionServlet();

        try {
            servlet.initInternal();
        } catch (ServletException e) {
            fail("initInternal() threw exception: " + e);
        }

        assertTrue("internal was initialized", servlet.getInternal() != null);
        assertTrue("internal of correct type",
            servlet.getInternal() instanceof MessageResources);
        servlet.destroyInternal();
        assertTrue("internal was destroyed", servlet.getInternal() == null);
    }

    /**
     * Test class loader resolution and splitting.
     */
    public void notestSplitAndResolvePaths()
        throws Exception {
        ActionServlet servlet = new ActionServlet();
        List list =
            servlet.splitAndResolvePaths(
                "org/apache/struts/config/struts-config.xml");

        assertNotNull(list);
        assertTrue("List size should be 1", list.size() == 1);

        list =
            servlet.splitAndResolvePaths(
                "org/apache/struts/config/struts-config.xml, "
                + "org/apache/struts/config/struts-config-1.1.xml");
        assertNotNull(list);
        assertTrue("List size should be 2, was " + list.size(), list.size() == 2);

        list = servlet.splitAndResolvePaths("META-INF/MANIFEST.MF");
        assertNotNull(list);
        assertTrue("Number of manifests should be more than 5, was "
            + list.size(), list.size() > 5);

        // test invalid path
        try {
            list =
                servlet.splitAndResolvePaths(
                    "org/apache/struts/config/struts-asdfasdfconfig.xml");
            fail("Should have thrown an exception on bad path");
        } catch (NullPointerException ex) {
            // correct behavior since internal error resources aren't loaded
        }
    }

    //----- Test initApplication() method --------------------------------------

    /**
     * Verify that nothing happens if no "application" property is defined in
     * the servlet configuration.
     */

    /*
    public void testInitApplicationNull() throws ServletException
    {
        ActionServlet servlet = new ActionServlet();
        servlet.init(config);

        // Test the initApplication() method
        servlet.initApplication();

        // As no "application" object is found in the servlet config, no
        // attribute should be set in the context
        assertTrue(config.getServletContext().getAttribute(Action.MESSAGES_KEY) == null);
    }
    */

    /**
     * Verify that eveything is fine when only a "application" parameter is
     * defined in the servlet configuration.
     */

    /*
    public void testInitApplicationOk1() throws ServletException
    {
        // initialize config
        config.setInitParameter("application", "org.apache.struts.webapp.example.ApplicationResources");

        ActionServlet servlet = new ActionServlet();
        servlet.init(config);

        // Test the initApplication() method
        servlet.initApplication();

        assertTrue(servlet.application != null);
        assertTrue(servlet.application.getReturnNull() == true);

        assertTrue(config.getServletContext().getAttribute(Action.MESSAGES_KEY) != null);
        assertEquals(servlet.application, config.getServletContext().getAttribute(Action.MESSAGES_KEY));

    }
    */

    // --------------------------------------------------- FormBeanConfig Tests

    /**
     * Test that nothing fails if there are no extensions.
     */
    public void testInitModuleFormBeansNoExtends()
        throws ServletException {
        moduleConfig.addFormBeanConfig(baseFormBean);

        try {
            actionServlet.initModuleExceptionConfigs(moduleConfig);
        } catch (Exception e) {
            fail("Unexpected exception caught.");
        }
    }

    /**
     * Test that initModuleFormBeans throws an exception when a form with a
     * null type is present.
     */
    public void testInitModuleFormBeansNullFormType()
        throws ServletException {
        FormBeanConfig formBean = new FormBeanConfig();

        formBean.setName("noTypeForm");
        moduleConfig.addFormBeanConfig(formBean);

        try {
            actionServlet.initModuleFormBeans(moduleConfig);
            fail("An exception should've been thrown here.");
        } catch (UnavailableException e) {
            // success
        } catch (Exception e) {
            fail("Unrecognized exception thrown: " + e);
        }
    }

    /**
     * Test that initModuleFormBeans throws an exception when a form whose
     * prop type is null is present.
     */
    public void testInitModuleFormBeansNullPropType()
        throws ServletException {
        moduleConfig.addFormBeanConfig(baseFormBean);
        baseFormBean.findFormPropertyConfig("name").setType(null);

        try {
            actionServlet.initModuleFormBeans(moduleConfig);
            fail("An exception should've been thrown here.");
        } catch (UnavailableException e) {
            // success
        } catch (Exception e) {
            fail("Unrecognized exception thrown: " + e);
        }
    }

    /**
     * Test that processFormBeanExtension() calls processExtends()
     */
    public void testProcessFormBeanExtension()
        throws ServletException {
        CustomFormBeanConfig form = new CustomFormBeanConfig();

        actionServlet.processFormBeanExtension(form, moduleConfig);

        assertTrue("processExtends() was not called", form.processExtendsCalled);
    }

    /**
     * Make sure processFormBeanConfigClass() returns an instance of the
     * correct class if the base config is using a custom class.
     */
    public void testProcessFormBeanConfigClass()
        throws Exception {
        CustomFormBeanConfig customBase = new CustomFormBeanConfig();

        customBase.setName("customBase");
        moduleConfig.addFormBeanConfig(customBase);

        FormBeanConfig customSub = new FormBeanConfig();

        customSub.setName("customSub");
        customSub.setExtends("customBase");
        customSub.setType("org.apache.struts.action.DynaActionForm");
        moduleConfig.addFormBeanConfig(customSub);

        FormBeanConfig result =
            actionServlet.processFormBeanConfigClass(customSub, moduleConfig);

        assertTrue("Incorrect class of form bean config",
            result instanceof CustomFormBeanConfig);
        assertEquals("Incorrect name", customSub.getName(), result.getName());
        assertEquals("Incorrect type", customSub.getType(), result.getType());
        assertEquals("Incorrect extends", customSub.getExtends(),
            result.getExtends());
        assertEquals("Incorrect 'restricted' value", customSub.isRestricted(),
            result.isRestricted());

        assertSame("Result was not registered in the module config", result,
            moduleConfig.findFormBeanConfig("customSub"));
    }

    /**
     * Make sure processFormBeanConfigClass() returns what it was given if the
     * form passed to it doesn't extend anything.
     */
    public void testProcessFormBeanConfigClassNoExtends()
        throws Exception {
        moduleConfig.addFormBeanConfig(baseFormBean);

        FormBeanConfig result = null;

        try {
            result =
                actionServlet.processFormBeanConfigClass(baseFormBean,
                    moduleConfig);
        } catch (UnavailableException e) {
            fail("An exception should not be thrown when there's nothing to do");
        }

        assertSame("Result should be the same as the input.", baseFormBean,
            result);
    }

    /**
     * Make sure processFormBeanConfigClass() returns the same class instance
     * if the base config isn't using a custom class.
     */
    public void testProcessFormBeanConfigClassSubFormCustomClass()
        throws Exception {
        moduleConfig.addFormBeanConfig(baseFormBean);

        FormBeanConfig customSub = new FormBeanConfig();

        customSub.setName("customSub");
        customSub.setExtends("baseForm");
        moduleConfig.addFormBeanConfig(customSub);

        FormBeanConfig result =
            actionServlet.processFormBeanConfigClass(customSub, moduleConfig);

        assertSame("The instance returned should be the param given it.",
            customSub, result);
    }

    /**
     * Make sure the code throws the correct exception when it can't create an
     * instance of the base config's custom class.
     */
    public void notestProcessFormBeanConfigClassError()
        throws Exception {
        CustomFormBeanConfigArg customBase =
            new CustomFormBeanConfigArg("customBase");

        moduleConfig.addFormBeanConfig(customBase);

        FormBeanConfig customSub = new FormBeanConfig();

        customSub.setName("customSub");
        customSub.setExtends("customBase");
        moduleConfig.addFormBeanConfig(customSub);

        try {
            actionServlet.processFormBeanConfigClass(customSub, moduleConfig);
            fail("Exception should be thrown");
        } catch (UnavailableException e) {
            // success
        } catch (Exception e) {
            fail("Unexpected exception thrown.");
        }
    }

    /**
     * Test the case where the subform has already specified its own form bean
     * config class.  If the code still attempts to create a new instance, an
     * error will be thrown.
     */
    public void testProcessFormBeanConfigClassOverriddenSubFormClass()
        throws Exception {
        CustomFormBeanConfigArg customBase =
            new CustomFormBeanConfigArg("customBase");

        moduleConfig.addFormBeanConfig(customBase);

        FormBeanConfig customSub = new CustomFormBeanConfigArg("customSub");

        customSub.setExtends("customBase");
        moduleConfig.addFormBeanConfig(customSub);

        try {
            actionServlet.processFormBeanConfigClass(customSub, moduleConfig);
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
    }

    // -------------------------------------------------- ExceptionConfig Tests

    /**
     * Test that nothing fails if there are no extensions.
     */
    public void testInitModuleExceptionConfigsNoExtends()
        throws ServletException {
        moduleConfig.addExceptionConfig(baseException);

        try {
            actionServlet.initModuleExceptionConfigs(moduleConfig);
        } catch (Exception e) {
            fail("Unexpected exception caught.");
        }
    }

    /**
     * Test that initModuleExceptionConfigs throws an exception when a handler
     * with a null key is present.
     */
    public void testInitModuleExceptionConfigsNullFormType()
        throws ServletException {
        ExceptionConfig handler = new ExceptionConfig();

        handler.setType("java.lang.NullPointerException");
        moduleConfig.addExceptionConfig(handler);

        try {
            actionServlet.initModuleExceptionConfigs(moduleConfig);
            fail("An exception should've been thrown here.");
        } catch (UnavailableException e) {
            // success
        } catch (Exception e) {
            fail("Unrecognized exception thrown: " + e);
        }
    }

    /**
     * Test that processExceptionExtension() calls processExtends()
     */
    public void testProcessExceptionExtension()
        throws ServletException {
        CustomExceptionConfig handler = new CustomExceptionConfig();

        handler.setType("java.lang.NullPointerException");
        moduleConfig.addExceptionConfig(handler);
        actionServlet.processExceptionExtension(handler, moduleConfig, null);

        assertTrue("processExtends() was not called",
            handler.processExtendsCalled);
    }

    /**
     * Make sure processExceptionConfigClass() returns an instance of the
     * correct class if the base config is using a custom class.
     */
    public void testProcessExceptionConfigClass()
        throws Exception {
        CustomExceptionConfig customBase = new CustomExceptionConfig();

        customBase.setType("java.lang.NullPointerException");
        customBase.setKey("msg.exception.npe");
        moduleConfig.addExceptionConfig(customBase);

        ExceptionConfig customSub = new ExceptionConfig();

        customSub.setType("java.lang.IllegalStateException");
        customSub.setExtends("java.lang.NullPointerException");
        moduleConfig.addExceptionConfig(customSub);

        ExceptionConfig result =
            actionServlet.processExceptionConfigClass(customSub, moduleConfig,
                null);

        assertTrue("Incorrect class of exception config",
            result instanceof CustomExceptionConfig);
        assertEquals("Incorrect type", customSub.getType(), result.getType());
        assertEquals("Incorrect key", customSub.getKey(), result.getKey());
        assertEquals("Incorrect extends", customSub.getExtends(),
            result.getExtends());

        assertSame("Result was not registered in the module config", result,
            moduleConfig.findExceptionConfig("java.lang.IllegalStateException"));
    }

    /**
     * Make sure processExceptionConfigClass() returns what it was given if
     * the handler passed to it doesn't extend anything.
     */
    public void testProcessExceptionConfigClassNoExtends()
        throws Exception {
        moduleConfig.addExceptionConfig(baseException);

        ExceptionConfig result = null;

        try {
            result =
                actionServlet.processExceptionConfigClass(baseException,
                    moduleConfig, null);
        } catch (UnavailableException e) {
            fail("An exception should not be thrown when there's nothing to do");
        }

        assertSame("Result should be the same as the input.", baseException,
            result);
    }

    /**
     * Make sure processExceptionConfigClass() returns the same class instance
     * if the base config isn't using a custom class.
     */
    public void testProcessExceptionConfigClassSubConfigCustomClass()
        throws Exception {
        moduleConfig.addExceptionConfig(baseException);

        ExceptionConfig customSub = new ExceptionConfig();

        customSub.setType("java.lang.IllegalStateException");
        customSub.setExtends("java.lang.NullPointerException");
        moduleConfig.addExceptionConfig(customSub);

        ExceptionConfig result =
            actionServlet.processExceptionConfigClass(customSub, moduleConfig,
                null);

        assertSame("The instance returned should be the param given it.",
            customSub, result);
    }

    /**
     * Make sure the code throws the correct exception when it can't create an
     * instance of the base config's custom class.
     */
    public void notestProcessExceptionConfigClassError()
        throws Exception {
        ExceptionConfig customBase =
            new CustomExceptionConfigArg("java.lang.NullPointerException");

        moduleConfig.addExceptionConfig(customBase);

        ExceptionConfig customSub = new ExceptionConfig();

        customSub.setType("java.lang.IllegalStateException");
        customSub.setExtends("java.lang.NullPointerException");
        moduleConfig.addExceptionConfig(customSub);

        try {
            actionServlet.processExceptionConfigClass(customSub, moduleConfig,
                null);
            fail("Exception should be thrown");
        } catch (UnavailableException e) {
            // success
        } catch (Exception e) {
            fail("Unexpected exception thrown.");
        }
    }

    /**
     * Test the case where the subconfig has already specified its own config
     * class.  If the code still attempts to create a new instance, an error
     * will be thrown.
     */
    public void testProcessExceptionConfigClassOverriddenSubFormClass()
        throws Exception {
        moduleConfig.addExceptionConfig(baseException);

        ExceptionConfig customSub =
            new CustomExceptionConfigArg("java.lang.IllegalStateException");

        customSub.setExtends("java.lang.NullPointerException");
        moduleConfig.addExceptionConfig(customSub);

        try {
            actionServlet.processExceptionConfigClass(customSub, moduleConfig,
                null);
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
    }

    // ---------------------------------------------------- ForwardConfig Tests

    /**
     * Test that nothing fails if there are no extensions.
     */
    public void testInitModuleForwardConfigsNoExtends()
        throws ServletException {
        moduleConfig.addForwardConfig(baseForward);

        try {
            actionServlet.initModuleForwards(moduleConfig);
        } catch (Exception e) {
            fail("Unexpected exception caught.");
        }
    }

    /**
     * Test that initModuleForwards throws an exception when a forward with a
     * null path is present.
     */
    public void testInitModuleForwardsNullFormType()
        throws ServletException {
        ActionForward forward = new ActionForward("success", null, false);

        moduleConfig.addForwardConfig(forward);

        try {
            actionServlet.initModuleForwards(moduleConfig);
            fail("An exception should've been thrown here.");
        } catch (UnavailableException e) {
            // success
        } catch (Exception e) {
            fail("Unrecognized exception thrown: " + e);
        }
    }

    /**
     * Test that processForwardExtension() calls processExtends()
     */
    public void testProcessForwardExtension()
        throws ServletException {
        CustomForwardConfig forward =
            new CustomForwardConfig("forward", "/forward.jsp");

        moduleConfig.addForwardConfig(forward);
        actionServlet.processForwardExtension(forward, moduleConfig, null);

        assertTrue("processExtends() was not called",
            forward.processExtendsCalled);
    }

    /**
     * Make sure processForwardConfigClass() returns an instance of the
     * correct class if the base config is using a custom class.
     */
    public void testProcessForwardConfigClass()
        throws Exception {
        CustomForwardConfig customBase =
            new CustomForwardConfig("success", "/success.jsp");

        moduleConfig.addForwardConfig(customBase);

        ActionForward customSub = new ActionForward();

        customSub.setName("failure");
        customSub.setExtends("success");
        moduleConfig.addForwardConfig(customSub);

        ForwardConfig result =
            actionServlet.processForwardConfigClass(customSub, moduleConfig,
                null);

        assertTrue("Incorrect class of forward config",
            result instanceof CustomForwardConfig);
        assertEquals("Incorrect name", customSub.getName(), result.getName());
        assertEquals("Incorrect path", customSub.getPath(), result.getPath());
        assertEquals("Incorrect extends", customSub.getExtends(),
            result.getExtends());

        assertSame("Result was not registered in the module config", result,
            moduleConfig.findForwardConfig("failure"));
    }

    /**
     * Make sure processForwardConfigClass() returns what it was given if the
     * forward passed to it doesn't extend anything.
     */
    public void testProcessForwardConfigClassNoExtends()
        throws Exception {
        moduleConfig.addForwardConfig(baseForward);

        ForwardConfig result = null;

        try {
            result =
                actionServlet.processForwardConfigClass(baseForward,
                    moduleConfig, null);
        } catch (UnavailableException e) {
            fail("An exception should not be thrown when there's nothing to do");
        }

        assertSame("Result should be the same as the input.", baseForward,
            result);
    }

    /**
     * Make sure processForwardConfigClass() returns the same class instance
     * if the base config isn't using a custom class.
     */
    public void testProcessForwardConfigClassSubConfigCustomClass()
        throws Exception {
        moduleConfig.addForwardConfig(baseForward);

        ForwardConfig customSub = new ActionForward();

        customSub.setName("failure");
        customSub.setExtends("success");
        moduleConfig.addForwardConfig(customSub);

        ForwardConfig result =
            actionServlet.processForwardConfigClass(customSub, moduleConfig,
                null);

        assertSame("The instance returned should be the param given it.",
            customSub, result);
    }

    /**
     * Make sure the code throws the correct exception when it can't create an
     * instance of the base config's custom class.
     */
    public void notestProcessForwardConfigClassError()
        throws Exception {
        ForwardConfig customBase =
            new CustomForwardConfigArg("success", "/success.jsp");

        moduleConfig.addForwardConfig(customBase);

        ForwardConfig customSub = new ActionForward();

        customSub.setName("failure");
        customSub.setExtends("success");
        moduleConfig.addForwardConfig(customSub);

        try {
            actionServlet.processForwardConfigClass(customSub, moduleConfig,
                null);
            fail("Exception should be thrown");
        } catch (UnavailableException e) {
            // success
        } catch (Exception e) {
            fail("Unexpected exception thrown.");
        }
    }

    /**
     * Test the case where the subconfig has already specified its own config
     * class.  If the code still attempts to create a new instance, an error
     * will be thrown.
     */
    public void testProcessForwardConfigClassOverriddenSubConfigClass()
        throws Exception {
        moduleConfig.addForwardConfig(baseForward);

        ForwardConfig customSub =
            new CustomForwardConfigArg("failure", "/failure.jsp");

        customSub.setExtends("success");
        moduleConfig.addForwardConfig(customSub);

        try {
            actionServlet.processForwardConfigClass(customSub, moduleConfig,
                null);
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
    }

    // --------------------------------------------------- ActionConfig Tests

    /**
     * Test that nothing fails if there are no extensions.
     */
    public void testInitModuleActionConfigsNoExtends()
        throws ServletException {
        moduleConfig.addActionConfig(baseAction);

        try {
            actionServlet.initModuleActions(moduleConfig);
        } catch (Exception e) {
            fail("Unexpected exception caught.");
        }
    }

    /**
     * Test that processActionConfigExtension() calls processExtends()
     */
    public void testProcessActionExtension()
        throws ServletException {
        CustomActionConfig action = new CustomActionConfig("/action");

        moduleConfig.addActionConfig(action);
        actionServlet.processActionConfigExtension(action, moduleConfig);

        assertTrue("processExtends() was not called",
            action.processExtendsCalled);
    }

    /**
     * Test that an ActionConfig's ForwardConfig can inherit from a
     * global ForwardConfig.
     */
    public void testProcessActionExtensionWithForwardConfig()
        throws ServletException {
        ForwardConfig forwardConfig = new ForwardConfig();
        forwardConfig.setName("sub");
        forwardConfig.setExtends("success");
        baseAction.addForwardConfig(forwardConfig);

        moduleConfig.addActionConfig(baseAction);
        moduleConfig.addForwardConfig(baseForward);
        actionServlet.processActionConfigExtension(baseAction, moduleConfig);

        forwardConfig = baseAction.findForwardConfig("sub");

        assertEquals("'sub' forward's inheritance was not processed.",
            baseForward.getPath(), forwardConfig.getPath());
    }

    /**
     * Test that an ActionConfig's ExceptionConfig can inherit from a
     * global ExceptionConfig.
     */
    public void testProcessActionExtensionWithExceptionConfig()
        throws ServletException {
        ExceptionConfig exceptionConfig = new ExceptionConfig();
        exceptionConfig.setType("SomeException");
        exceptionConfig.setExtends("java.lang.NullPointerException");
        baseAction.addExceptionConfig(exceptionConfig);

        moduleConfig.addActionConfig(baseAction);
        moduleConfig.addExceptionConfig(baseException);
        actionServlet.processActionConfigExtension(baseAction, moduleConfig);

        exceptionConfig = baseAction.findExceptionConfig("SomeException");

        assertEquals("SomeException's inheritance was not processed.",
            baseException.getKey(), exceptionConfig.getKey());
    }

    /**
     * Make sure processActionConfigClass() returns an instance of the correct
     * class if the base config is using a custom class.
     */
    public void testProcessActionConfigClass()
        throws Exception {
        CustomActionConfig customBase = new CustomActionConfig("/base");

        moduleConfig.addActionConfig(customBase);

        ActionMapping customSub = new ActionMapping();

        customSub.setPath("/sub");
        customSub.setExtends("/base");
        moduleConfig.addActionConfig(customSub);

        ActionConfig result =
            actionServlet.processActionConfigClass(customSub, moduleConfig);

        assertTrue("Incorrect class of action config",
            result instanceof CustomActionConfig);
        assertEquals("Incorrect path", customSub.getPath(), result.getPath());
        assertEquals("Incorrect extends", customSub.getExtends(),
            result.getExtends());

        assertSame("Result was not registered in the module config", result,
            moduleConfig.findActionConfig("/sub"));
    }

    /**
     * Make sure processActionConfigClass() returns what it was given if the
     * action passed to it doesn't extend anything.
     */
    public void testProcessActionConfigClassNoExtends()
        throws Exception {
        moduleConfig.addActionConfig(baseAction);

        ActionConfig result = null;

        try {
            result =
                actionServlet.processActionConfigClass(baseAction, moduleConfig);
        } catch (UnavailableException e) {
            fail("An exception should not be thrown here");
        }

        assertSame("Result should be the same as the input.", baseAction, result);
    }

    /**
     * Make sure processActionConfigClass() returns the same class instance if
     * the base config isn't using a custom class.
     */
    public void testProcessActionConfigClassSubConfigCustomClass()
        throws Exception {
        moduleConfig.addActionConfig(baseAction);

        ActionConfig customSub = new ActionMapping();

        customSub.setPath("/sub");
        customSub.setExtends("/index");
        moduleConfig.addActionConfig(customSub);

        ActionConfig result =
            actionServlet.processActionConfigClass(customSub, moduleConfig);

        assertSame("The instance returned should be the param given it.",
            customSub, result);
    }

    /**
     * Make sure the code throws the correct exception when it can't create an
     * instance of the base config's custom class.
     */
    public void notestProcessActionConfigClassError()
        throws Exception {
        ActionConfig customBase = new CustomActionConfigArg("/index");

        moduleConfig.addActionConfig(customBase);

        ActionConfig customSub = new ActionMapping();

        customSub.setPath("/sub");
        customSub.setExtends("/index");
        moduleConfig.addActionConfig(customSub);

        try {
            actionServlet.processActionConfigClass(customSub, moduleConfig);
            fail("Exception should be thrown");
        } catch (UnavailableException e) {
            // success
        } catch (Exception e) {
            fail("Unexpected exception thrown.");
        }
    }

    /**
     * Test the case where the subconfig has already specified its own config
     * class.  If the code still attempts to create a new instance, an error
     * will be thrown.
     */
    public void testProcessActionConfigClassOverriddenSubConfigClass()
        throws Exception {
        moduleConfig.addActionConfig(baseAction);

        ActionConfig customSub = new CustomActionConfigArg("/sub");

        customSub.setExtends("/index");
        moduleConfig.addActionConfig(customSub);

        try {
            actionServlet.processActionConfigClass(customSub, moduleConfig);
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
    }

    /**
     * Used for testing custom FormBeanConfig classes.
     */
    public static class CustomFormBeanConfig extends FormBeanConfig {
        public boolean processExtendsCalled = false;

        public CustomFormBeanConfig() {
            super();
        }

        /**
         * Set a flag so we know this method was called.
         */
        public void processExtends(ModuleConfig moduleConfig)
            throws ClassNotFoundException, IllegalAccessException,
                InstantiationException {
            processExtendsCalled = true;
        }
    }

    /**
     * Used to test cases where the subclass cannot be created with a no-arg
     * constructor.
     */
    private class CustomFormBeanConfigArg extends FormBeanConfig {
        CustomFormBeanConfigArg(String name) {
            super();
            setName(name);
        }
    }

    /**
     * Used for testing custom ExceptionConfig classes.
     */
    public static class CustomExceptionConfig extends ExceptionConfig {
        public boolean processExtendsCalled = false;

        public CustomExceptionConfig() {
            super();
        }

        /**
         * Set a flag so we know this method was called.
         */
        public void processExtends(ModuleConfig moduleConfig,
            ActionConfig actionConfig)
            throws ClassNotFoundException, IllegalAccessException,
                InstantiationException {
            processExtendsCalled = true;
        }
    }

    /**
     * Used to test cases where the subclass cannot be created with a no-arg
     * constructor.
     */
    private class CustomExceptionConfigArg extends ExceptionConfig {
        CustomExceptionConfigArg(String type) {
            super();
            setType(type);
        }
    }

    /**
     * Used for testing custom ForwardConfig classes.
     */
    public static class CustomForwardConfig extends ForwardConfig {
        public boolean processExtendsCalled = false;

        public CustomForwardConfig() {
            super();
        }

        public CustomForwardConfig(String name, String path) {
            super(name, path, false);
        }

        /**
         * Set a flag so we know this method was called.
         */
        public void processExtends(ModuleConfig moduleConfig,
            ActionConfig actionConfig)
            throws ClassNotFoundException, IllegalAccessException,
                InstantiationException {
            processExtendsCalled = true;
        }
    }

    /**
     * Used to test cases where the subclass cannot be created with a no-arg
     * constructor.
     */
    private class CustomForwardConfigArg extends ForwardConfig {
        CustomForwardConfigArg(String name, String path) {
            super();
            setName(name);
            setPath(path);
        }
    }

    /**
     * Used for testing custom ActionConfig classes.
     */
    public static class CustomActionConfig extends ActionConfig {
        public boolean processExtendsCalled = false;

        public CustomActionConfig() {
            super();
        }

        public CustomActionConfig(String path) {
            super();
            setPath(path);
        }

        /**
         * Set a flag so we know this method was called.
         */
        public void processExtends(ModuleConfig moduleConfig)
            throws ClassNotFoundException, IllegalAccessException,
                InstantiationException {
            processExtendsCalled = true;
        }
    }

    /**
     * Used to test cases where the subclass cannot be created with a no-arg
     * constructor.
     */
    private class CustomActionConfigArg extends ActionConfig {
        CustomActionConfigArg(String path) {
            super();
            setPath(path);
        }
    }

    // [...]
}
