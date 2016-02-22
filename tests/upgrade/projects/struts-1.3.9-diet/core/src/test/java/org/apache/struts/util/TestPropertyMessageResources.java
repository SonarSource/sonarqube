/*
 * $Id: TestPropertyMessageResources.java 480549 2006-11-29 12:16:15Z niallp $
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
import junit.framework.TestCase;

import java.util.Locale;
import org.apache.struts.config.MessageResourcesConfig;

/**
 * Unit tests for PropertyMessageResources.
 *
 * @version $Revision: 480549 $
 */
public class TestPropertyMessageResources extends TestCase {


    private static final String FOO_RESOURCES = "org.apache.struts.util.Foo";

    private Locale defaultLocale;
    
    // ----------------------------------------------------------------- Basics
    public TestPropertyMessageResources(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.awtui.TestRunner.main(new String[] {
                TestPropertyMessageResources.class.getName()
            });
    }

    public static Test suite() {
        return (new TestSuite(TestPropertyMessageResources.class));
    }

    // ----------------------------------------------------- Setup and Teardown
    public void setUp() {
        // cache the default locale
        defaultLocale = Locale.getDefault();
    }

    public void tearDown() {
        // restore the default locale
        Locale.setDefault(defaultLocale);
    }

    // ------------------------------------------------------- Individual Tests

    /**
     * Test Struts default PropertyMessageResources behaviour
     */
    public void testDefaultMode() {

        Locale.setDefault(Locale.US);
        
        // Create message resources - default Struts Behaviour
//        MessageResources resources = createMessageResources(FOO_RESOURCES, true, "DEFAULT");
        MessageResources resources = createMessageResources(FOO_RESOURCES, true, null);

        // Test language (& default) only keys
        assertEquals("key.lang FRANCE",  "LANG default", resources.getMessage(Locale.FRANCE,  "key.lang")); // no cached en_US
        assertEquals("key.lang English", "LANG en",      resources.getMessage(Locale.ENGLISH, "key.lang"));
        assertEquals("key.lang US",      "LANG en",      resources.getMessage(Locale.US,      "key.lang"));
        assertEquals("key.lang ITALY",   "LANG en",      resources.getMessage(Locale.ITALY,   "key.lang")); // cached en_US
        assertEquals("key.lang German",  "LANG de",      resources.getMessage(Locale.GERMAN,  "key.lang"));
        assertEquals("key.lang GERMANY", "LANG de",      resources.getMessage(Locale.GERMANY, "key.lang"));

        // Test country (& default) only keys
        assertEquals("key.country FRANCE",  "COUNTRY en_US", resources.getMessage(Locale.FRANCE,  "key.country"));
        assertEquals("key.country English", "COUNTRY en_US", resources.getMessage(Locale.ENGLISH, "key.country"));
        assertEquals("key.country US",      "COUNTRY en_US", resources.getMessage(Locale.US,      "key.country"));
        assertEquals("key.country ITALY",   "COUNTRY en_US", resources.getMessage(Locale.ITALY,   "key.country"));
        assertEquals("key.country German",  "COUNTRY en_US", resources.getMessage(Locale.GERMAN,  "key.country"));
        assertEquals("key.country GERMANY", "COUNTRY de_DE", resources.getMessage(Locale.GERMANY, "key.country"));

        // Test Unique Keys with wrong Locale
        assertEquals("Wrong Locale en only",    null,         resources.getMessage(Locale.GERMAN,  "key.en"));
        assertEquals("Wrong Locale en_US only", "en_US only", resources.getMessage(Locale.GERMANY, "key.en_US"));

        // Run tests with common expected results
        commonTests(resources);
    }

    /**
     * Test JSTL compatible PropertyMessageResources behaviour
     */
    public void testJstlMode() {

        Locale.setDefault(Locale.US);
        
        // Create message resources - default Struts Behaviour
        MessageResources resources = createMessageResources(FOO_RESOURCES, true, "JSTL");

        // Test language (& default) only keys
        assertEquals("key.lang FRANCE",  "LANG default", resources.getMessage(Locale.FRANCE,  "key.lang"));
        assertEquals("key.lang English", "LANG en",      resources.getMessage(Locale.ENGLISH, "key.lang"));
        assertEquals("key.lang US",      "LANG en",      resources.getMessage(Locale.US,      "key.lang"));
        assertEquals("key.lang ITALY",   "LANG default", resources.getMessage(Locale.ITALY,   "key.lang"));
        assertEquals("key.lang German",  "LANG de",      resources.getMessage(Locale.GERMAN,  "key.lang"));
        assertEquals("key.lang GERMANY", "LANG de",      resources.getMessage(Locale.GERMANY, "key.lang"));

        // Test country (& default) only keys
        assertEquals("key.country FRANCE",  "COUNTRY default", resources.getMessage(Locale.FRANCE,  "key.country"));
        assertEquals("key.country English", "COUNTRY default", resources.getMessage(Locale.ENGLISH, "key.country"));
        assertEquals("key.country US",      "COUNTRY en_US",   resources.getMessage(Locale.US,      "key.country"));
        assertEquals("key.country ITALY",   "COUNTRY default", resources.getMessage(Locale.ITALY,   "key.country"));
        assertEquals("key.country German",  "COUNTRY default", resources.getMessage(Locale.GERMAN,  "key.country"));
        assertEquals("key.country GERMANY", "COUNTRY de_DE",   resources.getMessage(Locale.GERMANY, "key.country"));

        // Test Unique Keys with wrong Locale
        assertEquals("Wrong Locale en only",    null, resources.getMessage(Locale.GERMAN,  "key.en"));
        assertEquals("Wrong Locale en_US only", null, resources.getMessage(Locale.GERMANY, "key.en_US"));

        // Run tests with common expected results
        commonTests(resources);

    }

    /**
     * Test "PropertyResourceBundle" compatible PropertyMessageResources behaviour
     */
    public void testResourceBundleMode() {

        Locale.setDefault(Locale.US);
        
        // Create message resources - default Struts Behaviour
        MessageResources resources = createMessageResources(FOO_RESOURCES, true, "RESOURCE");

        // Test language (& default) only keys
        assertEquals("key.lang FRANCE",  "LANG en",      resources.getMessage(Locale.FRANCE,  "key.lang"));
        assertEquals("key.lang English", "LANG en",      resources.getMessage(Locale.ENGLISH, "key.lang"));
        assertEquals("key.lang US",      "LANG en",      resources.getMessage(Locale.US,      "key.lang"));
        assertEquals("key.lang ITALY",   "LANG en",      resources.getMessage(Locale.ITALY,   "key.lang"));
        assertEquals("key.lang German",  "LANG de",      resources.getMessage(Locale.GERMAN,  "key.lang"));
        assertEquals("key.lang GERMANY", "LANG de",      resources.getMessage(Locale.GERMANY, "key.lang"));

        // Test country (& default) only keys
        assertEquals("key.country FRANCE",  "COUNTRY en_US", resources.getMessage(Locale.FRANCE,  "key.country"));
        assertEquals("key.country English", "COUNTRY en_US", resources.getMessage(Locale.ENGLISH, "key.country"));
        assertEquals("key.country US",      "COUNTRY en_US", resources.getMessage(Locale.US,      "key.country"));
        assertEquals("key.country ITALY",   "COUNTRY en_US", resources.getMessage(Locale.ITALY,   "key.country"));
        assertEquals("key.country German",  "COUNTRY en_US", resources.getMessage(Locale.GERMAN,  "key.country"));
        assertEquals("key.country GERMANY", "COUNTRY de_DE", resources.getMessage(Locale.GERMANY, "key.country"));

        // Test Unique Keys with wrong Locale
        assertEquals("Wrong Locale en only",    "en only",    resources.getMessage(Locale.GERMAN,  "key.en"));
        assertEquals("Wrong Locale en_US only", "en_US only", resources.getMessage(Locale.GERMANY, "key.en_US"));

        // Run tests with common expected results
        commonTests(resources);
    }

    /**
     * Tests with common expected results
     */
    public void commonTests(MessageResources resources) {

        // Test "null" Locale
        assertEquals("null Locale",  "ALL default", resources.getMessage((Locale)null,  "key.all"));

        // Test Default only key with all Locales
        assertEquals("Check default en",    "default only", resources.getMessage(Locale.ENGLISH, "key.default"));
        assertEquals("Check default en_US", "default only", resources.getMessage(Locale.US,      "key.default"));
        assertEquals("Check default de",    "default only", resources.getMessage(Locale.GERMAN,  "key.default"));
        assertEquals("Check default de_DE", "default only", resources.getMessage(Locale.GERMANY, "key.default"));

        // Test key in all locales
        assertEquals("Check ALL en",        "ALL en",       resources.getMessage(Locale.ENGLISH, "key.all"));
        assertEquals("Check ALL en_US",     "ALL en_US",    resources.getMessage(Locale.US,      "key.all"));
        assertEquals("Check ALL de",        "ALL de",       resources.getMessage(Locale.GERMAN,  "key.all"));
        assertEquals("Check ALL de_DE",     "ALL de_DE",    resources.getMessage(Locale.GERMANY, "key.all"));

        // Test key unique to each locale
        assertEquals("Check en only",       "en only",      resources.getMessage(Locale.ENGLISH, "key.en"));
        assertEquals("Check en_US only",    "en_US only",   resources.getMessage(Locale.US,      "key.en_US"));
        assertEquals("Check de only",       "de only",      resources.getMessage(Locale.GERMAN,  "key.de"));
        assertEquals("Check de_DE only",    "de_DE only",   resources.getMessage(Locale.GERMANY, "key.de_DE"));

        // Test unique keys with incorrect Locale
        assertEquals("Missing default",     null,           resources.getMessage(Locale.ENGLISH, "missing"));
        assertEquals("Missing de only",     null,           resources.getMessage(Locale.US,      "key.de"));
        assertEquals("Missing de_DE only",  null,           resources.getMessage(Locale.US,      "key.de_DE"));
    }

    /**
     * Create the PropertyMessageResources.
     */
    private MessageResources createMessageResources(String file, boolean returnNull, String mode) {
        MessageResourcesConfig config = new MessageResourcesConfig();
        config.setNull(returnNull);
        if (mode != null) {
            config.setProperty("mode", mode);
        }
        PropertyMessageResourcesFactory factory = new PropertyMessageResourcesFactory();
        factory.setConfig(config);
        factory.setReturnNull(returnNull);
        return factory.createResources(file);
    }
}
