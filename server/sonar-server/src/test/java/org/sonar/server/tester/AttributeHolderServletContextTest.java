/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.tester;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.SessionTrackingMode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AttributeHolderServletContextTest {
  public static final String SOME_STRING = "SOME_STRING";
  public static final Exception SOME_EXCEPTION = new Exception();
  public static final String SOME_OTHER_STRING = "SOME OTHER STRING";
  AttributeHolderServletContext servletContext = new AttributeHolderServletContext();
  public static final ImmutableSet<SessionTrackingMode> SOME_SET_OF_SESSION_TRACKING_MODE = ImmutableSet.of();

  @Test(expected = UnsupportedOperationException.class)
  public void getContextPath_is_not_supported() {
    servletContext.getContextPath();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getContext_is_not_supported() {
    servletContext.getContext(SOME_STRING);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getMajorVersion_is_not_supported() {
    servletContext.getMajorVersion();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getMinorVersion_is_not_supported() {
    servletContext.getMinorVersion();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getEffectiveMajorVersion_is_not_supported() {
    servletContext.getEffectiveMajorVersion();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getEffectiveMinorVersion_is_not_supported() {
    servletContext.getEffectiveMinorVersion();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getMimeType_is_not_supported() {
    servletContext.getMimeType(SOME_STRING);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getResourcePaths_is_not_supported() {
    servletContext.getResourcePaths(SOME_STRING);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getResource_is_not_supported() throws Exception {
    servletContext.getResource(SOME_STRING);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getResourceAsStream_is_not_supported() {
    servletContext.getResourceAsStream(SOME_STRING);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getRequestDispatcher_is_not_supported() {
    servletContext.getRequestDispatcher(SOME_STRING);

  }

  @Test(expected = UnsupportedOperationException.class)
  public void getNamedDispatcher_is_not_supported() {
    servletContext.getNamedDispatcher(SOME_STRING);

  }

  @Test(expected = UnsupportedOperationException.class)
  public void getServlet_is_not_supported() throws ServletException {
    servletContext.getServlet(SOME_STRING);

  }

  @Test(expected = UnsupportedOperationException.class)
  public void getServlets_is_not_supported() {
    servletContext.getServlets();

  }

  @Test(expected = UnsupportedOperationException.class)
  public void getServletNames_is_not_supported() {
    servletContext.getServletNames();

  }

  @Test(expected = UnsupportedOperationException.class)
  public void log_is_not_supported() {
    servletContext.log(SOME_STRING);

  }

  @Test(expected = UnsupportedOperationException.class)
  public void log1_is_not_supported() {
    servletContext.log(SOME_EXCEPTION, SOME_STRING);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void log2_is_not_supported() {
    servletContext.log(SOME_STRING, SOME_EXCEPTION);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getRealPath_is_not_supported() {
    servletContext.getRealPath(SOME_STRING);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getServerInfo_is_not_supported() {
    servletContext.getServerInfo();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getInitParameter_is_not_supported() {
    servletContext.getInitParameter(SOME_STRING);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getInitParameterNames_is_not_supported() {
    servletContext.getInitParameterNames();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void setInitParameter_is_not_supported() {
    servletContext.setInitParameter(SOME_STRING, SOME_STRING);

  }

  @Test
  public void getAttribute_returns_null_when_attributes_are_empty() {
    assertThat(servletContext.getAttribute(SOME_STRING)).isNull();
  }

  @Test
  public void getAttribute_returns_attribute() {
    servletContext.setAttribute(SOME_STRING, SOME_OTHER_STRING);

    assertThat(servletContext.getAttribute(SOME_STRING)).isEqualTo(SOME_OTHER_STRING);
  }

  @Test
  public void getAttributeNames_returns_empty_enumeration_if_attributes_are_empty() {
    Enumeration<String> attributeNames = servletContext.getAttributeNames();
    assertThat(attributeNames.hasMoreElements()).isFalse();
  }

  @Test
  public void getAttributeNames_returns_names_of_attributes() {
    servletContext.setAttribute(SOME_STRING, new Object());
    servletContext.setAttribute(SOME_OTHER_STRING, new Object());

    assertThat(Collections.list(servletContext.getAttributeNames())).containsOnly(SOME_STRING, SOME_OTHER_STRING);
  }

  @Test
  public void removeAttribute_removes_specified_attribute() {
    servletContext.setAttribute(SOME_STRING, new Object());
    servletContext.setAttribute(SOME_OTHER_STRING, new Object());

    servletContext.removeAttribute(SOME_OTHER_STRING);

    assertThat(servletContext.getAttribute(SOME_OTHER_STRING)).isNull();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getServletContextName_is_not_supported() {
    servletContext.getServletContextName();

  }

  @Test(expected = UnsupportedOperationException.class)
  public void addServlet_by_class_is_not_supported() {
    servletContext.addServlet(SOME_STRING, Servlet.class);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void addServlet_by_instance_is_not_supported() {
    servletContext.addServlet(SOME_STRING, new Servlet() {
      @Override
      public void init(ServletConfig servletConfig) {

      }

      @Override
      public ServletConfig getServletConfig() {
        return null;
      }

      @Override
      public void service(ServletRequest servletRequest, ServletResponse servletResponse) {

      }

      @Override
      public String getServletInfo() {
        return null;
      }

      @Override
      public void destroy() {

      }
    });

  }

  @Test(expected = UnsupportedOperationException.class)
  public void addServlet_by_string_is_not_supported() {
    servletContext.addServlet(SOME_STRING, SOME_OTHER_STRING);

  }

  @Test(expected = UnsupportedOperationException.class)
  public void createServlet_is_not_supported() throws ServletException {
    servletContext.createServlet(Servlet.class);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getServletRegistration_is_not_supported() {
    servletContext.getServletRegistration(SOME_STRING);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getServletRegistrations_is_not_supported() {
    servletContext.getServletRegistrations();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void addFilter_by_class_is_not_supported() {
    servletContext.addFilter(SOME_STRING, Filter.class);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void addFilter_by_instance_is_not_supported() {
    servletContext.addFilter(SOME_STRING, new Filter() {
      @Override
      public void init(FilterConfig filterConfig) {

      }

      @Override
      public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {

      }

      @Override
      public void destroy() {

      }
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void addFilter2_by_string_is_not_supported() {
    servletContext.addFilter(SOME_STRING, SOME_OTHER_STRING);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void createFilter_is_not_supported() throws ServletException {
    servletContext.createFilter(Filter.class);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getFilterRegistration_is_not_supported() {
    servletContext.getFilterRegistration(SOME_STRING);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getFilterRegistrations_is_not_supported() {
    servletContext.getFilterRegistrations();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getSessionCookieConfig_is_not_supported() {
    servletContext.getSessionCookieConfig();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void setSessionTrackingModes_is_not_supported() {
    servletContext.setSessionTrackingModes(SOME_SET_OF_SESSION_TRACKING_MODE);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getDefaultSessionTrackingModes_is_not_supported() {
    servletContext.getDefaultSessionTrackingModes();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getEffectiveSessionTrackingModes_is_not_supported() {
    servletContext.getEffectiveSessionTrackingModes();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void addListener_by_class_is_not_supported() {
    servletContext.addListener(EventListener.class);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void addListener_by_string_is_not_supported() {
    servletContext.addListener(SOME_STRING);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void addListener_by_instance_is_not_supported() {
    servletContext.addListener(new EventListener() {
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void createListener_is_not_supported() throws ServletException {
    servletContext.createListener(EventListener.class);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getJspConfigDescriptor_is_not_supported() {
    servletContext.getJspConfigDescriptor();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getClassLoader_is_not_supported() {
    servletContext.getClassLoader();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void declareRoles_is_not_supported() {
    servletContext.declareRoles();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getVirtualServerName_is_not_supported() {
    servletContext.getVirtualServerName();
  }
}
