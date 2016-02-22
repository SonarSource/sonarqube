/*
 * $Id: TestModuleConfig.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.commons.digester.Digester;

import java.io.InputStream;

/**
 * Unit tests for the <code>org.apache.struts.config</code> package.
 *
 * @version $Rev: 471754 $ $Date: 2005-03-01 20:26:14 -0500 (Tue, 01 Mar 2005)
 *          $
 */
public class TestModuleConfig extends TestCase {
    // ----------------------------------------------------- Instance Variables

    /**
     * The ModuleConfig we are testing.
     */
    protected ModuleConfig config = null;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new instance of this test case.
     *
     * @param name Name of the test case
     */
    public TestModuleConfig(String name) {
        super(name);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Set up instance variables required by this test case.
     */
    public void setUp() {
        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();

        config = factoryObject.createModuleConfig("");
    }

    /**
     * Return the tests included in this test suite.
     */
    public static Test suite() {
        return (new TestSuite(TestModuleConfig.class));
    }

    /**
     * Tear down instance variables required by this test case.
     */
    public void tearDown() {
        config = null;
    }

    // ------------------------------------------------ Individual Test Methods
    private void parseConfig(String publicId, String entityURL,
        String strutsConfig) {
        // Prepare a Digester for parsing a struts-config.xml file
        Digester digester = new Digester();

        digester.push(config);
        digester.setNamespaceAware(true);
        digester.setValidating(true);
        digester.addRuleSet(new ConfigRuleSet());
        digester.register(publicId,
            this.getClass().getResource(entityURL).toString());

        // Parse the test struts-config.xml file
        try {
            InputStream input =
                this.getClass().getResourceAsStream(strutsConfig);

            assertNotNull("Got an input stream for " + strutsConfig, input);
            digester.parse(input);
            input.close();
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail("Parsing threw exception:  " + t);
        }
    }

    /**
     * Test parsing of a struts-config.xml file.
     */
    public void testParse() {
        testParseBase("-//Apache Software Foundation//DTD Struts Configuration 1.2//EN",
            "/org/apache/struts/resources/struts-config_1_2.dtd",
            "/org/apache/struts/config/struts-config.xml");
    }

    public void testParse1_1() {
        testParseBase("-//Apache Software Foundation//DTD Struts Configuration 1.1//EN",
            "/org/apache/struts/resources/struts-config_1_1.dtd",
            "/org/apache/struts/config/struts-config-1.1.xml");
    }

    public void testParseBase(String publicId, String entityURL,
        String strutsConfig) {
        parseConfig(publicId, entityURL, strutsConfig);

        // Perform assertion tests on the parsed information
        FormBeanConfig[] fbcs = config.findFormBeanConfigs();

        assertNotNull("Found our form bean configurations", fbcs);
        assertEquals("Found three form bean configurations", 3, fbcs.length);

        ForwardConfig[] fcs = config.findForwardConfigs();

        assertNotNull("Found our forward configurations", fcs);
        assertEquals("Found three forward configurations", 3, fcs.length);

        ActionConfig logon = config.findActionConfig("/logon");

        assertNotNull("Found logon action configuration", logon);
        assertEquals("Found correct logon configuration", "logonForm",
            logon.getName());
    }

    /**
     * Tests a struts-config.xml that contains a custom mapping and property.
     */
    public void testCustomMappingParse() {
        // Prepare a Digester for parsing a struts-config.xml file
        testCustomMappingParseBase("-//Apache Software Foundation//DTD Struts Configuration 1.2//EN",
            "/org/apache/struts/resources/struts-config_1_2.dtd",
            "/org/apache/struts/config/struts-config-custom-mapping.xml");
    }

    /**
     * Tests a struts-config.xml that contains a custom mapping and property.
     */
    public void testCustomMappingParse1_1() {
        // Prepare a Digester for parsing a struts-config.xml file
        testCustomMappingParseBase("-//Apache Software Foundation//DTD Struts Configuration 1.1//EN",
            "/org/apache/struts/resources/struts-config_1_1.dtd",
            "/org/apache/struts/config/struts-config-custom-mapping-1.1.xml");
    }

    /**
     * Tests a struts-config.xml that contains a custom mapping and property.
     */
    private void testCustomMappingParseBase(String publicId, String entityURL,
        String strutsConfig) {
        parseConfig(publicId, entityURL, strutsConfig);

        // Perform assertion tests on the parsed information
        CustomMappingTest map =
            (CustomMappingTest) config.findActionConfig("/editRegistration");

        assertNotNull("Cannot find editRegistration mapping", map);
        assertTrue("The custom mapping attribute has not been set",
            map.getPublic());
    }

    /**
     * Test order of action mappings defined perserved.
     */
    public void testPreserveActionMappingsOrder() {
        parseConfig("-//Apache Software Foundation//DTD Struts Configuration 1.2//EN",
            "/org/apache/struts/resources/struts-config_1_2.dtd",
            "/org/apache/struts/config/struts-config.xml");

        String[] paths =
            new String[] {
                "/editRegistration", "/editSubscription", "/logoff", "/logon",
                "/saveRegistration", "/saveSubscription", "/tour"
            };

        ActionConfig[] actions = config.findActionConfigs();

        for (int x = 0; x < paths.length; x++) {
            assertTrue("Action config out of order:" + actions[x].getPath(),
                paths[x].equals(actions[x].getPath()));
        }
    }
}
