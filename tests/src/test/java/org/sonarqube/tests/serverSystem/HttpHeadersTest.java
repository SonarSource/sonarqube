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
package org.sonarqube.tests.serverSystem;

import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.Category4Suite;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import okhttp3.CacheControl;
import okhttp3.Response;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static com.google.common.io.Files.getFileExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.call;

public class HttpHeadersTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  private static String JS_HASH;

  @BeforeClass
  public static void setUp() throws Exception {
    JS_HASH = getJsHash();
  }

  @Test
  public void verify_headers_of_base_url() {
    Response response = call(orchestrator.getServer().getUrl() + "/");

    verifySecurityHeaders(response);
    verifyContentType(response, "text/html;charset=utf-8");

    // SONAR-6964
    assertNoCacheInBrowser(response);
  }

  @Test
  public void verify_headers_of_ws() {
    Response response = call(orchestrator.getServer().getUrl() + "/api/issues/search");

    verifySecurityHeaders(response);
    verifyContentType(response, "application/json");
    assertNoCacheInBrowser(response);
  }

  @Test
  public void verify_headers_of_images() {
    Response response = call(orchestrator.getServer().getUrl() + "/images/logo.svg");

    verifySecurityHeaders(response);
    verifyContentType(response, "image/svg+xml");
    assertCacheInBrowser(response);
  }

  @Test
  public void verify_headers_of_css() {
    Response response = call(orchestrator.getServer().getUrl() + "/css/sonar." + JS_HASH + ".css");

    verifySecurityHeaders(response);
    verifyContentType(response, "text/css");
    assertCacheInBrowser(response);
  }

  @Test
  public void verify_headers_of_js() {
    Response response = call(orchestrator.getServer().getUrl() + "/js/app." + JS_HASH + ".js");

    verifySecurityHeaders(response);
    verifyContentType(response, "application/javascript");
  }

  @Test
  public void verify_headers_of_images_provided_by_plugins() {
    Response response = call(orchestrator.getServer().getUrl() + "/static/uiextensionsplugin/cute.jpg");

    verifySecurityHeaders(response);
    verifyContentType(response, "image/jpeg");
  }

  @Test
  public void verify_headers_of_js_provided_by_plugins() {
    Response response = call(orchestrator.getServer().getUrl() + "/static/uiextensionsplugin/extension.js");

    verifySecurityHeaders(response);
    verifyContentType(response, "application/javascript");
  }

  @Test
  public void verify_headers_of_html_provided_by_plugins() {
    Response response = call(orchestrator.getServer().getUrl() + "/static/uiextensionsplugin/file.html");

    verifySecurityHeaders(response);
    verifyContentType(response, "text/html");
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
    assertThat(httpResponse.isSuccessful()).as("Code is %s", httpResponse.code()).isTrue();
    assertThat(httpResponse.headers().get("X-Frame-Options")).isEqualTo("SAMEORIGIN");
    assertThat(httpResponse.headers().get("X-XSS-Protection")).isEqualTo("1; mode=block");
    assertThat(httpResponse.headers().get("X-Content-Type-Options")).isEqualTo("nosniff");
  }

  private static void verifyContentType(Response httpResponse, String expectedContentType) {
    assertThat(httpResponse.headers().get("Content-Type")).isEqualTo(expectedContentType);
  }

  /**
   * Every JS and CSS files contains a hash between the file name and the extension.
   */
  private static String getJsHash() throws IOException {
    File cssFolder = new File(orchestrator.getServer().getHome(), "web/css");
    String fileName = Files.list(cssFolder.toPath())
      .map(path -> path.toFile().getName())
      .filter(name -> getFileExtension(name).equals("css"))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("sonar.css hasn't been found"));
    return fileName.replace("sonar.", "").replace(".css", "");
  }
}
