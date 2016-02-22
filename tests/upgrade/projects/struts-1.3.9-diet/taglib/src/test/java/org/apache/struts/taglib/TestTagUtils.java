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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.impl.ModuleConfigImpl;
import org.apache.struts.mock.MockFormBean;
import org.apache.struts.mock.MockHttpServletRequest;
import org.apache.struts.mock.MockHttpServletResponse;
import org.apache.struts.mock.MockPageContext;
import org.apache.struts.mock.MockServletConfig;
import org.apache.struts.mock.MockServletContext;
import org.apache.struts.taglib.html.Constants;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import java.net.MalformedURLException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Unit tests for the TagUtils.
 */
public class TestTagUtils extends TagTestBase {
    private static final Log log = LogFactory.getLog(TestTagUtils.class);

    /**
     * Defines the testcase name for JUnit.
     *
     * @param theName the testcase's name.
     */
    public TestTagUtils(String theName) {
        super(theName);
    }

    /**
     * Start the tests.
     *
     * @param theArgs the arguments. Not used
     */
    public static void main(String[] theArgs) {
        junit.awtui.TestRunner.main(new String[] { TestTagUtils.class.getName() });
    }

    /**
     * @return a test suite (<code>TestSuite</code>) that includes all methods
     *         starting with "test"
     */
    public static Test suite() {
        // All methods starting with "test" will be executed in the test suite.
        return new TestSuite(TestTagUtils.class);
    }

    /**
     * Test Operators.
     */
    public void testFilter() {
        assertNull("Null", null);

        // Test Null
        assertNull("Filter Test 1", tagutils.filter(null));

        // Test Empty String
        assertEquals("Filter Test 2", "", tagutils.filter(""));

        // Test Single Character
        assertEquals("Filter Test 3", "a", tagutils.filter("a"));

        // Test Multiple Characters
        assertEquals("Filter Test 4", "adhdfhdfhadhf",
            tagutils.filter("adhdfhdfhadhf"));

        // Test Each filtered Character
        assertEquals("Filter Test 5", "&lt;", tagutils.filter("<"));
        assertEquals("Filter Test 6", "&gt;", tagutils.filter(">"));
        assertEquals("Filter Test 7", "&amp;", tagutils.filter("&"));
        assertEquals("Filter Test 8", "&quot;", tagutils.filter("\""));
        assertEquals("Filter Test 9", "&#39;", tagutils.filter("'"));

        // Test filtering beginning, middle, end
        assertEquals("Filter Test 10", "a&lt;", tagutils.filter("a<"));
        assertEquals("Filter Test 11", "&lt;a", tagutils.filter("<a"));
        assertEquals("Filter Test 12", "a&lt;a", tagutils.filter("a<a"));

        // Test filtering beginning, middle, end
        assertEquals("Filter Test 13", "abc&lt;", tagutils.filter("abc<"));
        assertEquals("Filter Test 14", "&lt;abc", tagutils.filter("<abc"));
        assertEquals("Filter Test 15", "abc&lt;abc", tagutils.filter("abc<abc"));

        // Test Multiple Characters
        assertEquals("Filter Test 16",
            "&lt;input type=&quot;text&quot; value=&#39;Me &amp; You&#39;&gt;",
            tagutils.filter("<input type=\"text\" value='Me & You'>"));
    }

    // ---------------------------------------------------- computeParameters()
    // No parameters and no transaction token
    public void testComputeParameters0a() {
        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, null, null, null,
                    null, null, null, null, false);
        } catch (JspException e) {
            fail("JspException: " + e);
        }

        assertNull("Map is null", map);
    }

    // No parameters but add transaction token
    public void testComputeParameters0b() {
        request.getSession().setAttribute(Globals.TRANSACTION_TOKEN_KEY, "token");

        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, null, null, null,
                    null, null, null, null, true);
        } catch (JspException e) {
            fail("JspException: " + e);
        }

        assertNotNull("Map is not null", map);
        assertEquals("One parameter in the returned map", 1, map.size());
        assertTrue("Transaction token parameter present",
            map.containsKey(Constants.TOKEN_KEY));
        assertEquals("Transaction token parameter value", "token",
            (String) map.get(Constants.TOKEN_KEY));
    }

    // invalid scope name is requested
    public void testComputeParametersInvalidScope() {
        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, null, null, null,
                    "session", "i-do-not-exist", null, null, false);

            fail("JspException not thrown");
        } catch (JspException e) {
            assertNull("map is null", map);
        }
    }

    // specified bean is not found
    public void testComputeParametersBeanNotFound() {
        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, null, null, null,
                    null, "i-do-not-exist", null, null, false);

            fail("JspException not thrown");
        } catch (JspException e) {
            assertNull("map is null", map);
        }
    }

    // accessing this property causes an exception
    public void testComputeParametersPropertyThrowsException() {
        request.getSession().setAttribute("SomeBean", new MockFormBean(true));

        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, null, null, null,
                    null, "SomeBean", "justThrowAnException", null, false);
            fail("JspException not thrown");
        } catch (JspException e) {
            assertNull("map is null", map);
        }
    }

    public void testComputeParametersParamIdParamPropThrowException() {
        request.getSession().setAttribute("SomeBean", new MockFormBean(true));

        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, "paramId",
                    "SomeBean", "justThrowAnException", null, null, null, null,
                    false);

            fail("JspException not thrown");
        } catch (JspException e) {
            assertNull("map is null", map);
        }
    }

    public void testComputeParametersParamValueToString() {
        request.getSession().setAttribute("SomeBean",
            new MockFormBean(false, false, new Double(1)));

        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, "paramId",
                    "SomeBean", "doubleValue", null, null, null, null, false);

            assertNotNull("map is null", map);

            String val = (String) map.get("paramId");

            assertTrue("paramId should be 1.0", "1.0".equals(val));
        } catch (JspException e) {
            fail("JspException not thrown");
        }
    }

    public void skiptestComputeParametersParamIdAsStringArray() {
        Map params = new HashMap();

        //        String[] vals = new String[]{"test0, test1"};
        params.put("fooParamId", "fooParamValue");

        request.getSession().setAttribute("SomeBean", params);

        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, "fooParamId",
                    "SomeBean", null, null, "SomeBean", null, null, false);

            //            map = tagutils.computeParameters(
            //                    page, "paramId", "SomeBean", "stringArray", null,
            //                    null, null, null, false);
            assertNotNull("map is null", map);

            String val = (String) map.get("key0");

            assertTrue("paramId should be \"test\"", "1.0".equals(val));
        } catch (JspException e) {
            fail("JspException not thrown");
        }
    }

    // Single parameter -- name
    public void testComputeParameters1a() {
        request.getSession().setAttribute("attr", "bar");

        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, "foo", "attr", null,
                    null, null, null, null, false);
        } catch (JspException e) {
            fail("JspException: " + e);
        }

        assertNotNull("Map is not null", map);
        assertEquals("One parameter in the returned map", 1, map.size());
        assertTrue("Parameter present", map.containsKey("foo"));
        assertEquals("Parameter value", "bar", (String) map.get("foo"));
    }

    // Single parameter -- scope + name
    public void testComputeParameters1b() {
        request.setAttribute("attr", "bar");

        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, "foo", "attr", null,
                    "request", null, null, null, false);
        } catch (JspException e) {
            fail("JspException: " + e);
        }

        assertNotNull("Map is not null", map);
        assertEquals("One parameter in the returned map", 1, map.size());
        assertTrue("Parameter present", map.containsKey("foo"));
        assertEquals("Parameter value", "bar", (String) map.get("foo"));
    }

    // Single parameter -- scope + name + property
    public void testComputeParameters1c() {
        request.setAttribute("attr", new MockFormBean("bar"));

        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, "foo", "attr",
                    "stringProperty", "request", null, null, null, false);
        } catch (JspException e) {
            fail("JspException: " + e);
        }

        assertNotNull("Map is not null", map);
        assertEquals("One parameter in the returned map", 1, map.size());
        assertTrue("Parameter present", map.containsKey("foo"));
        assertEquals("Parameter value", "bar", (String) map.get("foo"));
    }

    // Provided map -- name
    public void testComputeParameters2a() {
        Map map = new HashMap();

        map.put("foo1", "bar1");
        map.put("foo2", "bar2");
        request.getSession().setAttribute("attr", map);

        try {
            map = tagutils.computeParameters(pageContext, null, null, null,
                    null, "attr", null, null, false);
        } catch (JspException e) {
            fail("JspException: " + e);
        }

        assertNotNull("Map is not null", map);
        assertEquals("Two parameter in the returned map", 2, map.size());
        assertTrue("Parameter foo1 present", map.containsKey("foo1"));
        assertEquals("Parameter foo1 value", "bar1", (String) map.get("foo1"));
        assertTrue("Parameter foo2 present", map.containsKey("foo2"));
        assertEquals("Parameter foo2 value", "bar2", (String) map.get("foo2"));
    }

    // Provided map -- scope + name
    public void testComputeParameters2b() {
        Map map = new HashMap();

        map.put("foo1", "bar1");
        map.put("foo2", "bar2");
        request.setAttribute("attr", map);

        try {
            map = tagutils.computeParameters(pageContext, null, null, null,
                    null, "attr", null, "request", false);
        } catch (JspException e) {
            fail("JspException: " + e);
        }

        assertNotNull("Map is not null", map);
        assertEquals("Two parameter in the returned map", 2, map.size());
        assertTrue("Parameter foo1 present", map.containsKey("foo1"));
        assertEquals("Parameter foo1 value", "bar1", (String) map.get("foo1"));
        assertTrue("Parameter foo2 present", map.containsKey("foo2"));
        assertEquals("Parameter foo2 value", "bar2", (String) map.get("foo2"));
    }

    // Provided map -- scope + name + property
    public void testComputeParameters2c() {
        request.setAttribute("attr", new MockFormBean());

        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, null, null, null,
                    null, "attr", "mapProperty", "request", false);
        } catch (JspException e) {
            fail("JspException: " + e);
        }

        assertNotNull("Map is not null", map);
        assertEquals("Two parameter in the returned map", 2, map.size());
        assertTrue("Parameter foo1 present", map.containsKey("foo1"));
        assertEquals("Parameter foo1 value", "bar1", (String) map.get("foo1"));
        assertTrue("Parameter foo2 present", map.containsKey("foo2"));
        assertEquals("Parameter foo2 value", "bar2", (String) map.get("foo2"));
    }

    // Provided map -- name with one key and two values
    public void testComputeParameters2d() {
        Map map = new HashMap();

        map.put("foo", new String[] { "bar1", "bar2" });
        request.getSession().setAttribute("attr", map);

        try {
            map = tagutils.computeParameters(pageContext, null, null, null,
                    null, "attr", null, null, false);
        } catch (JspException e) {
            fail("JspException: " + e);
        }

        assertNotNull("Map is not null", map);
        assertEquals("One parameter in the returned map", 1, map.size());
        assertTrue("Parameter foo present", map.containsKey("foo"));
        assertTrue("Parameter foo value type",
            map.get("foo") instanceof String[]);

        String[] values = (String[]) map.get("foo");

        assertEquals("Values count", 2, values.length);
    }

    // Kitchen sink combination of parameters with a merge
    public void testComputeParameters3a() {
        request.setAttribute("attr", new MockFormBean("bar3"));
        request.getSession().setAttribute(Globals.TRANSACTION_TOKEN_KEY, "token");

        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, "foo1", "attr",
                    "stringProperty", "request", "attr", "mapProperty",
                    "request", true);
        } catch (JspException e) {
            fail("JspException: " + e);
        }

        assertNotNull("Map is not null", map);
        assertEquals("Three parameter in the returned map", 3, map.size());

        assertTrue("Parameter foo1 present", map.containsKey("foo1"));
        assertTrue("Parameter foo1 value type",
            map.get("foo1") instanceof String[]);

        String[] values = (String[]) map.get("foo1");

        assertEquals("Values count", 2, values.length);

        assertTrue("Parameter foo2 present", map.containsKey("foo2"));
        assertEquals("Parameter foo2 value", "bar2", (String) map.get("foo2"));

        assertTrue("Transaction token parameter present",
            map.containsKey(Constants.TOKEN_KEY));
        assertEquals("Transaction token parameter value", "token",
            (String) map.get(Constants.TOKEN_KEY));
    }

    // Kitchen sink combination of parameters with a merge
    // with array values in map
    public void testComputeParameters3aa() {
        request.setAttribute("attr", new MockFormBean("bar3"));
        request.getSession().setAttribute(Globals.TRANSACTION_TOKEN_KEY, "token");

        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, "foo1", "attr",
                    "stringProperty", "request", "attr",
                    "mapPropertyArrayValues", "request", true);
        } catch (JspException e) {
            fail("JspException: " + e);
        }

        assertNotNull("Map is not null", map);
        assertEquals("Three parameter in the returned map", 3, map.size());

        assertTrue("Parameter foo1 present", map.containsKey("foo1"));
        assertTrue("Parameter foo1 value type",
            map.get("foo1") instanceof String[]);

        String[] values = (String[]) map.get("foo1");

        assertEquals("Values count", 3, values.length);

        assertTrue("Parameter foo2 present", map.containsKey("foo2"));

        String[] arrayValues = (String[]) map.get("foo2");
        String val = arrayValues[0];

        assertEquals("Parameter foo2 value", "bar2", val);

        assertTrue("Transaction token parameter present",
            map.containsKey(Constants.TOKEN_KEY));
        assertEquals("Transaction token parameter value", "token",
            (String) map.get(Constants.TOKEN_KEY));
    }

    // Kitchen sink combination of parameters with a merge
    public void testComputeParameters3b() {
        request.setAttribute("attr", new MockFormBean("bar3"));
        request.getSession().setAttribute(Globals.TRANSACTION_TOKEN_KEY, "token");

        Map map = null;

        try {
            map = tagutils.computeParameters(pageContext, "foo1", "attr",
                    "stringProperty", "request", "attr", "mapProperty",
                    "request", true);
        } catch (JspException e) {
            fail("JspException: " + e);
        }

        assertNotNull("Map is not null", map);
        assertEquals("Three parameter in the returned map", 3, map.size());

        assertTrue("Parameter foo1 present", map.containsKey("foo1"));
        assertTrue("Parameter foo1 value type",
            map.get("foo1") instanceof String[]);

        String[] values = (String[]) map.get("foo1");

        assertEquals("Values count", 2, values.length);

        assertTrue("Parameter foo2 present", map.containsKey("foo2"));
        assertEquals("Parameter foo2 value", "bar2", (String) map.get("foo2"));

        assertTrue("Transaction token parameter present",
            map.containsKey(Constants.TOKEN_KEY));
        assertEquals("Transaction token parameter value", "token",
            (String) map.get(Constants.TOKEN_KEY));
    }

    // ----------------------------------------------------------- computeURL()
    // Default module -- Forward only
    public void testComputeURL1a() {
        request.setPathElements("/myapp", "/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, "foo", null, null, null,
                    null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "/myapp/bar.jsp", url);
    }

    // Default module -- Href only
    public void testComputeURL1b() {
        request.setPathElements("/myapp", "/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, "http://foo.com/bar",
                    null, null, null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "http://foo.com/bar", url);
    }

    // Default module -- Page only
    public void testComputeURL1c() {
        request.setPathElements("/myapp", "/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, "/bar", null,
                    null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "/myapp/bar", url);
    }

    // Default module -- Forward with pattern
    public void testComputeURL1d() {
        moduleConfig.getControllerConfig().setForwardPattern("$C/WEB-INF/pages$M$P");
        request.setPathElements("/myapp", "/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, "foo", null, null, null,
                    null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "/myapp/WEB-INF/pages/bar.jsp", url);
    }

    // Default module -- Page with pattern
    public void testComputeURL1e() {
        moduleConfig.getControllerConfig().setPagePattern("$C/WEB-INF/pages$M$P");
        request.setPathElements("/myapp", "/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, "/bar", null,
                    null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "/myapp/WEB-INF/pages/bar", url);
    }

    // Default module -- Forward with relative path (non-context-relative)
    public void testComputeURL1f() {
        request.setPathElements("/myapp", "/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, "relative1", null, null,
                    null, null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value",
        //                     "/myapp/relative.jsp",
        "relative.jsp", url);
    }

    // Default module -- Forward with relative path (context-relative)
    public void testComputeURL1g() {
        request.setPathElements("/myapp", "/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, "relative2", null, null,
                    null, null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value",
        //                     "/myapp/relative.jsp",
        "relative.jsp", url);
    }

    // Default module -- Forward with external path
    public void testComputeURL1h() {
        request.setPathElements("/myapp", "/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, "external", null, null,
                    null, null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "http://struts.apache.org/", url);
    }

    // Second module -- Forward only
    public void testComputeURL2a() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig2);
        request.setPathElements("/myapp", "/2/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, "foo", null, null, null,
                    null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "/myapp/2/baz.jsp", url);
    }

    // Second module -- Href only
    public void testComputeURL2b() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig2);
        request.setPathElements("/myapp", "/2/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, "http://foo.com/bar",
                    null, null, null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "http://foo.com/bar", url);
    }

    // Second module -- Page only
    public void testComputeURL2c() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig2);
        request.setPathElements("/myapp", "/2/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, "/bar", null,
                    null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "/myapp/2/bar", url);
    }

    // Default module -- Forward with pattern
    public void testComputeURL2d() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig2);
        moduleConfig2.getControllerConfig().setForwardPattern("$C/WEB-INF/pages$M$P");
        request.setPathElements("/myapp", "/2/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, "foo", null, null, null,
                    null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "/myapp/WEB-INF/pages/2/baz.jsp", url);
    }

    // Second module -- Page with pattern
    public void testComputeURL2e() {
        moduleConfig2.getControllerConfig().setPagePattern("$C/WEB-INF/pages$M$P");
        request.setAttribute(Globals.MODULE_KEY, moduleConfig2);
        request.setPathElements("/myapp", "/2/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, "/bar", null,
                    null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "/myapp/WEB-INF/pages/2/bar", url);
    }

    // Second module -- Forward with relative path (non-context-relative)
    public void testComputeURL2f() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig2);
        request.setPathElements("/myapp", "/2/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, "relative1", null, null,
                    null, null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value",
        //                     "/myapp/2/relative.jsp",
        "relative.jsp", url);
    }

    // Second module -- Forward with relative path (context-relative)
    public void testComputeURL2g() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig2);
        request.setPathElements("/myapp", "/2/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, "relative2", null, null,
                    null, null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value",
        //                     "/myapp/relative.jsp",
        "relative.jsp", url);
    }

    // Second module -- Forward with external path
    public void testComputeURL2h() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig2);
        request.setPathElements("/myapp", "/2/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, "external", null, null,
                    null, null, null, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "http://struts.apache.org/", url);
    }

    // Add parameters only -- forward URL
    public void testComputeURL3a() {
        request.setPathElements("/myapp", "/action.do", null, null);

        Map map = new HashMap();

        map.put("foo1", "bar1");
        map.put("foo2", "bar2");

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, "/bar", null,
                    null, map, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertTrue("url value",
            url.equals("/myapp/bar?foo1=bar1&amp;foo2=bar2")
            || url.equals("/myapp/bar?foo2=bar2&amp;foo1=bar1"));
    }

    // Add anchor only -- forward URL
    public void testComputeURL3b() {
        request.setPathElements("/myapp", "/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, "/bar", null,
                    null, null, "anchor", false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "/myapp/bar#anchor", url);
    }

    // Add parameters + anchor -- forward URL
    public void testComputeURL3c() {
        request.setPathElements("/myapp", "/action.do", null, null);

        Map map = new HashMap();

        map.put("foo1", "bar1");
        map.put("foo2", "bar2");

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, "/bar", null,
                    null, map, "anchor", false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertTrue("url value",
            url.equals("/myapp/bar?foo1=bar1&amp;foo2=bar2#anchor")
            || url.equals("/myapp/bar?foo2=bar2&amp;foo1=bar1#anchor"));
    }

    // Add parameters only -- redirect URL
    public void testComputeURL3d() {
        request.setPathElements("/myapp", "/action.do", null, null);

        Map map = new HashMap();

        map.put("foo1", "bar1");
        map.put("foo2", "bar2");

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, "/bar", null,
                    null, map, null, true);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertTrue("url value",
            url.equals("/myapp/bar?foo1=bar1&foo2=bar2")
            || url.equals("/myapp/bar?foo2=bar2&foo1=bar1"));
    }

    // Add anchor only -- redirect URL
    public void testComputeURL3e() {
        request.setPathElements("/myapp", "/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, "/bar", null,
                    null, null, "anchor", true);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertEquals("url value", "/myapp/bar#anchor", url);
    }

    // Add parameters + anchor -- redirect URL
    public void testComputeURL3f() {
        request.setPathElements("/myapp", "/action.do", null, null);

        Map map = new HashMap();

        map.put("foo1", "bar1");
        map.put("foo2", "bar2");

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, "/bar", null,
                    null, map, "anchor", false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertTrue("url value",
            url.equals("/myapp/bar?foo1=bar1&amp;foo2=bar2#anchor")
            || url.equals("/myapp/bar?foo2=bar2&amp;foo1=bar1#anchor"));
    }

    // Add parameters only -- forward URL -- do not encode seperator
    public void testComputeURL3g() {
        request.setPathElements("/myapp", "/action.do", null, null);

        Map map = new HashMap();

        map.put("foo1", "bar1");
        map.put("foo2", "bar2");

        String url = null;

        try {
            url = tagutils.computeURLWithCharEncoding(pageContext, null, null,
                    "/bar", null, null, map, null, false, false, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertTrue("url value",
            url.equals("/myapp/bar?foo1=bar1&foo2=bar2")
            || url.equals("/myapp/bar?foo2=bar2&foo1=bar1"));
    }

    // Add parameters only
    //  -- forward URL
    //  -- do not encode seperator
    //  -- send param with null value
    public void testComputeURL3h() {
        request.setPathElements("/myapp", "/action.do", null, null);

        Map map = new HashMap();

        map.put("foo1", null);

        String url = null;

        try {
            url = tagutils.computeURLWithCharEncoding(pageContext, null, null,
                    "/bar", null, null, map, null, false, false, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertTrue("url value", url.equals("/myapp/bar?foo1="));
    }

    // Add parameters only
    //  -- forward URL
    //  -- do not encode seperator
    //  -- send param with null value
    //  -- add ? to page
    public void testComputeURL3i() {
        request.setPathElements("/myapp", "/action.do", null, null);

        Map map = new HashMap();

        map.put("foo1", null);

        String url = null;

        try {
            url = tagutils.computeURLWithCharEncoding(pageContext, null, null,
                    "/bar?", null, null, map, null, false, false, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertTrue("url value", url.equals("/myapp/bar?&foo1="));
    }

    // Add parameters only
    //  -- forward URL
    //  -- do not encode seperator
    //  -- send param with null value
    //  -- add ? and param to page
    public void testComputeURL3j() {
        request.setPathElements("/myapp", "/action.do", null, null);

        Map map = new HashMap();

        map.put("foo1", null);
        map.put("foo2", "bar2");

        String url = null;

        try {
            url = tagutils.computeURLWithCharEncoding(pageContext, null, null,
                    "/bar?a=b", null, null, map, null, false, false, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertTrue("url value",
            url.equals("/myapp/bar?a=b&foo1=&foo2=bar2")
            || url.equals("/myapp/bar?a=b&foo2=bar2&foo1="));
    }

    // -- Add Parameters
    // -- Parameter as String Array
    public void testComputeURL3k() {
        request.setPathElements("/myapp", "/action.do", null, null);

        Map map = new HashMap();

        map.put("foo1", new String[] { "bar1", "baz1" });

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, "/bar", null,
                    null, map, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertTrue("url value",
            url.equals("/myapp/bar?foo1=bar1&amp;foo1=baz1")
            || url.equals("/myapp/bar?foo1=baz1&amp;foo1=bar1"));
    }

    // -- Add Parameters
    // -- Parameter as non String or String Array
    public void testComputeURL3l() {
        request.setPathElements("/myapp", "/action.do", null, null);

        Map map = new HashMap();

        map.put("foo1", new Double(0));

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, "/bar", null,
                    null, map, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertTrue("url value", url.equals("/myapp/bar?foo1=0.0"));
    }

    // -- Add Parameters
    // -- Parameter as non String or String Array
    // -- with ? on path
    public void testComputeURL3m() {
        request.setPathElements("/myapp", "/action.do", null, null);

        Map map = new HashMap();

        map.put("foo1", new Double(0));

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, "/bar?", null,
                    null, map, null, false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertTrue("url value", url.equals("/myapp/bar?&amp;foo1=0.0"));
    }

    public void testComputeURLCharacterEncoding() {
        request.setPathElements("/myapp", "/action.do", null, null);

        String url = null;

        try {
            url = tagutils.computeURLWithCharEncoding(pageContext, "foo", null,
                    null, null, null, null, null, false, true, true);
            fail("Exception not thrown");
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        } catch (UnsupportedOperationException e) {
            assertNull("url should be null", url);
        }
    }

    public void testComputeURLCharacterEncodingMultipleSpecifier() {
        verifyBadSetOfSpecifiers("foo", "foo", null, null);
        verifyBadSetOfSpecifiers("foo", null, "foo", null);
        verifyBadSetOfSpecifiers("foo", null, null, "foo");

        verifyBadSetOfSpecifiers(null, "foo", "foo", null);
        verifyBadSetOfSpecifiers(null, "foo", null, "foo");

        verifyBadSetOfSpecifiers(null, null, "foo", "foo");
    }

    public void testComputeURLCharacterEncodingAction() {
        ActionConfig actionConfig = new ActionConfig();

        actionConfig.setName("baz");
        actionConfig.setPath("/baz");

        moduleConfig.addActionConfig(actionConfig);

        request.setPathElements("/myapp", "/foo.do", null, null);

        Map map = new HashMap();

        map.put("foo1", "bar1");
        map.put("foo2", "bar2");

        String url = null;

        try {
            url = tagutils.computeURL(pageContext, null, null, null, "baz",
                    null, map, "anchor", false);
        } catch (MalformedURLException e) {
            fail("MalformedURLException: " + e);
        }

        assertNotNull("url present", url);
        assertTrue("url value",
            url.equals("/myapp/baz?foo1=bar1&amp;foo2=bar2#anchor")
            || url.equals("/myapp/baz?foo2=bar2&amp;foo1=bar1#anchor"));
    }

    // -------------------------------------------------------------- pageURL()
    // Default module (default pagePattern)
    public void testPageURL1() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig);
        request.setPathElements("/myapp", "/action.do", null, null);

        String page = null;
        String result = null;

        // Straight substitution
        page = "/mypages/index.jsp";
        result = tagutils.pageURL(request, page, moduleConfig);
        assertNotNull("straight sub found", result);
        assertEquals("straight sub value", "/mypages/index.jsp", result);
    }

    // Second module (default pagePattern)
    public void testPageURL2() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig2);
        request.setPathElements("/myapp", "/2/action.do", null, null);

        String page = null;
        String result = null;

        // Straight substitution
        page = "/mypages/index.jsp";
        result = tagutils.pageURL(request, page, moduleConfig2);
        assertNotNull("straight sub found", result);
        assertEquals("straight sub value", "/2/mypages/index.jsp", result);
    }

    // Third module (custom pagePattern)
    // TODO finish me
    public void testPageURL3a() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig3);
        request.setPathElements("/myapp", "/3/action.do", null, null);

        //        String page = null;
        //        String result = null;
    }

    // Third module (custom pagePattern)
    public void testPageURL3b() {
        request.setAttribute(Globals.MODULE_KEY, moduleConfig3);
        request.setPathElements("/myapp", "/3/action.do", null, null);

        String page = null;
        String result = null;

        // Straight substitution
        page = "/mypages/index.jsp";
        result = tagutils.pageURL(request, page, moduleConfig3);
        assertNotNull("straight sub found", result);
        assertEquals("straight sub value", "/3/mypages/index.jsp", result);
    }

    /**
     * Helper method that verifies the supplied specifiers.
     *
     * @param forward    The forward specified
     * @param href       The href specified
     * @param pageString The pageString specified
     * @param action     The action specified
     */
    private void verifyBadSetOfSpecifiers(String forward, String href,
        String pageString, String action) {
        String url = null;

        try {
            url = tagutils.computeURLWithCharEncoding(pageContext, forward,
                    href, pageString, action, null, null, null, false, true,
                    false);
        } catch (MalformedURLException e) {
            assertNull("url should be null", url);
        } catch (UnsupportedOperationException e) {
            fail("MalformedURLException not thrown");
        }
    }

    // -------------------------------------------------------------- encodeURL()
    // Default module (default pagePattern)
    public void testencodeURL1() {
        String encodedURL = null;

        encodedURL = tagutils.encodeURL("foo-bar.baz");
        assertEquals("encode url", "foo-bar.baz", encodedURL);
    }

    // ------------------------------------------ getActionErrors()
    // ActionErrors
    public void testGetActionErrors1a() {
        ActionMessages actionErrors = new ActionMessages();

        actionErrors.add("prop", new ActionMessage("key.key"));
        request.setAttribute("errors", actionErrors);

        try {
            ActionMessages errors =
                tagutils.getActionMessages(pageContext, "errors");

            assertNotNull("errors should not be null", errors);
            assertNotNull("errors prop should not be null", errors.get("prop"));

            String val = null;
            int i = 0;

            for (Iterator iter = errors.get("prop"); iter.hasNext();) {
                ActionMessage error = (ActionMessage) iter.next();

                val = error.getKey();
                i++;
            }

            assertEquals("only 1 error", i, 1);
            assertEquals("errors prop should match", val, "key.key");
        } catch (JspException e) {
            fail(e.getMessage());
        }
    }

    // String
    public void testGetActionErrors1b() {
        request.setAttribute("foo", "bar");

        try {
            ActionMessages errors =
                tagutils.getActionMessages(pageContext, "foo");

            assertNotNull("errors should not be null", errors);
            assertNotNull("errors prop should not be null", errors.get("prop"));

            String key = null;
            int i = 0;

            for (Iterator iter = errors.get(ActionMessages.GLOBAL_MESSAGE);
                iter.hasNext();) {
                ActionMessage error = (ActionMessage) iter.next();

                key = error.getKey();

                Object[] values = error.getValues();

                assertNull(values);
                i++;
            }

            assertEquals("only 1 error", i, 1);
            assertEquals("key should match", key, "bar");
        } catch (JspException e) {
            fail(e.getMessage());
        }
    }

    // String Array
    public void testGetActionErrors1c() {
        String[] vals = new String[] { "bar", "baz" };

        request.setAttribute("foo", vals);

        try {
            ActionMessages errors =
                tagutils.getActionMessages(pageContext, "foo");

            assertNotNull("errors should not be null", errors);
            assertNotNull("errors prop should not be null", errors.get("prop"));

            String key = null;
            int i = 0;

            for (Iterator iter = errors.get(ActionMessages.GLOBAL_MESSAGE);
                iter.hasNext();) {
                ActionMessage error = (ActionMessage) iter.next();

                key = error.getKey();

                Object[] values = error.getValues();

                assertNull((values));
                assertEquals("1st key should match", key, vals[i]);
                i++;
            }

            assertEquals("only 1 error", i, 2);
        } catch (JspException e) {
            fail(e.getMessage());
        }
    }

    // String Array (thrown JspException)
    public void testGetActionErrors1d() {
        request.setAttribute("foo", new MockFormBean());

        ActionMessages errors = null;

        try {
            errors = tagutils.getActionMessages(pageContext, "foo");
            fail("should have thrown JspException");
        } catch (JspException e) {
            assertNull("errors should be null", errors);
        }
    }

    // ActionErrors (thrown Exception)
    // TODO -- currently this does not hit the line for caught Exception
    public void testGetActionErrors1e() {
        ActionMessages actionErrors = new ActionMessages();

        actionErrors.add("prop", new ActionMessage("key.key"));
        request.setAttribute("errors", actionErrors);

        try {
            ActionMessages errors =
                tagutils.getActionMessages(pageContext, "does-not-exist");

            assertNotNull("errors should not be null", errors);
            assertNotNull("errors prop should not be null", errors.get("prop"));

            for (Iterator iter = errors.get("prop"); iter.hasNext();) {
                fail("Should not have any errors for does-not-exist");
            }
        } catch (JspException e) {
            fail(e.getMessage());
        }
    }

    // ------------------------------------------ getActionMappingName()
    public void testGetActionMappingName1() {
        String[] paths =
            {
                "foo", "foo.do", "foo?foo=bar", "foo?foo=bar&bar=baz",
                "foo?foo=bar&amp;bar=baz"
            };

        String[][] prepends =
            {
                { "", "/foo" },
                { "/", "/foo" },
                { "bar/", "/bar/foo" },
                { "/bar/", "/bar/foo" }
            };

        String[] appends =
            {
                "", "#anchor", "?", "?#", "?foo=bar", "?foo1=bar1&foo2=bar2",
                "?foo1=bar1&amp;foo2=bar2"
            };

        String finalResult = null;

        String path = null;
        String results = null;
        boolean equality = false;
        int ct = 0;

        for (int i = 0; i < appends.length; i++) {
            for (int j = 0; j < prepends.length; j++) {
                finalResult = prepends[j][1];

                for (int k = 0; k < paths.length; k++) {
                    path = prepends[j][0] + paths[k] + appends[i];
                    results = tagutils.getActionMappingName(path);
                    equality = finalResult.equals(results);

                    if (!equality) {
                        fail("Path does not return correct result\n"
                            + "\nexpected: " + results + "\nfound: " + path);
                    }

                    assertTrue("Path should translate to result", equality);
                    ct++;
                }
            }
        }

        log.debug(ct + " assertions run in this test");
    }

    public void testString_getActionMappingURL_String_PageContext() {
        ActionConfig actionConfig = new ActionConfig();

        actionConfig.setParameter("/foo");
        moduleConfig.addActionConfig(actionConfig);

        request.setAttribute(Globals.MODULE_KEY, moduleConfig);
        request.setPathElements("/myapp", "/foo.do", null, null);

        assertEquals("Check path /foo",
            tagutils.getActionMappingURL("/foo", pageContext), "/myapp/foo");
    }

    // use servlet mapping (extension mapping)
    public void testString_getActionMappingURL_String_String_PageContext_boolean1() {
        pageContext.getServletContext().setAttribute(Globals.SERVLET_KEY, "*.do");

        ActionConfig actionConfig = new ActionConfig();

        actionConfig.setParameter("/foo");
        moduleConfig.addActionConfig(actionConfig);

        request.setAttribute(Globals.MODULE_KEY, moduleConfig);
        request.setPathElements("/myapp", "/baz.do", null, null);

        assertEquals("Check path /foo",
            tagutils.getActionMappingURL("/foo", pageContext), "/myapp/foo.do");
    }

    // use servlet mapping (extension mapping)
    //  -- with params
    public void testString_getActionMappingURL_String_String_PageContext_boolean2() {
        pageContext.getServletContext().setAttribute(Globals.SERVLET_KEY, "*.do");

        ActionConfig actionConfig = new ActionConfig();

        actionConfig.setParameter("/foo");
        moduleConfig.addActionConfig(actionConfig);

        request.setAttribute(Globals.MODULE_KEY, moduleConfig);
        request.setPathElements("/myapp", "/baz.do?foo=bar", null, null);

        assertEquals("Check path /foo",
            tagutils.getActionMappingURL("/foo?foo=bar", pageContext),
            "/myapp/foo.do?foo=bar");
    }

    // use servlet mapping (extension mapping)
    //  -- path as "/"
    // (this is probably not a realistic use case)
    public void testString_getActionMappingURL_String_String_PageContext_boolean3() {
        pageContext.getServletContext().setAttribute(Globals.SERVLET_KEY, "*.do");

        ActionConfig actionConfig = new ActionConfig();

        actionConfig.setParameter("/foo");
        moduleConfig.addActionConfig(actionConfig);

        request.setAttribute(Globals.MODULE_KEY, moduleConfig);
        request.setPathElements("/mycontext", "/baz", null, null);

        assertEquals("Check path /foo",
            tagutils.getActionMappingURL("/", pageContext), "/mycontext/.do");
    }

    // use servlet mapping (path mapping)
    public void testString_getActionMappingURL_String_String_PageContext_boolean4() {
        pageContext.getServletContext().setAttribute(Globals.SERVLET_KEY,
            "/myapp/*");

        ActionConfig actionConfig = new ActionConfig();

        actionConfig.setParameter("/foo");
        moduleConfig.addActionConfig(actionConfig);

        request.setAttribute(Globals.MODULE_KEY, moduleConfig);
        request.setPathElements("/mycontext", "/baz", null, null);

        assertEquals("Check path /foo",
            tagutils.getActionMappingURL("/foo", pageContext),
            "/mycontext/myapp/foo");
    }

    // use servlet mapping (path mapping)
    //  -- with params
    public void testString_getActionMappingURL_String_String_PageContext_boolean5() {
        pageContext.getServletContext().setAttribute(Globals.SERVLET_KEY,
            "/myapp/*");

        ActionConfig actionConfig = new ActionConfig();

        actionConfig.setParameter("/foo");
        moduleConfig.addActionConfig(actionConfig);

        request.setAttribute(Globals.MODULE_KEY, moduleConfig);
        request.setPathElements("/mycontext", "/baz?foo=bar", null, null);

        assertEquals("Check path /foo",
            tagutils.getActionMappingURL("/foo?foo=bar", pageContext),
            "/mycontext/myapp/foo?foo=bar");
    }

    // use servlet mapping (path mapping)
    //  -- using "/" as mapping
    public void testString_getActionMappingURL_String_String_PageContext_boolean6() {
        pageContext.getServletContext().setAttribute(Globals.SERVLET_KEY, "/");

        ActionConfig actionConfig = new ActionConfig();

        actionConfig.setParameter("/foo");
        moduleConfig.addActionConfig(actionConfig);

        request.setAttribute(Globals.MODULE_KEY, moduleConfig);
        request.setPathElements("/mycontext", "/baz", null, null);

        assertEquals("Check path /foo",
            tagutils.getActionMappingURL("/", pageContext), "/mycontext/");
    }

    // ------------------------------------------ getActionMessages()
    // -- using ActionMessages
    public void testActionMessages_getActionMessages_PageContext_String1() {
        ActionMessages actionMessages = new ActionMessages();

        actionMessages.add("prop", new ActionMessage("key.key"));
        request.setAttribute("messages", actionMessages);

        try {
            ActionMessages messages =
                tagutils.getActionMessages(pageContext, "messages");

            assertNotNull("messages should not be null", messages);
            assertNotNull("messages prop should not be null",
                messages.get("prop"));

            String val = null;
            int i = 0;

            for (Iterator iter = messages.get("prop"); iter.hasNext();) {
                ActionMessage message = (ActionMessage) iter.next();

                val = message.getKey();
                i++;
            }

            assertEquals("only 1 message", i, 1);
            assertEquals("messages prop should match", val, "key.key");
        } catch (JspException e) {
            fail(e.getMessage());
        }
    }

    // -- using ActionErrors
    public void testActionMessages_getActionMessages_PageContext_String2() {
        ActionMessages actionMessages = new ActionMessages();

        actionMessages.add("prop", new ActionMessage("key.key"));
        request.setAttribute("messages", actionMessages);

        try {
            ActionMessages messages =
                tagutils.getActionMessages(pageContext, "messages");

            assertNotNull("messages should not be null", messages);
            assertNotNull("messages prop should not be null",
                messages.get("prop"));

            String val = null;
            int i = 0;

            for (Iterator iter = messages.get("prop"); iter.hasNext();) {
                ActionMessage message = (ActionMessage) iter.next();

                val = message.getKey();
                i++;
            }

            assertEquals("only 1 message", i, 1);
            assertEquals("messages prop should match", val, "key.key");
        } catch (JspException e) {
            fail(e.getMessage());
        }
    }

    // -- using String
    public void testActionMessages_getActionMessages_PageContext_String3() {
        request.setAttribute("foo", "bar");

        try {
            ActionMessages messages =
                tagutils.getActionMessages(pageContext, "foo");

            assertNotNull("messages should not be null", messages);
            assertNotNull("messages prop should not be null",
                messages.get("prop"));

            String key = null;
            int i = 0;

            for (Iterator iter = messages.get(ActionMessages.GLOBAL_MESSAGE);
                iter.hasNext();) {
                ActionMessage message = (ActionMessage) iter.next();

                key = message.getKey();

                Object[] values = message.getValues();

                assertNull(values);
                i++;
            }

            assertEquals("only 1 message", i, 1);
            assertEquals("key should match", key, "bar");
        } catch (JspException e) {
            fail(e.getMessage());
        }
    }

    // -- using String Array
    public void testActionMessages_getActionMessages_PageContext_String4() {
        String[] vals = new String[] { "bar", "baz" };

        request.setAttribute("foo", vals);

        try {
            ActionMessages messages =
                tagutils.getActionMessages(pageContext, "foo");

            assertNotNull("messages should not be null", messages);
            assertNotNull("messages prop should not be null",
                messages.get("prop"));

            String key = null;
            int i = 0;

            for (Iterator iter = messages.get(ActionMessages.GLOBAL_MESSAGE);
                iter.hasNext();) {
                ActionMessage message = (ActionMessage) iter.next();

                key = message.getKey();

                Object[] values = message.getValues();

                assertNull((values));
                assertEquals("1st key should match", key, vals[i]);
                i++;
            }

            assertEquals("only 1 message", i, 2);
        } catch (JspException e) {
            fail(e.getMessage());
        }
    }

    // String Array (thrown JspException)
    public void testActionMessages_getActionMessages_PageContext_String5() {
        request.setAttribute("foo", new MockFormBean());

        ActionMessages messages = null;

        try {
            messages = tagutils.getActionMessages(pageContext, "foo");
            fail("should have thrown JspException");
        } catch (JspException e) {
            assertNull("messages should be null", messages);
        }
    }

    // ActionMessages (thrown Exception)
    // TODO -- currently this does not hit the line for caught Exception
    public void testActionMessages_getActionMessages_PageContext_String6() {
        ActionMessages actionMessages = new ActionMessages();

        actionMessages.add("prop", new ActionMessage("key.key"));
        request.setAttribute("messages", actionMessages);

        try {
            ActionMessages messages =
                tagutils.getActionMessages(pageContext, "does-not-exist");

            assertNotNull("messages should not be null", messages);
            assertNotNull("messages prop should not be null",
                messages.get("prop"));

            for (Iterator iter = messages.get("prop"); iter.hasNext();) {
                fail("Should not have any messages for does-not-exist");
            }
        } catch (JspException e) {
            fail(e.getMessage());
        }
    }

    // ----public ModuleConfig getModuleConfig(PageContext pageContext)
    public void testModuleConfig_getModuleConfig_PageContext() {
        MockServletConfig mockServletConfig = new MockServletConfig();
        ModuleConfig moduleConfig = new ModuleConfigImpl("");
        MockServletContext mockServletContext = new MockServletContext();
        MockHttpServletRequest mockHttpServletRequest =
            new MockHttpServletRequest();
        MockHttpServletResponse mockHttpServletResponse =
            new MockHttpServletResponse();

        mockServletConfig.setServletContext(mockServletContext);

        MockPageContext mockPageContext =
            new MockPageContext(mockServletConfig, mockHttpServletRequest,
                mockHttpServletResponse);

        ModuleConfig foundModuleConfig = null;

        try {
            foundModuleConfig = tagutils.getModuleConfig(mockPageContext);
            fail("Expected ModuleConfig to not be found");
        } catch (NullPointerException ignore) {
            // expected result
        }

        mockHttpServletRequest.setAttribute(Globals.MODULE_KEY, moduleConfig);

        mockPageContext.getServletContext().setAttribute(Globals.MODULE_KEY,
            mockPageContext);

        foundModuleConfig = tagutils.getModuleConfig(mockPageContext);
        assertNotNull(foundModuleConfig);
    }

    // -- public Locale getUserLocale(PageContext pageContext, String locale)
    public void testLocale_getUserLocale_PageContext_String() {
        request.setLocale(Locale.ENGLISH);
        assertEquals(tagutils.getUserLocale(pageContext, ""), Locale.ENGLISH);

        request.setLocale(Locale.CANADA);
        assertEquals(tagutils.getUserLocale(pageContext, ""), Locale.CANADA);
    }

    // -- public boolean isXhtml(PageContext pageContext)
    public void test_boolean_isXhtml_PageContext() {
        assertFalse(tagutils.isXhtml(pageContext));
        pageContext.setAttribute(Globals.XHTML_KEY, "true");

        assertTrue(tagutils.isXhtml(pageContext));
    }

    // -- public Object lookup(PageContext pageContext, String name, String scopeName)
    // lookup with null scope
    public void test_Object_lookup_PageContext_String__String1() {
        pageContext.setAttribute("bean", new MockFormBean());

        try {
            Object val = tagutils.lookup(pageContext, "bean", null);

            assertNotNull((val));
        } catch (JspException e) {
            fail("bean not found:" + e.getMessage());
        }
    }

    // lookup with page scope
    public void test_Object_lookup_PageContext_String__String2() {
        pageContext.setAttribute("bean", new MockFormBean());

        try {
            Object val = tagutils.lookup(pageContext, "bean", "page");

            assertNotNull((val));
        } catch (JspException e) {
            fail("bean not found:" + e.getMessage());
        }
    }

    // lookup with invalid scope
    // -- (where an exception is thrown)
    public void test_Object_lookup_PageContext_String__String3() {
        pageContext.setAttribute("bean", new MockFormBean());

        Object val = null;

        try {
            val = tagutils.lookup(pageContext, "bean", "invalid");
            fail("invalid scope :");
        } catch (JspException e) {
            assertNull((val));
        }
    }

    // try to get the call to throw an IllegalAccessException
    public void test_Object_lookup_PageContext_String_String_String1() {
        //        page.setAttribute("bean", new MockFormBean());
        //        Object val = null;
        //        try {
        //            val = tagutils.lookup(page, "bean", "throwIllegalAccessException");
        //            fail("should have thrown exception");
        //        } catch (JspException e) {
        //            assertNull(val);
        //        }
    }

    // try to get the call to throw an IllegalArgumentException
    public void test_Object_lookup_PageContext_String_String_String2() {
        pageContext.setAttribute("bean", new MockFormBean());

        Object val = null;

        try {
            val = tagutils.lookup(pageContext, "bean", "doesNotExistMethod",
                    "page");
            fail("should have thrown exception");
        } catch (JspException e) {
            assertNull(val);
        }
    }

    // try to get the call to throw an NoSuchMethodException
    public void test_Object_lookup_PageContext_String_String_String3() {
        pageContext.setAttribute("bean", new MockFormBean());

        Object val = null;

        try {
            val = tagutils.lookup(pageContext, "bean", "doesNotExistMethod");
            fail("should have thrown exception");
        } catch (JspException e) {
            assertNull(val);
        }
    }

    /**
     * Testing message()
     *
     * public String message( PageContext pageContext, String bundle, String
     * locale, String key) throws JspException
     */
    public void testMessageBadParams() {
        String val = null;

        try {
            val = tagutils.message(pageContext, "bundle", "locale", "key");
            fail("val should be null");
        } catch (JspException e) {
            assertNull(val);
        }
    }

    // set bundle in page scope
    // message() assumes the bundle will never be in page scope
    // -- bad key
    public void donttestMessagePageBadKey() {
        putBundleInScope(PageContext.PAGE_SCOPE, true);

        String val = null;

        try {
            val = tagutils.message(pageContext, null, null,
                    "foo.bar.does.not.exist");
            fail("val should be null");
        } catch (JspException e) {
            assertNull(val);
        }
    }

    // set bundle in request scope
    // -- bad key
    public void testMessageRequestBadKey() {
        putBundleInScope(PageContext.REQUEST_SCOPE, true);

        String val = null;

        try {
            val = tagutils.message(pageContext, null, null,
                    "foo.bar.does.not.exist");
            assertNull(val);
        } catch (JspException e) {
            fail("val should be null, no exception");
        }
    }

    // set bundle in session scope
    // -- bad key
    //
    // This represents a use case where the user specifically set the bundle
    //  in session under Globals.MESSAGES_KEY.
    // Perhaps we should check session, and throw/log a rather explicit message
    // for why this is a bad idea, instead of ignoring or returning null.
    public void testMessageSessionBadKey() {
        putBundleInScope(PageContext.SESSION_SCOPE, true);

        String val = null;

        try {
            val = tagutils.message(pageContext, null, null,
                    "foo.bar.does.not.exist");
            fail("MessageResources should never be put in session scope.");
        } catch (JspException e) {
            assertNull(val);
        }
    }

    // set bundle in application scope
    // -- bad key
    public void testMessageApplicationBadKey() {
        putBundleInScope(PageContext.APPLICATION_SCOPE, true);

        String val = null;

        try {
            val = tagutils.message(pageContext, null, null,
                    "foo.bar.does.not.exist");
            assertNull(val);
        } catch (JspException e) {
            fail("val should be null, no exception");
        }
    }

    // set bundle in request scope
    // -- good key
    public void testMessageRequestGoodKey() {
        putBundleInScope(PageContext.REQUEST_SCOPE, true);

        String val = null;

        try {
            val = tagutils.message(pageContext, null, null, "foo");
            assertTrue("Validate message value", "bar".equals(val));
        } catch (JspException e) {
            fail("val should be \"bar\"");
        }
    }

    // set bundle in application scope
    // -- good key
    public void testMessageApplicationGoodKey() {
        putBundleInScope(PageContext.APPLICATION_SCOPE, true);

        String val = null;

        try {
            val = tagutils.message(pageContext, null, null, "foo");
            assertTrue("Validate message value", "bar".equals(val));
        } catch (JspException e) {
            fail("val should be \"bar\"");
        }
    }

    /**
     * Tests for:
     *
     * public String message( PageContext pageContext, String bundle, String
     * locale, String key, Object args[]) throws JspException
     */
    public void testMessageRequestGoodKeyWithNullParams() {
        putBundleInScope(PageContext.REQUEST_SCOPE, true);

        String[] args = null;

        String val = null;

        try {
            val = tagutils.message(pageContext, null, null, "foo", args);
            assertTrue("Validate message value", "bar".equals(val));
        } catch (JspException e) {
            fail("val should be \"bar\"");
        }
    }

    public void testMessageApplicationGoodKeyWithNullParams() {
        putBundleInScope(PageContext.REQUEST_SCOPE, true);

        String[] args = null;

        String val = null;

        try {
            val = tagutils.message(pageContext, null, null, "foo", args);
            assertTrue("Validate message value", "bar".equals(val));
        } catch (JspException e) {
            fail("val should be \"bar\"");
        }
    }

    public void testMessageRequestGoodKeyWithParams() {
        putBundleInScope(PageContext.REQUEST_SCOPE, true);

        String[] args = { "I love this" };

        String val = null;

        try {
            val = tagutils.message(pageContext, null, null, "foo.bar", args);
            assertTrue("Validate message value", "I love this bar".equals(val));
        } catch (JspException e) {
            fail("val should be \"bar\"");
        }
    }

    public void testMessageApplicationGoodKeyWithParams() {
        putBundleInScope(PageContext.REQUEST_SCOPE, true);

        String[] args = { "I love this" };

        String val = null;

        try {
            val = tagutils.message(pageContext, null, null, "foo.bar", args);
            assertTrue("Validate message value", "I love this bar".equals(val));
        } catch (JspException e) {
            fail("val should be \"bar\"");
        }
    }

    /**
     * Tests for: public boolean present( PageContext pageContext, String
     * bundle, String locale, String key) throws JspException {
     */
    public void testPresentNulls() {
        boolean result = false;

        try {
            result = tagutils.present(null, null, null, null);
            fail("An exception should have been thrown");
        } catch (JspException e) {
            fail("An npe should have been thrown");
        } catch (NullPointerException e) {
            assertFalse("Correct behaviour", result);
        }
    }

    public void testPresentBadKey() {
        putBundleInScope(PageContext.REQUEST_SCOPE, true);

        boolean result = false;

        try {
            result =
                tagutils.present(pageContext, null, null, "foo.bar.not.exist");
            assertFalse("Value should be null", result);
        } catch (JspException e) {
            fail("An npe should have been thrown");
        } catch (NullPointerException e) {
            assertFalse("Correct behaviour", result);
        }
    }

    public void testPresentGoodKey() {
        putBundleInScope(PageContext.REQUEST_SCOPE, true);

        boolean result = false;

        try {
            result = tagutils.present(pageContext, null, null, "foo");
            assertTrue("Key should have been found", result);
        } catch (JspException e) {
            fail("An exception should have been thrown");
        }
    }

    /**
     * public void write(PageContext pageContext, String text) throws
     * JspException {
     */
    public void testWriteNullParams() {
        try {
            tagutils.write(null, null);
            fail("NullPointerException should have been thrown");
        } catch (JspException e) {
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // pass
        }
    }

    public void testWrite() {
        MockPageContext pg = new MockPageContext(false, false);

        try {
            tagutils.write(pg, null);
        } catch (JspException e) {
            fail("JspException should not have been thrown");
        }
    }

    public void testWriteThrowException() {
        MockPageContext pg = new MockPageContext(true, false);

        try {
            tagutils.write(pg, null);
            fail("JspException should have been thrown");
        } catch (JspException e) {
            // success
        }
    }

    public void testWritePrevious() {
        MockPageContext pg = new MockPageContext(false, false);

        try {
            tagutils.writePrevious(pg, null);
        } catch (JspException e) {
            fail("JspException should not have been thrown");
        }
    }

    public void testWritePreviousThrowException() {
        MockPageContext pg = new MockPageContext(true, false);

        try {
            tagutils.writePrevious(pg, null);
            fail("JspException should have been thrown");
        } catch (JspException e) {
            // success
        }
    }

    public void testWritePreviousBody() {
        MockPageContext pg = new MockPageContext(false, true);

        try {
            tagutils.writePrevious(pg, null);
        } catch (JspException e) {
            fail("JspException should not have been thrown");
        }
    }

    public void testOverrideInstance(){

        class CustomTagUtils extends TagUtils{
            public String filter(String value) {
                return "I HAVE BEEN OVERRIDDEN!";
            }
        }
        // verify original logic
        assertNull("Filter Test", TagUtils.getInstance().filter(null));

        // set the custom instance
        TagUtils.setInstance(new CustomTagUtils());
        assertEquals("Custom Instance Test", TagUtils.getInstance().filter(null), "I HAVE BEEN OVERRIDDEN!");

        // reset back to the cached instance
        TagUtils.setInstance(tagutils);
        assertNull("Filter Test", TagUtils.getInstance().filter(null));

    }
}
