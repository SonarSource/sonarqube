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

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JakartaToJavaxResponseWrapperTest {

  private final HttpServletResponse delegate = getResponse();

  private HttpServletResponse getResponse() {
    HttpServletResponse mockedResponse = mock(HttpServletResponse.class);
    when(mockedResponse.containsHeader(anyString())).thenReturn(false);
    return mockedResponse;
  }

  private final JakartaToJavaxResponseWrapper underTest = new JakartaToJavaxResponseWrapper(delegate);

  @Test
  void sendRedirectIsDelegated() throws IOException {
    underTest.sendRedirect("redirectUrl");

    verify(delegate).sendRedirect("redirectUrl");
  }

  @Test
  void methodsNotImplemented_throwUnsupportedOperationException() {
    assertThatException().isThrownBy(() -> underTest.setBufferSize(0)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::resetBuffer).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::flushBuffer).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.setLocale(null)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.setContentType("type")).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.setContentLengthLong(0L)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.setContentLength(0)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.setCharacterEncoding(null)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.setStatus(0, "")).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.setStatus(0)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.addIntHeader("",0)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.setIntHeader("",0)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.addDateHeader("",0l)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.setDateHeader("",0l)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.addHeader("","")).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.setHeader("","")).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.sendError(0)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.sendError(0, "")).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.addCookie(null)).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.encodeURL("")).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.encodeRedirectURL("")).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.encodeUrl("")).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.encodeRedirectUrl("")).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getCharacterEncoding).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getContentType).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getStatus).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.getHeader("")).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getBufferSize).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getOutputStream).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getWriter).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getLocale).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.getHeaders("")).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::getHeaderNames).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(() -> underTest.containsHeader("")).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::isCommitted).isInstanceOf(UnsupportedOperationException.class);
    assertThatException().isThrownBy(underTest::reset).isInstanceOf(UnsupportedOperationException.class);

    verifyNoInteractions(delegate);
  }


}
