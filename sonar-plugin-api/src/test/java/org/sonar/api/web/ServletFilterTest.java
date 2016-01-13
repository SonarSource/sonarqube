/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.web;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ServletFilterTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void matchAll() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("/*");
    assertThat(pattern.matches("/")).isTrue();
    assertThat(pattern.matches("/foo/ooo")).isTrue();
    assertThat(pattern.toString()).isEqualTo("/*");
  }

  @Test
  public void matchEndOfUrl() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("*foo");
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/hello/foo")).isTrue();
    assertThat(pattern.matches("/hello/bar")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo2")).isFalse();
  }

  @Test
  public void matchBeginningOfUrl() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("/foo/*");
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo/bar")).isTrue();
    assertThat(pattern.matches("/bar")).isFalse();
  }

  @Test
  public void matchExactUrl() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("/foo");
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo/")).isFalse();
    assertThat(pattern.matches("/bar")).isFalse();
  }

  @Test
  public void url_pattern_cant_be_empty() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Empty url");
    ServletFilter.UrlPattern.create("");
  }

  @Test
  public void filter_should_return_url_pattern() {
    ServletFilter filter = new FakeFilter();
    assertThat(filter.doGetPattern().getUrl()).isEqualTo("/fake");
  }

  @Test
  public void filter_should_apply_to_all_urls_by_default() {
    ServletFilter filter = new DefaultFilter();
    assertThat(filter.doGetPattern().getUrl()).isEqualTo("/*");
  }

  static class FakeFilter extends ServletFilter {
    @Override
    public UrlPattern doGetPattern() {
      return UrlPattern.create("/fake");
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    }

    public void destroy() {
    }
  }

  static class DefaultFilter extends ServletFilter {
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    }

    public void destroy() {
    }
  }
}
