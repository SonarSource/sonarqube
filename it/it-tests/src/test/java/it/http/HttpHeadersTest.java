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

package it.http;

import com.google.common.base.Throwables;
import com.sonar.orchestrator.Orchestrator;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import it.Category4Suite;
import java.io.IOException;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import util.QaOnly;

import static org.assertj.core.api.Assertions.assertThat;

@Category(QaOnly.class)
public class HttpHeadersTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  /**
   * SONAR-6964
   */
  @Test
  public void no_browser_cache_for_pages() {
    Response httpResponse = call(orchestrator.getServer().getUrl() + "/");

    assertNoCacheInBrowser(httpResponse);
  }

  @Test
  public void no_browser_cache_for_ws() {
    Response httpResponse = call(orchestrator.getServer().getUrl() + "/api/issues/search");

    assertNoCacheInBrowser(httpResponse);
  }

  @Test
  public void no_browser_cache_in_ruby_ws() {
    Response httpResponse = call(orchestrator.getServer().getUrl() + "/api/resources/index");

    assertNoCacheInBrowser(httpResponse);
  }

  @Test
  public void browser_cache_on_images() {
    Response httpResponse = call(orchestrator.getServer().getUrl() + "/images/logo.svg");

    assertCacheInBrowser(httpResponse);
  }

  @Test
  public void browser_cache_on_css() {
    Response httpResponse = call(orchestrator.getServer().getUrl() + "/css/sonar.css");

    assertCacheInBrowser(httpResponse);
  }

  @Test
  public void verify_security_headers_on_base_url() throws Exception {
    verifySecurityHeaders(call(orchestrator.getServer().getUrl() + "/"));
  }

  @Test
  public void verify_security_headers_on_ws() throws Exception {
    verifySecurityHeaders(call(orchestrator.getServer().getUrl() + "/api/issues/search"));
  }

  @Test
  public void verify_security_headers_on_ruby_ws() throws Exception {
    verifySecurityHeaders(call(orchestrator.getServer().getUrl() + "/api/resources/index"));
  }

  @Test
  public void verify_security_headers_on_images() throws Exception {
    verifySecurityHeaders(call(orchestrator.getServer().getUrl() + "/images/logo.svg"));
  }

  @Test
  public void verify_security_headers_on_css() throws Exception {
    verifySecurityHeaders(call(orchestrator.getServer().getUrl() + "/css/sonar.css"));
  }

  private static void assertCacheInBrowser(Response httpResponse) {
    CacheControl cacheControl = httpResponse.cacheControl();
    assertThat(cacheControl.mustRevalidate()).isFalse();
    assertThat(cacheControl.noCache()).isFalse();
    assertThat(cacheControl.noStore()).isFalse();
  }

  private static void assertNoCacheInBrowser(Response httpResponse) {
    CacheControl cacheControl = httpResponse.cacheControl();
    assertThat(cacheControl.mustRevalidate()).isTrue();
    assertThat(cacheControl.noCache()).isTrue();
    assertThat(cacheControl.noStore()).isTrue();
  }

  /**
   * SONAR-8247
   */
  private static void verifySecurityHeaders(Response httpResponse) {
    assertThat(httpResponse.isSuccessful()).isTrue();
    assertThat(httpResponse.headers().get("X-Frame-Options")).isEqualTo("SAMEORIGIN");
    assertThat(httpResponse.headers().get("X-XSS-Protection")).isEqualTo("1; mode=block");
    assertThat(httpResponse.headers().get("X-Content-Type-Options")).isEqualTo("nosniff");
  }

  private static Response call(String url) {
    Request request = new Request.Builder().get().url(url).build();
    try {
      return new OkHttpClient().newCall(request).execute();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
