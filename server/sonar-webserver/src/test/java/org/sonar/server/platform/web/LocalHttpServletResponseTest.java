/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.web;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalHttpServletResponseTest {

  private final LocalHttpServletResponse underTest = new LocalHttpServletResponse();

  @Test
  void outputStream_shouldBeReturnedAsBytes() throws IOException {
    underTest.getOutputStream().write("foo".getBytes(StandardCharsets.UTF_8));
    underTest.getOutputStream().write('!');

    assertThat(underTest.getBytes()).isEqualTo("foo!".getBytes(StandardCharsets.UTF_8));
    assertThat(underTest.getOutputStream().isReady()).isTrue();
  }

  @Test
  void writer_shouldUseCharacterEncodingAndBeFlushedWhenReadingBytes() {
    underTest.setCharacterEncoding("ISO-8859-1");

    underTest.getWriter().write("é");

    assertThat(underTest.getCharacterEncoding()).isEqualTo("ISO-8859-1");
    assertThat(underTest.getBytes()).isEqualTo("é".getBytes(StandardCharsets.ISO_8859_1));
  }

  @Test
  void characterEncoding_whenWriterAlreadyOpened_shouldNotChange() {
    underTest.getWriter();

    underTest.setCharacterEncoding("ISO-8859-1");

    assertThat(underTest.getCharacterEncoding()).isEqualTo("UTF-8");
  }

  @Test
  void writerAndOutputStream_shouldBeMutuallyExclusive() {
    LocalHttpServletResponse withWriter = new LocalHttpServletResponse();
    withWriter.getWriter();
    assertThatThrownBy(withWriter::getOutputStream).isInstanceOf(IllegalStateException.class);

    LocalHttpServletResponse withStream = new LocalHttpServletResponse();
    ServletOutputStream outputStream = withStream.getOutputStream();
    assertThatThrownBy(withStream::getWriter).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> outputStream.setWriteListener(null)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void mediaType_shouldBeContentTypeWithoutParameters() {
    assertThat(underTest.getMediaType()).isNull();

    underTest.setContentType("application/json;charset=UTF-8");

    assertThat(underTest.getContentType()).isEqualTo("application/json;charset=UTF-8");
    assertThat(underTest.getMediaType()).isEqualTo("application/json");
  }

  @Test
  void headers_shouldBeCaseInsensitive() {
    underTest.setHeader("X-Foo", "a");
    underTest.addHeader("x-foo", "b");
    underTest.addHeader("X-Ignored", null);
    underTest.setIntHeader("X-Int", 1);
    underTest.addIntHeader("X-Int", 2);
    underTest.setDateHeader("X-Date", 3L);
    underTest.addDateHeader("X-Date", 4L);
    underTest.setContentLength(10);

    assertThat(underTest.getHeader("x-FOO")).isEqualTo("a");
    assertThat(underTest.getHeaders("X-Foo")).containsExactly("a", "b");
    assertThat(underTest.getHeaders("X-Int")).containsExactly("1", "2");
    assertThat(underTest.getHeaders("X-Date")).containsExactly("3", "4");
    assertThat(underTest.getHeader("Content-Length")).isEqualTo("10");
    assertThat(underTest.getHeaderNames()).containsExactlyInAnyOrder("X-Foo", "X-Int", "X-Date", "Content-Length");
    assertThat(underTest.containsHeader("X-Ignored")).isFalse();
    assertThat(underTest.getHeader("X-Missing")).isNull();
    assertThat(underTest.getHeaders("X-Missing")).isEmpty();

    underTest.setHeader("X-Foo", null);
    assertThat(underTest.containsHeader("X-Foo")).isFalse();
  }

  @Test
  void status_shouldDefaultToOk() {
    assertThat(underTest.getStatus()).isEqualTo(200);

    underTest.setStatus(201);

    assertThat(underTest.getStatus()).isEqualTo(201);
  }

  @Test
  void sendError_shouldClearBodyAndCommitResponse() throws IOException {
    underTest.getOutputStream().write(1);

    underTest.sendError(403, "Forbidden");
    underTest.setStatus(200);

    assertThat(underTest.getStatus()).isEqualTo(403);
    assertThat(underTest.getBytes()).isEmpty();
    assertThat(underTest.isCommitted()).isTrue();
    assertThatThrownBy(() -> underTest.sendError(500)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void reset_shouldClearStatusHeadersAndBody() throws IOException {
    underTest.setStatus(400);
    underTest.setHeader("X-Foo", "a");
    underTest.getOutputStream().write(1);

    underTest.reset();

    assertThat(underTest.getStatus()).isEqualTo(200);
    assertThat(underTest.getHeaderNames()).isEmpty();
    assertThat(underTest.getBytes()).isEmpty();
  }

  @Test
  void flushBuffer_shouldCommitResponse() {
    underTest.getWriter().write("foo");

    underTest.flushBuffer();

    assertThat(underTest.isCommitted()).isTrue();
    assertThat(underTest.getBytes()).isEqualTo("foo".getBytes(StandardCharsets.UTF_8));
    assertThatThrownBy(underTest::resetBuffer).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void sendRedirect_shouldSetLocationAndCommitResponse() throws IOException {
    underTest.getOutputStream().write(1);

    underTest.sendRedirect("/foo", 302, true);

    assertThat(underTest.getStatus()).isEqualTo(302);
    assertThat(underTest.getHeader("Location")).isEqualTo("/foo");
    assertThat(underTest.getBytes()).isEmpty();
    assertThat(underTest.isCommitted()).isTrue();
  }

  @Test
  void defaults_shouldIgnoreCookiesAndKeepUrlsAndLocale() {
    underTest.addCookie(new Cookie("a", "b"));
    underTest.setBufferSize(1024);
    underTest.setLocale(Locale.FRENCH);
    underTest.setLocale(null);

    assertThat(underTest.getLocale()).isEqualTo(Locale.FRENCH);
    assertThat(underTest.getBufferSize()).isZero();
    assertThat(underTest.encodeURL("/foo")).isEqualTo("/foo");
    assertThat(underTest.encodeRedirectURL("/bar")).isEqualTo("/bar");
    assertThat(underTest.getHeaderNames()).isEmpty();
  }
}
