/*
 * $Id: TestActionConfig.java 480593 2006-11-29 15:17:52Z niallp $
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
package org.apache.struts.config;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.lang.reflect.InvocationTargetException;

/**
 * Unit tests for the <code>org.apache.struts.config.ActionConfig</code>
 * class.  Currently only contains code to test the methods that support
 * configuration inheritance.
 *
 * @version $Rev: 480593 $ $Date: 2005-05-25 19:35:00 -0400 (Wed, 25 May 2005)
 *          $
 */
public class TestActionConfig extends TestCase {
    // ----------------------------------------------------- Instance Variables

    /**
     * The ModuleConfig we'll use.
     */
    protected ModuleConfig config = null;

    /**
     * The common base we'll use.
     */
    protected ActionConfig baseConfig = null;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new instance of this test case.
     *
     * @param name Name of the test case
     */
    public TestActionConfig(String name) {
        super(name);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Set up instance variables required by this test case.
     */
    public void setUp() {
        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();

        config = factoryObject.createModuleConfig("");

        // setup the base form
        baseConfig = new ActionConfig();
        baseConfig.setPath("/base");
        baseConfig.setType("org.apache.struts.actions.DummyAction");

        // set up success and failure forward
        ForwardConfig forward =
            new ForwardConfig("success", "/success.jsp", false);

        baseConfig.addForwardConfig(forward);

        forward = new ForwardConfig("failure", "/failure.jsp", false);
        forward.setProperty("forwardCount", "10");
        baseConfig.addForwardConfig(forward);

        // setup an exception handler
        ExceptionConfig exceptionConfig = new ExceptionConfig();

        exceptionConfig.setType("java.sql.SQLException");
        exceptionConfig.setKey("msg.exception.sql");
        exceptionConfig.setProperty("exceptionCount", "10");
        baseConfig.addExceptionConfig(exceptionConfig);

        // set some arbitrary properties
        baseConfig.setProperty("label", "base");
        baseConfig.setProperty("version", "1a");

        // register it to our config
        config.addActionConfig(baseConfig);
    }

    /**
     * Return the tests included in this test suite.
     */
    public static Test suite() {
        return (new TestSuite(TestActionConfig.class));
    }

    /**
     * Tear down instance variables required by this test case.
     */
    public void tearDown() {
        config = null;
        baseConfig = null;
    }

    // ------------------------------------------------------- Individual Tests

    /**
     * Basic check that shouldn't detect circular inheritance.
     */
    public void testCheckCircularInheritance() {
        ActionConfig child = new ActionConfig();

        child.setPath("/child");
        child.setExtends("/base");

        ActionConfig grandChild = new ActionConfig();

        grandChild.setPath("/grandChild");
        grandChild.setExtends("/child");

        config.addActionConfig(child);
        config.addActionConfig(grandChild);

        assertTrue("Circular inheritance shouldn't have been detected",
            !grandChild.checkCircularInheritance(config));
    }

    /**
     * Basic check that should detect circular inheritance.
     */
    public void testCheckCircularInheritanceError() {
        ActionConfig child = new ActionConfig();

        child.setPath("/child");
        child.setExtends("/base");

        ActionConfig grandChild = new ActionConfig();

        grandChild.setPath("/grandChild");
        grandChild.setExtends("/child");

        // establish the circular relationship with base
        baseConfig.setExtends("/grandChild");

        config.addActionConfig(child);
        config.addActionConfig(grandChild);

        assertTrue("Circular inheritance should've been detected",
            grandChild.checkCircularInheritance(config));
    }

    /**
     * Test that processExtends() makes sure that a base action's own
     * extension has been processed.
     */
    public void testProcessExtendsActionExtends()
        throws Exception {
        CustomActionConfig first = new CustomActionConfig();

        first.setPath("/first");

        CustomActionConfig second = new CustomActionConfig();

        second.setPath("/second");
        second.setExtends("/first");

        config.addActionConfig(first);
        config.addActionConfig(second);

        // set baseConfig to extend second
        baseConfig.setExtends("/second");

        baseConfig.processExtends(config);

        assertTrue("The first action's processExtends() wasn't called",
            first.processExtendsCalled);
        assertTrue("The second action's processExtends() wasn't called",
            second.processExtendsCalled);
    }

    /**
     * Make sure that correct exception is thrown if a base action can't be
     * found.
     */
    public void testProcessExtendsMissingAction()
        throws Exception {
        baseConfig.setExtends("/someMissingAction");

        try {
            baseConfig.processExtends(config);
            fail(
                "An exception should be thrown if a super form can't be found.");
        } catch (NullPointerException e) {
            // succeed
        } catch (InstantiationException e) {
            fail("Unrecognized exception thrown.");
        }
    }

    /**
     * Test a typical form bean configuration extension where various forwards
     * and exception handlers should be inherited from a base form. This
     * method checks all the subelements.
     */
    public void testInheritFrom()
        throws Exception {
        // create a basic subform
        ActionConfig subConfig = new ActionConfig();
        String subConfigPath = "subConfig";

        subConfig.setPath(subConfigPath);
        subConfig.setExtends("/base");

        // override success
        ForwardConfig forward = new ForwardConfig();

        forward.setName("success");
        forward.setPath("/newSuccess.jsp");
        forward.setRedirect(true);
        subConfig.addForwardConfig(forward);

        // add an exception handler
        ExceptionConfig handler = new ExceptionConfig();

        handler.setType("java.lang.NullPointerException");
        handler.setKey("msg.exception.npe");
        subConfig.addExceptionConfig(handler);

        // override arbitrary "label" property
        subConfig.setProperty("label", "sub");

        config.addActionConfig(subConfig);

        subConfig.inheritFrom(baseConfig);

        // check that our subConfig is still the one in the config
        assertSame("subConfig no longer in ModuleConfig", subConfig,
            config.findActionConfig("subConfig"));

        // check our configured sub config
        assertNotNull("Action type was not inherited", subConfig.getType());
        assertEquals("Wrong config path", subConfigPath, subConfig.getPath());
        assertEquals("Wrong config type", baseConfig.getType(),
            subConfig.getType());

        // check our forwards
        ForwardConfig[] forwards = subConfig.findForwardConfigs();

        assertEquals("Wrong forwards count", 2, forwards.length);

        forward = subConfig.findForwardConfig("success");
        assertNotNull("'success' forward was not found", forward);
        assertEquals("Wrong path for success", "/newSuccess.jsp",
            forward.getPath());

        forward = subConfig.findForwardConfig("failure");

        ForwardConfig origForward = baseConfig.findForwardConfig("failure");

        assertNotNull("'failure' forward was not inherited", forward);
        assertEquals("Wrong type for 'failure'", origForward.getPath(),
            forward.getPath());
        assertEquals("Arbitrary property not copied",
            origForward.getProperty("forwardCount"),
            forward.getProperty("forwardCount"));

        // check our exceptions
        ExceptionConfig[] handlers = subConfig.findExceptionConfigs();

        assertEquals("Wrong exception config count", 2, handlers.length);

        handler = subConfig.findExceptionConfig("java.sql.SQLException");

        ExceptionConfig origHandler =
            baseConfig.findExceptionConfig("java.sql.SQLException");

        assertNotNull("'SQLException' handler was not found", handler);
        assertEquals("Wrong key for 'SQLException'", origHandler.getKey(),
            handler.getKey());
        assertEquals("Arbitrary property not copied",
            origHandler.getProperty("exceptionCount"),
            handler.getProperty("exceptionCount"));

        handler =
            subConfig.findExceptionConfig("java.lang.NullPointerException");
        assertNotNull("'NullPointerException' handler disappeared", handler);

        // check the arbitrary properties
        String version = subConfig.getProperty("version");

        assertEquals("Arbitrary property 'version' wasn't inherited", "1a",
            version);

        String label = subConfig.getProperty("label");

        assertEquals("Arbitrary property 'label' shouldn't have changed",
            "sub", label);
    }

    /**
     * Make sure that correct exception is thrown if a base action can't be
     * found.
     */
    public void testInheritBoolean()
        throws Exception {

        ActionConfig parentConfig = new ActionConfig();
        parentConfig.setPath("/parent");
        ActionConfig childConfig  = null;

        // Test if boolean is NOT set it IS inherited
        parentConfig.setValidate(true);
        parentConfig.setCancellable(true);
        childConfig = new ActionConfig();
        childConfig.inheritFrom(parentConfig);
        assertEquals("default validate inherit true", true, childConfig.getValidate());
        assertEquals("default cancellable inherit true", true, childConfig.getValidate());

        // Test if boolean is NOT set it IS inherited
        parentConfig.setValidate(false);
        parentConfig.setCancellable(false);
        childConfig = new ActionConfig();
        childConfig.inheritFrom(parentConfig);
        assertEquals("default validate inherit false", false, childConfig.getValidate());
        assertEquals("default cancellable inherit false", false, childConfig.getValidate());

        // Test if boolean IS set it is NOT inherited
        parentConfig.setValidate(true);
        parentConfig.setCancellable(true);
        childConfig = new ActionConfig();
        childConfig.setValidate(false);
        childConfig.setCancellable(false);
        childConfig.inheritFrom(parentConfig);
        assertEquals("set validate (not inherit true)", false, childConfig.getValidate());
        assertEquals("set cancellable (not inherit false)", false, childConfig.getValidate());

        // Test if boolean IS set it is NOT inherited
        parentConfig.setValidate(false);
        parentConfig.setCancellable(false);
        childConfig = new ActionConfig();
        childConfig.setValidate(true);
        childConfig.setCancellable(true);
        childConfig.inheritFrom(parentConfig);
        assertEquals("set validate (not inherit false)", true, childConfig.getValidate());
        assertEquals("set cancellable (not inherit false)", true, childConfig.getValidate());

    }

    /**
     * Used to detect that ActionConfig is making the right calls.
     */
    public static class CustomActionConfig extends ActionConfig {
        boolean processExtendsCalled = false;

        public void processExtends(ModuleConfig moduleConfig)
            throws ClassNotFoundException, IllegalAccessException,
                InstantiationException, InvocationTargetException {
            super.processExtends(moduleConfig);
            processExtendsCalled = true;
        }
    }
}
