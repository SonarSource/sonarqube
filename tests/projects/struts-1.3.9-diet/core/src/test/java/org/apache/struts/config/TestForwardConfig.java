/*
 * $Id: TestForwardConfig.java 471754 2006-11-06 14:55:09Z husted $
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

/**
 * <p>Unit tests for ForwardConfig.  Currently contains tests for methods
 * supporting configuration inheritance.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-05-21 19:06:53 -0400 (Sat, 21 May 2005)
 *          $
 */
public class TestForwardConfig extends TestCase {
    // ----------------------------------------------------- Instance Variables

    /**
     * The ModuleConfig we'll use.
     */
    protected ModuleConfig moduleConfig = null;

    /**
     * The common base we'll use.
     */
    protected ForwardConfig baseConfig = null;

    /**
     * The common subForward we'll use.
     */
    protected ForwardConfig subConfig = null;

    /**
     * A ForwardConfig we'll use to test cases where a ForwardConfig declared
     * for an action extends a ForwardConfig declared globally, with both
     * ForwardConfigs using the same name.
     */
    protected ForwardConfig actionBaseConfig = null;

    /**
     * An action mapping we'll use within tests.
     */
    protected ActionConfig actionConfig = null;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new instance of this test case.
     *
     * @param name Name of the test case
     */
    public TestForwardConfig(String name) {
        super(name);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Set up instance variables required by this test case.
     */
    public void setUp() {
        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();

        moduleConfig = factoryObject.createModuleConfig("");

        // Setup the base and sub forwards, with sub extending base
        baseConfig = new ForwardConfig();
        baseConfig.setName("baseConfig");
        baseConfig.setPath("/somePage.jsp");

        subConfig = new ForwardConfig();
        subConfig.setName("subConfig");
        subConfig.setExtends("baseConfig");
        subConfig.setRedirect(true);

        actionBaseConfig = new ForwardConfig();
        actionBaseConfig.setName("baseConfig");
        actionBaseConfig.setExtends("baseConfig");
        actionBaseConfig.setModule("/other");

        // Setup the default action config
        actionConfig = new ActionConfig();
        actionConfig.setPath("/index");
        moduleConfig.addActionConfig(actionConfig);

        // No forward configs are registered to either module or action configs.
        // Each test will determine where it needs these configs, if at all.
    }

    /**
     * Return the tests included in this test suite.
     */
    public static Test suite() {
        return (new TestSuite(TestForwardConfig.class));
    }

    /**
     * Tear down instance variables required by this test case.
     */
    public void tearDown() {
        moduleConfig = null;
        baseConfig = null;
    }

    // ------------------------------------------------------- Individual Tests

    /**
     * Make sure checkCircularInheritance() works as expected where there is
     * no inheritance set up.
     */
    public void testCheckCircularInheritanceNoExtends() {
        moduleConfig.addForwardConfig(baseConfig);

        boolean result =
            baseConfig.checkCircularInheritance(moduleConfig, null);

        assertTrue("Incorrect result", !result);
    }

    /**
     * Test checkCircularInheritance() for when there is no circular
     * inheritance.
     */
    public void testCheckCircularInheritanceNoConflicts() {
        moduleConfig.addForwardConfig(baseConfig);
        moduleConfig.addForwardConfig(subConfig);

        boolean result = subConfig.checkCircularInheritance(moduleConfig, null);

        assertTrue("Incorrect result", !result);
    }

    /**
     * Test checkCircularInheritance() for circular inheritance between global
     * forwards.
     */
    public void testCheckCircularInheritanceBasicGlobal() {
        moduleConfig.addForwardConfig(subConfig);
        moduleConfig.addForwardConfig(baseConfig);

        // set the baseConfig to extend subConfig
        baseConfig.setExtends("subConfig");

        boolean result = subConfig.checkCircularInheritance(moduleConfig, null);

        assertTrue("Circular inheritance not detected.", result);
    }

    /**
     * Test checkCircularInheritance() for circular inheritance between global
     * forwards.
     */
    public void testCheckCircularInheritanceGlobal2Levels() {
        moduleConfig.addForwardConfig(baseConfig);
        moduleConfig.addForwardConfig(subConfig);

        ForwardConfig grand = new ForwardConfig();

        grand.setName("grandConfig");
        grand.setExtends("subConfig");
        moduleConfig.addForwardConfig(grand);

        // set the baseConfig to extend grandConfig
        baseConfig.setExtends("grandConfig");

        boolean result = grand.checkCircularInheritance(moduleConfig, null);

        assertTrue("Circular inheritance not detected.", result);
    }

    /**
     * Test checkCircularInheritance() for circular inheritance between
     * forwards in an action.
     */
    public void testCheckCircularInheritanceActionForwardsNoConflict() {
        actionConfig.addForwardConfig(baseConfig);
        actionConfig.addForwardConfig(subConfig);

        boolean result =
            subConfig.checkCircularInheritance(moduleConfig, actionConfig);

        assertTrue("Incorrect result", !result);
    }

    /**
     * Test checkCircularInheritance() for circular inheritance between
     * forwards in an action.
     */
    public void testCheckCircularInheritanceActionForwardsBasic() {
        actionConfig.addForwardConfig(baseConfig);
        actionConfig.addForwardConfig(subConfig);

        // set base to extend sub
        baseConfig.setExtends("subConfig");

        boolean result =
            subConfig.checkCircularInheritance(moduleConfig, actionConfig);

        assertTrue("Circular inheritance not detected.", result);
    }

    /**
     * Test checkCircularInheritance() for circular inheritance between a
     * forward declared in an action and a global forward.
     */
    public void testCheckCircularInheritanceActionForwardExtendGlobal() {
        actionConfig.addForwardConfig(subConfig);
        moduleConfig.addForwardConfig(baseConfig);

        boolean result =
            subConfig.checkCircularInheritance(moduleConfig, actionConfig);

        assertTrue("Incorrect result", !result);
    }

    /**
     * Test checkCircularInheritance() for circular inheritance between a
     * forward declared in an action and a global forward and both forwards
     * have the same name.
     */
    public void testCheckCircularInheritanceActionForwardExtendGlobalSameName() {
        moduleConfig.addForwardConfig(baseConfig);
        actionConfig.addForwardConfig(actionBaseConfig);

        boolean result =
            actionBaseConfig.checkCircularInheritance(moduleConfig, actionConfig);

        assertTrue("Incorrect result", !result);
    }

    /**
     * Make sure processExtends() throws an error when the config is already
     * configured.
     */
    public void testProcessExtendsConfigured()
        throws Exception {
        baseConfig.configured = true;
        moduleConfig.addForwardConfig(baseConfig);

        try {
            baseConfig.processExtends(moduleConfig, null);
            fail(
                "processExtends should not succeed when object is already configured");
        } catch (IllegalStateException e) {
            // success
        }
    }

    /**
     * Test processExtends() when nothing is extended.
     */
    public void testProcessExtendsNoExtension()
        throws Exception {
        String path = baseConfig.getPath();
        String module = baseConfig.getModule();
        String name = baseConfig.getName();
        String inherit = baseConfig.getExtends();
        boolean redirect = baseConfig.getRedirect();

        moduleConfig.addForwardConfig(baseConfig);
        baseConfig.processExtends(moduleConfig, null);

        assertEquals("Path shouldn't have changed", path, baseConfig.getPath());
        assertEquals("Module shouldn't have changed", module,
            baseConfig.getModule());
        assertEquals("Name shouldn't have changed", name, baseConfig.getName());
        assertEquals("Extends shouldn't have changed", inherit,
            baseConfig.getExtends());
        assertEquals("Redirect shouldn't have changed", redirect,
            baseConfig.getRedirect());
    }

    /**
     * Test processExtends() with a basic extension.
     */
    public void testProcessExtendsBasicExtension()
        throws Exception {
        String baseCount = "10";

        baseConfig.setProperty("count", baseCount);

        String baseLabel = "label a";

        baseConfig.setProperty("label", baseLabel);

        String path = baseConfig.getPath();
        String module = baseConfig.getModule();

        String inherit = subConfig.getExtends();
        String name = subConfig.getName();
        boolean redirect = subConfig.getRedirect();

        String subLabel = "label b";

        subConfig.setProperty("label", subLabel);

        moduleConfig.addForwardConfig(baseConfig);
        moduleConfig.addForwardConfig(subConfig);
        subConfig.processExtends(moduleConfig, null);

        assertEquals("Path wasn't inherited", path, subConfig.getPath());
        assertEquals("Module wasn't inherited", module, subConfig.getModule());
        assertEquals("Name shouldn't have changed", name, subConfig.getName());
        assertEquals("Extends shouldn't have changed", inherit,
            subConfig.getExtends());
        assertEquals("Redirect shouldn't have changed", redirect,
            subConfig.getRedirect());
        assertEquals("Arbitrary config property was not inherited", baseCount,
            subConfig.getProperty("count"));
        assertEquals("Overridden config property was not retained", subLabel,
            subConfig.getProperty("label"));
    }

    /**
     * Test processExtends() with a basic extension between an action config
     * and a global config.
     */
    public void testProcessExtendsBasicGlobalExtension()
        throws Exception {
        String path = baseConfig.getPath();
        String module = baseConfig.getModule();

        boolean redirect = subConfig.getRedirect();
        String inherit = subConfig.getExtends();
        String name = subConfig.getName();

        moduleConfig.addForwardConfig(baseConfig);
        actionConfig.addForwardConfig(subConfig);
        subConfig.processExtends(moduleConfig, actionConfig);

        assertEquals("Path wasn't inherited", path, subConfig.getPath());
        assertEquals("Module wasn't inherited", module, subConfig.getModule());
        assertEquals("Name shouldn't have changed", name, subConfig.getName());
        assertEquals("Extends shouldn't have changed", inherit,
            subConfig.getExtends());
        assertEquals("Redirect shouldn't have changed", redirect,
            subConfig.getRedirect());
    }

    /**
     * Test processExtends() with an incorrect setup where a global config
     * attempts to extend an action config.
     */
    public void testProcessExtendsGlobalExtendingAction()
        throws Exception {
        moduleConfig.addForwardConfig(subConfig);
        actionConfig.addForwardConfig(baseConfig);

        try {
            subConfig.processExtends(moduleConfig, actionConfig);
            fail(
                "Should not find config from actionConfig when *this* is global");
        } catch (NullPointerException npe) {
            // succeed
        }
    }

    /**
     * Test processExtends() with an action config that extends a global
     * config with the same name.
     */
    public void testProcessExtendsSameNames()
        throws Exception {
        String path = baseConfig.getPath();
        boolean redirect = baseConfig.getRedirect();

        String module = actionBaseConfig.getModule();
        String inherit = actionBaseConfig.getExtends();
        String name = actionBaseConfig.getName();

        moduleConfig.addForwardConfig(baseConfig);
        actionConfig.addForwardConfig(actionBaseConfig);

        actionBaseConfig.processExtends(moduleConfig, actionConfig);

        assertEquals("Path wasn't inherited", path, actionBaseConfig.getPath());
        assertEquals("Module shouldn't have changed", module,
            actionBaseConfig.getModule());
        assertEquals("Name shouldn't have changed", name,
            actionBaseConfig.getName());
        assertEquals("Extends shouldn't have changed", inherit,
            actionBaseConfig.getExtends());
        assertEquals("Redirect shouldn't have changed", redirect,
            actionBaseConfig.getRedirect());
    }

    /**
     * Test processExtends() where an action ForwardConfig extends another
     * ForwardConfig, which in turn extends a global ForwardConfig with the
     * same name.
     */
    public void testProcessExtendsActionExtendsActionExtendsGlobalWithSameName()
        throws Exception {
        String path = baseConfig.getPath();

        String module = actionBaseConfig.getModule();

        boolean redirect = subConfig.getRedirect();
        String inherit = subConfig.getExtends();
        String name = subConfig.getName();

        moduleConfig.addForwardConfig(baseConfig);
        actionConfig.addForwardConfig(actionBaseConfig);
        actionConfig.addForwardConfig(subConfig);

        subConfig.processExtends(moduleConfig, actionConfig);

        assertTrue("baseConfig's processExtends() should've been called",
            baseConfig.extensionProcessed);
        assertTrue("actionBaseConfig's processExtends() should've been called",
            actionBaseConfig.extensionProcessed);

        assertEquals("Path wasn't inherited", path, subConfig.getPath());
        assertEquals("Module wasn't inherited", module, subConfig.getModule());
        assertEquals("Name shouldn't have changed", name, subConfig.getName());
        assertEquals("Extends shouldn't have changed", inherit,
            subConfig.getExtends());
        assertEquals("Redirect shouldn't have changed", redirect,
            subConfig.getRedirect());
    }
}
