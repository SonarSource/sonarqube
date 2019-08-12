/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.Maps;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

/**
 * A dummy implementation of {@link ServletContext} which only implements the attribute related methods. All other
 * methods thrown a {@link UnsupportedOperationException} when called.
 */
class AttributeHolderServletContext implements ServletContext {
  @Override
  public String getContextPath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServletContext getContext(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getMajorVersion() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getMinorVersion() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getEffectiveMajorVersion() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getEffectiveMinorVersion() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getMimeType(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> getResourcePaths(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public URL getResource(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getResourceAsStream(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RequestDispatcher getNamedDispatcher(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Servlet getServlet(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Enumeration<Servlet> getServlets() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Enumeration<String> getServletNames() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void log(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void log(Exception e, String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void log(String s, Throwable throwable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getRealPath(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getServerInfo() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getInitParameter(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Enumeration<String> getInitParameterNames() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean setInitParameter(String s, String s1) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getAttribute(String s) {
    return this.attributes.get(s);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(this.attributes.keySet());
  }

  private final Map<String, Object> attributes = Maps.newHashMap();

  @Override
  public void setAttribute(String s, Object o) {
    attributes.put(s, o);
  }

  @Override
  public void removeAttribute(String s) {
    attributes.remove(s);
  }

  @Override
  public String getServletContextName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServletRegistration.Dynamic addServlet(String s, String s1) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServletRegistration.Dynamic addServlet(String s, Servlet servlet) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServletRegistration.Dynamic addServlet(String s, Class<? extends Servlet> aClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends Servlet> T createServlet(Class<T> aClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServletRegistration getServletRegistration(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, ? extends ServletRegistration> getServletRegistrations() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FilterRegistration.Dynamic addFilter(String s, String s1) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FilterRegistration.Dynamic addFilter(String s, Filter filter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FilterRegistration.Dynamic addFilter(String s, Class<? extends Filter> aClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends Filter> T createFilter(Class<T> aClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FilterRegistration getFilterRegistration(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SessionCookieConfig getSessionCookieConfig() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setSessionTrackingModes(Set<SessionTrackingMode> set) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addListener(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends EventListener> void addListener(T t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addListener(Class<? extends EventListener> aClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends EventListener> T createListener(Class<T> aClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public JspConfigDescriptor getJspConfigDescriptor() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ClassLoader getClassLoader() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void declareRoles(String... strings) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getVirtualServerName() {
    throw new UnsupportedOperationException();
  }
}
