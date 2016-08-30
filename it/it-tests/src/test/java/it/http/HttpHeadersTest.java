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
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

  private static Response call(String url) {
    Request request = new Request.Builder().get().url(url).build();
    try {
      // SonarQube ws-client cannot be used as it does not
      // expose HTTP headers
      return new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
        .newCall(request).execute();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
