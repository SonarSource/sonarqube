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
import it.Category4Suite;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Response;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import util.QaOnly;

import static org.assertj.core.api.Assertions.assertThat;

@Category(QaOnly.class)
public class HttpHeadersTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Test
  public void verify_headers_of_base_url() throws Exception {
    Response response = call(orchestrator.getServer().getUrl() + "/");

    verifySecurityHeaders(response);
    verifyContentType(response, "text/html;charset=utf-8");

    // SONAR-6964
    assertNoCacheInBrowser(response);
  }

  @Test
  public void verify_headers_of_ws() throws Exception {
    Response response = call(orchestrator.getServer().getUrl() + "/api/issues/search");

    verifySecurityHeaders(response);
    verifyContentType(response, "application/json;charset=utf-8");
    assertNoCacheInBrowser(response);
  }

  @Test
  public void verify_headers_of_ruby_ws() throws Exception {
    Response response = call(orchestrator.getServer().getUrl() + "/api/resources/index");

    verifySecurityHeaders(response);
    verifyContentType(response, "application/json;charset=utf-8");
    assertNoCacheInBrowser(response);
  }

  @Test
  public void verify_headers_of_images() throws Exception {
    Response response = call(orchestrator.getServer().getUrl() + "/images/logo.svg");

    verifySecurityHeaders(response);
    verifyContentType(response, "image/svg+xml");
    assertCacheInBrowser(response);
  }

  @Test
  public void verify_headers_of_css() throws Exception {
    Response response = call(orchestrator.getServer().getUrl() + "/css/sonar.css");

    verifySecurityHeaders(response);
    verifyContentType(response, "text/css");
    assertCacheInBrowser(response);
  }

  @Test
  public void verify_headers_of_js() throws Exception {
    Response response = call(orchestrator.getServer().getUrl() + "/js/bundles/sonar.js");

    verifySecurityHeaders(response);
    verifyContentType(response, "application/javascript");
  }

  @Test
  public void verify_headers_of_images_provided_by_plugins() throws Exception {
    Response response = call(orchestrator.getServer().getUrl() + "/static/uiextensionsplugin/cute.jpg");

    verifySecurityHeaders(response);
    verifyContentType(response, "image/jpeg");
  }

  @Test
  public void verify_headers_of_js_provided_by_plugins() throws Exception {
    Response response = call(orchestrator.getServer().getUrl() + "/static/uiextensionsplugin/extension.js");

    verifySecurityHeaders(response);
    verifyContentType(response, "application/javascript");
  }

  @Test
  public void verify_headers_of_html_provided_by_plugins() throws Exception {
    Response response = call(orchestrator.getServer().getUrl() + "/static/uiextensionsplugin/file.html");

    verifySecurityHeaders(response);
    verifyContentType(response, "text/html");
  }

  private static void assertCacheInBrowser(Response httpResponse) {
    okhttp3.CacheControl cacheControl = httpResponse.cacheControl();
    assertThat(cacheControl.mustRevalidate()).isFalse();
    assertThat(cacheControl.noCache()).isFalse();
    assertThat(cacheControl.noStore()).isFalse();
  }

  private static void assertNoCacheInBrowser(Response httpResponse) {
    okhttp3.CacheControl cacheControl = httpResponse.cacheControl();
    assertThat(cacheControl.mustRevalidate()).isTrue();
    assertThat(cacheControl.noCache()).isTrue();
    assertThat(cacheControl.noStore()).isTrue();
  }

  /**
   * SONAR-8247
   */
  private static void verifySecurityHeaders(Response httpResponse) {
    assertThat(httpResponse.isSuccessful()).as("Code is %s", httpResponse.code()).isTrue();
    assertThat(httpResponse.headers().get("X-Frame-Options")).isEqualTo("SAMEORIGIN");
    assertThat(httpResponse.headers().get("X-XSS-Protection")).isEqualTo("1; mode=block");
    assertThat(httpResponse.headers().get("X-Content-Type-Options")).isEqualTo("nosniff");
  }

  private static void verifyContentType(Response httpResponse, String expectedContentType) {
    assertThat(httpResponse.headers().get("Content-Type")).isEqualTo(expectedContentType);
  }

  public static Response call(String url) {
    try {
      return new Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
        .newCall(new okhttp3.Request.Builder().get().url(url).build())
        .execute();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
