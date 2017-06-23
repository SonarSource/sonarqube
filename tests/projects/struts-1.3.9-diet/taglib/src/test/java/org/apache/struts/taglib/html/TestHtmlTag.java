/*
 * $Id: TestHtmlTag.java 482895 2006-12-06 05:12:27Z niallp $
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

import java.util.Locale;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.struts.mock.MockHttpServletRequest;
import org.apache.struts.mock.MockHttpServletResponse;
import org.apache.struts.mock.MockPageContext;
import org.apache.struts.mock.MockServletConfig;

/**
 * Unit tests for the HtmlTag.
 */
public class TestHtmlTag extends TestCase {

    private MockServletConfig       config;
    private MockHttpServletRequest  request;
    private MockHttpServletResponse response;
    private MockPageContext         pageContext;
    private HtmlTag                 htmlTag;

    /**
     * Defines the testcase name for JUnit.
     *
     * @param theName the testcase's name.
     */
    public TestHtmlTag(String theName) {
        super(theName);
    }

    /**
     * Start the tests.
     *
     * @param theArgs the arguments. Not used
     */
    public static void main(String[] theArgs) {
        junit.awtui.TestRunner.main(new String[] { TestHtmlTag.class.getName() });
    }

    /**
     * @return a test suite (<code>TestSuite</code>) that includes all methods
     *         starting with "test"
     */
    public static Test suite() {
        // All methods starting with "test" will be executed in the test suite.
        return new TestSuite(TestHtmlTag.class);
    }

    /**
     * Set up mock objects.
     */
    public void setUp() {
        config      = new MockServletConfig();
        request     = new MockHttpServletRequest();
        response    = new MockHttpServletResponse();
        pageContext = new MockPageContext(config, request, response);
        htmlTag     = new HtmlTag();
        htmlTag.setPageContext(pageContext);
    }

    /**
     * Test the "lang" attribute with valid characters.
     */
    public void testValidLangTrue() {
        
        // switch to render "lang" attribute
        htmlTag.setLang(true);

        // Render for Locale.US
        request.setLocale(Locale.US);
        assertEquals("render en_US", "<html lang=\"en-US\">", htmlTag.renderHtmlStartElement());

        // Render for Locale.ENGLISH
        request.setLocale(Locale.ENGLISH);
        assertEquals("render en", "<html lang=\"en\">", htmlTag.renderHtmlStartElement());

        // Test valid characters
        request.setLocale(new Locale("abcd-efghijklmnopqrstuvwxyz", "ABCDEFGHIJKLM-NOPQRSTUVWXYZ", ""));
        assertEquals("valid characters", "<html lang=\"abcd-efghijklmnopqrstuvwxyz-ABCDEFGHIJKLM-NOPQRSTUVWXYZ\">", htmlTag.renderHtmlStartElement());

    }

    /**
     * Test the "lang" attribute with valid characters.
     */
    public void testValidLangFalse() {
        
        // switch to NOT render "lang" attribute
        htmlTag.setLang(false);

        // Ignore for Locale.US
        request.setLocale(Locale.US);
        assertEquals("ignore en_US", "<html>", htmlTag.renderHtmlStartElement());

        // Ignore for Locale.ENGLISH
        request.setLocale(Locale.ENGLISH);
        assertEquals("ignore en", "<html>", htmlTag.renderHtmlStartElement());

    }

    /**
     * Test an invalid "language"
     */
    public void testInvalidLanguage() {
        
        // switch to render "lang" attribute
        htmlTag.setLang(true);

        // make sure HtmlTag is setup to render "lang" using a valid value
        request.setLocale(Locale.US);
        assertEquals("check valid", "<html lang=\"en-US\">", htmlTag.renderHtmlStartElement());

        // Test script injection
        request.setLocale(new Locale("/><script>alert()</script>", "", ""));
        assertEquals("invalid <script>", "<html>", htmlTag.renderHtmlStartElement());

        // Test <
        request.setLocale(new Locale("abc<def", "", ""));
        assertEquals("invalid LT", "<html>", htmlTag.renderHtmlStartElement());

        // Test >
        request.setLocale(new Locale("abc>def", "", ""));
        assertEquals("invalid GT", "<html>", htmlTag.renderHtmlStartElement());

        // Test /
        request.setLocale(new Locale("abc/def", "", ""));
        assertEquals("invalid SLASH", "<html>", htmlTag.renderHtmlStartElement());

        // Test &
        request.setLocale(new Locale("abc&def", "", ""));
        assertEquals("invalid AMP", "<html>", htmlTag.renderHtmlStartElement());

    }

    /**
     * Test an invalid "country"
     */
    public void testInvalidCountry() {
        
        // switch to render "lang" attribute
        htmlTag.setLang(true);

        // make sure HtmlTag is setup to render "lang" using a valid value
        request.setLocale(Locale.US);
        assertEquals("check valid", "<html lang=\"en-US\">", htmlTag.renderHtmlStartElement());

        // Test script injection
        request.setLocale(new Locale("en", "/><script>alert()</script>", ""));
        assertEquals("invalid <script>", "<html lang=\"en\">", htmlTag.renderHtmlStartElement());

        // Test <
        request.setLocale(new Locale("en", "abc<def", ""));
        assertEquals("invalid LT", "<html lang=\"en\">", htmlTag.renderHtmlStartElement());

        // Test >
        request.setLocale(new Locale("en", "abc>def", ""));
        assertEquals("invalid GT", "<html lang=\"en\">", htmlTag.renderHtmlStartElement());

        // Test /
        request.setLocale(new Locale("en", "abc/def", ""));
        assertEquals("invalid SLASH", "<html lang=\"en\">", htmlTag.renderHtmlStartElement());

        // Test &
        request.setLocale(new Locale("en", "abc&def", ""));
        assertEquals("invalid AMP", "<html lang=\"en\">", htmlTag.renderHtmlStartElement());

    }
}
