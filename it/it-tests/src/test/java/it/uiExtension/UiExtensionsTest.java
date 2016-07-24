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
package it.uiExtension;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category4Suite;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import util.selenium.SeleneseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class UiExtensionsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @BeforeClass
  public static void setUp() throws Exception {
    orchestrator.resetData();
    orchestrator.getServer().provisionProject("sample", "Sample");
  }

  @Test
  public void test_static_files() {
    new SeleneseTest(
      Selenese.builder().setHtmlTestsInClasspath("ui-static-files",
        "/uiExtension/UiExtensionsTest/static-files.html"
        ).build()).runOn(orchestrator);
  }

  /**
   * SONAR-3555
   */
  @Test
  public void content_type_of_static_files_is_set() throws Exception {
    HttpClient httpclient = new DefaultHttpClient();
    try {
      HttpGet get = new HttpGet(orchestrator.getServer().getUrl() + "/static/uiextensionsplugin/cute.jpg");
      HttpResponse response = httpclient.execute(get);
      assertThat(response.getLastHeader("Content-Type").getValue()).isEqualTo("image/jpeg");

      EntityUtils.consume(response.getEntity());

    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }

  /**
   * SONAR-2376
   */
  @Test
  public void test_page_decoration() {
    new SeleneseTest(
      Selenese.builder().setHtmlTestsInClasspath("ui-page-decoration",
        "/uiExtension/UiExtensionsTest/page-decoration.html"
        ).build()).runOn(orchestrator);
  }

  @Test
  public void test_ruby_extensions() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("ui-ruby-extensions",
      "/uiExtension/UiExtensionsTest/ruby-api-tester.html",
      "/uiExtension/UiExtensionsTest/ruby-rails-app.html",
      "/uiExtension/UiExtensionsTest/ruby-rails-app-advanced.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4173
   */
  @Test
  public void test_resource_configuration_extension() {
    new SeleneseTest(
      Selenese.builder().setHtmlTestsInClasspath("resource-configuration-extension",
        "/uiExtension/UiExtensionsTest/resource-configuration-extension.html"
        ).build()).runOn(orchestrator);
  }

}
