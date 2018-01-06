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
package org.sonar.api.web;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ServletFilterTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void include_all() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("/*");
    assertThat(pattern.matches("/")).isTrue();
    assertThat(pattern.matches("/foo/ooo")).isTrue();

    assertThat(pattern.getInclusions()).containsOnly("/*");
    assertThat(pattern.getExclusions()).isEmpty();
  }

  @Test
  public void include_end_of_url() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("*foo");
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/hello/foo")).isTrue();
    assertThat(pattern.matches("/hello/bar")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo2")).isFalse();
  }

  @Test
  public void include_beginning_of_url() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("/foo/*");
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo/bar")).isTrue();
    assertThat(pattern.matches("/bar")).isFalse();
  }

  @Test
  public void include_exact_url() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("/foo");
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo/")).isFalse();
    assertThat(pattern.matches("/bar")).isFalse();
  }

  @Test
  public void exclude_all() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .excludes("/*")
      .build();
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo/ooo")).isFalse();
  }

  @Test
  public void exclude_end_of_url() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .excludes("*foo")
      .build();

    assertThat(pattern.matches("/")).isTrue();
    assertThat(pattern.matches("/hello/foo")).isFalse();
    assertThat(pattern.matches("/hello/bar")).isTrue();
    assertThat(pattern.matches("/foo")).isFalse();
    assertThat(pattern.matches("/foo2")).isTrue();
  }

  @Test
  public void exclude_beginning_of_url() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .excludes("/foo/*")
      .build();

    assertThat(pattern.matches("/")).isTrue();
    assertThat(pattern.matches("/foo")).isFalse();
    assertThat(pattern.matches("/foo/bar")).isFalse();
    assertThat(pattern.matches("/bar")).isTrue();
  }

  @Test
  public void exclude_exact_url() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .excludes("/foo")
      .build();

    assertThat(pattern.matches("/")).isTrue();
    assertThat(pattern.matches("/foo")).isFalse();
    assertThat(pattern.matches("/foo/")).isTrue();
    assertThat(pattern.matches("/bar")).isTrue();
  }

  @Test
  public void use_multiple_include_patterns() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .includes("/foo", "/foo2")
      .build();
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo2")).isTrue();
    assertThat(pattern.matches("/foo/")).isFalse();
    assertThat(pattern.matches("/bar")).isFalse();
  }

  @Test
  public void use_multiple_exclude_patterns() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .excludes("/foo", "/foo2")
      .build();
    assertThat(pattern.matches("/")).isTrue();
    assertThat(pattern.matches("/foo")).isFalse();
    assertThat(pattern.matches("/foo2")).isFalse();
    assertThat(pattern.matches("/foo/")).isTrue();
    assertThat(pattern.matches("/bar")).isTrue();
  }

  @Test
  public void use_include_and_exclude_patterns() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .includes("/foo/*", "/foo/lo*")
      .excludes("/foo/login", "/foo/logout", "/foo/list")
      .build();
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo/login")).isFalse();
    assertThat(pattern.matches("/foo/logout")).isFalse();
    assertThat(pattern.matches("/foo/list")).isFalse();
    assertThat(pattern.matches("/foo/locale")).isTrue();
    assertThat(pattern.matches("/foo/index")).isTrue();
  }

  @Test
  public void exclude_pattern_has_higher_priority_than_include_pattern() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .includes("/foo")
      .excludes("/foo")
      .build();
    assertThat(pattern.matches("/foo")).isFalse();
  }

  @Test
  public void accept_empty_patterns() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .excludes()
      .includes()
      .build();
    assertThat(pattern.matches("/")).isTrue();
    assertThat(pattern.matches("/foo/bar")).isTrue();
  }

  @Test
  public void create_throws_IAE_if_empty_url() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("URL pattern must start with slash '/': ");
    ServletFilter.UrlPattern.create("");
  }

  @Test
  public void filter_should_return_url_pattern() {
    ServletFilter filter = new FakeFilter();
    assertThat(filter.doGetPattern()).isNotNull();
  }

  @Test
  public void filter_should_apply_to_all_urls_by_default() {
    ServletFilter filter = new DefaultFilter();
    assertThat(filter.doGetPattern().matches("/")).isTrue();
    assertThat(filter.doGetPattern().matches("/foo/bar")).isTrue();
  }

  @Test
  public void getUrl_returns_single_inclusion() {
    assertThat(ServletFilter.UrlPattern.create("/*").getUrl()).isEqualTo("/*");
    assertThat(ServletFilter.UrlPattern.create("/foo/bar").getUrl()).isEqualTo("/foo/bar");
  }

  @Test
  public void getUrl_throws_ISE_if_many_urls() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("this method is deprecated and should not be used anymore");

    ServletFilter.UrlPattern.builder()
      .includes("/foo/*", "/foo/lo*")
      .excludes("/foo/login", "/foo/logout", "/foo/list")
      .build().getUrl();
  }

  @Test
  public void test_staticResourcePatterns() {
    assertThat(ServletFilter.UrlPattern.Builder.staticResourcePatterns()).containsOnly(
      "/css/*",
      "/fonts/*",
      "/images/*",
      "/js/*",
      "/static/*",
      "/robots.txt",
      "/favicon.ico",
      "/apple-touch-icon*",
      "/mstile*");
  }

  @Test
  public void test_label() throws Exception {
    assertThat(ServletFilter.UrlPattern.builder().build().label()).isEqualTo("UrlPattern{inclusions=[], exclusions=[]}");
    assertThat(ServletFilter.UrlPattern.builder()
      .includes("/foo/*")
      .excludes("/foo/login")
      .build().label()).isEqualTo("UrlPattern{inclusions=[/foo/*], exclusions=[/foo/login]}");
    assertThat(ServletFilter.UrlPattern.builder()
      .includes("/foo/*", "/foo/lo*")
      .excludes("/foo/login", "/foo/logout", "/foo/list")
      .build().label()).isEqualTo("UrlPattern{inclusions=[/foo/*, ...], exclusions=[/foo/login, ...]}");
  }

  private static class FakeFilter extends ServletFilter {
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

  private static class DefaultFilter extends ServletFilter {
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    }

    public void destroy() {
    }
  }
}
