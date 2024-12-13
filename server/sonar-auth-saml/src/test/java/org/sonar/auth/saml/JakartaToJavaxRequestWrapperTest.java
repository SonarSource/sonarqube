/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.auth.saml;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JakartaToJavaxRequestWrapperTest {

  @Test
  void delegate_methods() throws IOException, ServletException, jakarta.servlet.ServletException {

    HttpServletRequest delegateRequest = getDelegateRequest();

    JakartaToJavaxRequestWrapper underTest = new JakartaToJavaxRequestWrapper(delegateRequest);

    assertThat(underTest.getServerPort()).isEqualTo(80);
    assertThat(underTest.getScheme()).isEqualTo("https");
    assertThat(underTest.getServerName()).isEqualTo("hostname");
    assertThat(underTest.getRequestURL()).hasToString("https://hostname:80/path");
    assertThat(underTest.getRequestURI()).isEqualTo("/request-uri");
    assertThat(underTest.getQueryString()).isEqualTo("param1=value1");
    assertThat(underTest.getContextPath()).isEqualTo("/context-path");
    assertThat(underTest.getMethod()).isEqualTo("POST");
    assertThat(underTest.getParameter("param1")).isEqualTo("value1");
    assertThat(underTest.getParameterValues("param1")).containsExactly("value1");
    assertThat(underTest.getHeader("header1")).isEqualTo("hvalue1");
    assertThat(underTest.getHeaders("header1")).isEqualTo(delegateRequest.getHeaders("header1"));
    assertThat(underTest.getHeaderNames()).isEqualTo(delegateRequest.getHeaderNames());
    assertThat(underTest.getRemoteAddr()).isEqualTo("192.168.0.1");
    assertThat(underTest.getRemoteHost()).isEqualTo("remoteHost");
    assertThat(underTest.getRemotePort()).isEqualTo(80);
    assertThat(underTest.getServletPath()).isEqualTo("/servlet-path");
    assertThat(underTest.getReader()).isEqualTo(delegateRequest.getReader());
    assertThat(underTest.getAuthType()).isEqualTo("authType");
    assertThat(underTest.getDateHeader("header1")).isEqualTo(1L);
    assertThat(underTest.getIntHeader("header1")).isEqualTo(1);
    assertThat(underTest.getPathInfo()).isEqualTo("/path-info");
    assertThat(underTest.getPathTranslated()).isEqualTo("/path-translated");
    assertThat(underTest.getRemoteUser()).isEqualTo("remoteUser");
    assertThat(underTest.isUserInRole("role")).isFalse();
    assertThat(underTest.getRequestedSessionId()).isEqualTo("sessionId");
    assertThat(underTest.getProtocol()).isEqualTo("protocol");
    assertThat(underTest.getContentType()).isEqualTo("content-type");
    assertThat(underTest.getContentLength()).isEqualTo(1);
    assertThat(underTest.getContentLengthLong()).isEqualTo(1L);
    assertThat(underTest.getParameterNames()).isEqualTo(delegateRequest.getParameterNames());
    assertThat(underTest.getLocale()).isEqualTo(Locale.ENGLISH);
    assertThat(underTest.getLocales()).isEqualTo(delegateRequest.getLocales());
    assertThat(underTest.getUserPrincipal()).isEqualTo(delegateRequest.getUserPrincipal());
    assertThat(underTest.getLocalName()).isEqualTo("localName");
    assertThat(underTest.getLocalAddr()).isEqualTo("localAddress");
    assertThat(underTest.getLocalPort()).isEqualTo(80);
    assertThat(underTest.getParameterMap()).isEqualTo(delegateRequest.getParameterMap());
    assertThat(underTest.getAttribute("key")).isEqualTo("value");
    assertThat(underTest.getAttributeNames()).isEqualTo(delegateRequest.getAttributeNames());
    assertThat(underTest.getCharacterEncoding()).isEqualTo("encoding");

    assertTrue(underTest.isRequestedSessionIdValid());
    assertTrue(underTest.isRequestedSessionIdFromCookie());
    assertTrue(underTest.isRequestedSessionIdFromURL());
    assertTrue(underTest.isRequestedSessionIdFromUrl());
    assertTrue(underTest.isSecure());

    underTest.changeSessionId();
    verify(delegateRequest).changeSessionId();

    underTest.setAttribute("name", "value");
    verify(delegateRequest).setAttribute("name", "value");

    underTest.setCharacterEncoding("encoding");
    verify(delegateRequest).setCharacterEncoding("encoding");

    underTest.removeAttribute("name");
    verify(delegateRequest).removeAttribute("name");

    underTest.login("name", "password");
    verify(delegateRequest).login("name", "password");

    underTest.logout();
    verify(delegateRequest).logout();
  }

  @Test
  void methodsNotImplemented_throwUnsupportedOperationException() throws IOException {
    HttpServletRequest delegateRequest = getDelegateRequest();

    JakartaToJavaxRequestWrapper underTest = new JakartaToJavaxRequestWrapper(delegateRequest);

    assertThatException().isThrownBy(underTest::getInputStream).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getParts).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getCookies).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getSession).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getAsyncContext).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getDispatcherType).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::isAsyncSupported).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::isAsyncStarted).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::startAsync).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getServletContext).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getInputStream).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.upgrade(null)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.getSession(false)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.authenticate(null)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.getPart(null)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.getRequestDispatcher(null)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.getRequestDispatcher(null)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.startAsync(null, null)).isInstanceOf(UnsupportedOperationException.class);

    verifyNoInteractions(delegateRequest);
  }

  HttpServletRequest getDelegateRequest() throws IOException {
    HttpServletRequest delegateRequest = mock(HttpServletRequest.class);
    Enumeration<String> stringEnumeration = Collections.enumeration(Collections.emptySet());
    Enumeration<Locale> localeEnumeration = Collections.enumeration(Collections.emptySet());

    Map<String, String[]> paramterMap = new HashMap<>();
    when(delegateRequest.getParameterMap()).thenReturn(paramterMap);

    HttpSession httpSession = mock(HttpSession.class);
    when(httpSession.getId()).thenReturn("sessionId");

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("name");
    when(delegateRequest.getUserPrincipal()).thenReturn(principal);

    when(delegateRequest.getSession()).thenReturn(httpSession);

    when(delegateRequest.getAuthType()).thenReturn("authType");
    when(delegateRequest.getCookies()).thenReturn(new jakarta.servlet.http.Cookie[0]);
    when(delegateRequest.getDateHeader("header1")).thenReturn(1L);
    when(delegateRequest.getHeader("header1")).thenReturn("hvalue1");
    when(delegateRequest.getHeaders("header1")).thenReturn(stringEnumeration);
    when(delegateRequest.getHeaderNames()).thenReturn(stringEnumeration);
    when(delegateRequest.getIntHeader("header1")).thenReturn(1);
    when(delegateRequest.getMethod()).thenReturn("POST");
    when(delegateRequest.getPathInfo()).thenReturn("/path-info");
    when(delegateRequest.getPathTranslated()).thenReturn("/path-translated");
    when(delegateRequest.getContextPath()).thenReturn("/context-path");
    when(delegateRequest.getQueryString()).thenReturn("param1=value1");
    when(delegateRequest.getRemoteUser()).thenReturn("remoteUser");
    when(delegateRequest.getRequestURI()).thenReturn("/request-uri");
    when(delegateRequest.getRequestURL()).thenReturn(new StringBuffer("https://hostname:80/path"));
    when(delegateRequest.getServletPath()).thenReturn("/servlet-path");
    when(delegateRequest.getServerName()).thenReturn("hostname");
    when(delegateRequest.getServerPort()).thenReturn(80);
    when(delegateRequest.isSecure()).thenReturn(true);
    when(delegateRequest.getRemoteHost()).thenReturn("remoteHost");
    when(delegateRequest.getLocale()).thenReturn(Locale.ENGLISH);
    when(delegateRequest.getLocales()).thenReturn(localeEnumeration);
    when(delegateRequest.getRemoteAddr()).thenReturn("192.168.0.1");
    when(delegateRequest.getRemotePort()).thenReturn(80);
    BufferedReader bufferedReader = mock(BufferedReader.class);
    when(delegateRequest.getReader()).thenReturn(bufferedReader);
    jakarta.servlet.http.Cookie[] cookies = new jakarta.servlet.http.Cookie[0];
    when(delegateRequest.getCookies()).thenReturn(cookies);
    when(delegateRequest.getScheme()).thenReturn("https");
    when(delegateRequest.getParameter("param1")).thenReturn("value1");
    when(delegateRequest.getParameterValues("param1")).thenReturn(new String[]{"value1"});
    when(delegateRequest.getHeader("header1")).thenReturn("hvalue1");
    Enumeration<String> headers = mock(Enumeration.class);
    when(delegateRequest.getHeaders("header1")).thenReturn(headers);
    when(delegateRequest.isRequestedSessionIdValid()).thenReturn(true);
    when(delegateRequest.isRequestedSessionIdFromCookie()).thenReturn(true);
    when(delegateRequest.isRequestedSessionIdFromURL()).thenReturn(true);
    when(delegateRequest.getProtocol()).thenReturn("protocol");
    when(delegateRequest.getContentType()).thenReturn("content-type");
    when(delegateRequest.getContentLength()).thenReturn(1);
    when(delegateRequest.getContentLengthLong()).thenReturn(1L);
    when(delegateRequest.getParameterNames()).thenReturn(stringEnumeration);
    when(delegateRequest.getLocalName()).thenReturn("localName");
    when(delegateRequest.getLocalAddr()).thenReturn("localAddress");
    when(delegateRequest.getLocalPort()).thenReturn(80);
    when(delegateRequest.getAttribute("key")).thenReturn("value");
    when(delegateRequest.getAttributeNames()).thenReturn(stringEnumeration);
    when(delegateRequest.getCharacterEncoding()).thenReturn("encoding");

    return delegateRequest;
  }

}
