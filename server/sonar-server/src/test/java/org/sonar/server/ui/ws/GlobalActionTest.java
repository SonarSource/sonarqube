/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.ui.ws;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.PageDefinition;
import org.sonar.core.extension.CoreExtensionRepository;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.db.DbClient;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MySql;
import org.sonar.server.branch.BranchFeatureRule;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.platform.WebServer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ui.PageRepository;
import org.sonar.server.ws.WsActionTester;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class GlobalActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private MapSettings settings = new MapSettings();

  private Server server = mock(Server.class);
  private WebServer webServer = mock(WebServer.class);
  private DbClient dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.fromUuid("foo");
  private BranchFeatureRule branchFeature = new BranchFeatureRule();
  private PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);

  private WsActionTester ws;

  @Test
  public void empty_call() {
    init();

    assertJson(call()).isSimilarTo("{" +
      "  \"globalPages\": []," +
      "  \"settings\": {}," +
      "  \"qualifiers\": []" +
      "}");
  }

  @Test
  public void return_qualifiers() {
    init(new Page[] {}, new ResourceTypeTree[] {
      ResourceTypeTree.builder()
        .addType(ResourceType.builder("POL").build())
        .addType(ResourceType.builder("LOP").build())
        .addRelations("POL", "LOP")
        .build(),
      ResourceTypeTree.builder()
        .addType(ResourceType.builder("PAL").build())
        .addType(ResourceType.builder("LAP").build())
        .addRelations("PAL", "LAP")
        .build()
    });

    assertJson(call()).isSimilarTo("{" +
      "  \"qualifiers\": [\"POL\", \"PAL\"]" +
      "}");
  }

  @Test
  public void return_settings() {
    settings.setProperty("sonar.lf.logoUrl", "http://example.com/my-custom-logo.png");
    settings.setProperty("sonar.lf.logoWidthPx", 135);
    settings.setProperty("sonar.lf.gravatarServerUrl", "https://secure.gravatar.com/avatar/{EMAIL_MD5}.jpg?s={SIZE}&d=identicon");
    settings.setProperty("sonar.lf.enableGravatar", true);
    settings.setProperty("sonar.updatecenter.activate", false);
    settings.setProperty("sonar.technicalDebt.ratingGrid", "0.05,0.1,0.2,0.5");
    // This setting should be ignored as it's not needed
    settings.setProperty("sonar.defaultGroup", "sonar-users");
    init();

    assertJson(call()).isSimilarTo("{" +
      "  \"settings\": {" +
      "    \"sonar.lf.logoUrl\": \"http://example.com/my-custom-logo.png\"," +
      "    \"sonar.lf.logoWidthPx\": \"135\"," +
      "    \"sonar.lf.gravatarServerUrl\": \"https://secure.gravatar.com/avatar/{EMAIL_MD5}.jpg?s={SIZE}&d=identicon\"," +
      "    \"sonar.lf.enableGravatar\": \"true\"," +
      "    \"sonar.updatecenter.activate\": \"false\"," +
      "    \"sonar.technicalDebt.ratingGrid\": \"0.05,0.1,0.2,0.5\"" +
      "  }" +
      "}");
  }

  @Test
  public void return_sonarcloud_settings() {
    settings.setProperty("sonar.sonarcloud.enabled", true);
    settings.setProperty("sonar.prismic.accessToken", "secret");
    settings.setProperty("sonar.analytics.trackingId", "ga_id");
    settings.setProperty("sonar.homepage.url", "https://s3/homepage.json");
    init();

    assertJson(call()).isSimilarTo("{" +
      "  \"settings\": {" +
      "    \"sonar.prismic.accessToken\": \"secret\"," +
      "    \"sonar.analytics.trackingId\": \"ga_id\"," +
      "    \"sonar.homepage.url\": \"https://s3/homepage.json\"" +
      "  }" +
      "}");
  }

  @Test
  public void return_deprecated_logo_settings() {
    init();
    settings.setProperty("sonar.lf.logoUrl", "http://example.com/my-custom-logo.png");
    settings.setProperty("sonar.lf.logoWidthPx", 135);

    assertJson(call()).isSimilarTo("{" +
      "  \"settings\": {" +
      "    \"sonar.lf.logoUrl\": \"http://example.com/my-custom-logo.png\"," +
      "    \"sonar.lf.logoWidthPx\": \"135\"" +
      "  }," +
      "  \"logoUrl\": \"http://example.com/my-custom-logo.png\"," +
      "  \"logoWidth\": \"135\"" +
      "}");
  }

  @Test
  public void the_returned_global_pages_do_not_include_administration_pages() {
    init(createPages(), new ResourceTypeTree[] {});

    assertJson(call()).isSimilarTo("{" +
      "  \"globalPages\": [" +
      "    {" +
      "      \"key\": \"another_plugin/page\"," +
      "      \"name\": \"My Another Page\"" +
      "    }," +
      "    {" +
      "      \"key\": \"my_plugin/page\"," +
      "      \"name\": \"My Plugin Page\"" +
      "    }" +
      "  ]" +
      "}");
  }

  @Test
  public void return_sonarqube_version() {
    init();
    when(server.getVersion()).thenReturn("6.2");

    assertJson(call()).isSimilarTo("{" +
      "  \"version\": \"6.2\"" +
      "}");
  }

  @Test
  public void functional_version_when_4_digits() {
    init();
    when(server.getVersion()).thenReturn("6.3.1.1234");

    String result = call();

    assertThat(result).contains("6.3.1 (build 1234)");
  }

  @Test
  public void functional_version_when_third_digit_is_0() {
    init();
    when(server.getVersion()).thenReturn("6.3.0.1234");

    String result = call();

    assertThat(result).contains("6.3 (build 1234)");
  }

  @Test
  public void return_if_production_database_or_not() {
    init();
    when(dbClient.getDatabase().getDialect()).thenReturn(new MySql());

    assertJson(call()).isSimilarTo("{" +
      "  \"productionDatabase\": true" +
      "}");
  }

  @Test
  public void organization_support() {
    init();
    organizationFlags.setEnabled(true);

    assertJson(call()).isSimilarTo("{" +
      "  \"organizationsEnabled\": true," +
      "  \"defaultOrganization\": \"key_foo\"" +
      "}");
  }

  @Test
  public void branch_support() {
    init();
    branchFeature.setEnabled(true);
    assertJson(call()).isSimilarTo("{\"branchesEnabled\":true}");

    branchFeature.setEnabled(false);
    assertJson(call()).isSimilarTo("{\"branchesEnabled\":false}");
  }

  @Test
  public void can_admin_on_global_level() {
    init();
    userSession.logIn().setRoot();

    assertJson(call()).isSimilarTo("{\"canAdmin\":true}");
  }

  @Test
  public void standalone_flag() {
    init();
    userSession.logIn().setRoot();
    when(webServer.isStandalone()).thenReturn(true);

    assertJson(call()).isSimilarTo("{\"standalone\":true}");
  }

  @Test
  public void not_standalone_flag() {
    init();
    userSession.logIn().setRoot();
    when(webServer.isStandalone()).thenReturn(false);

    assertJson(call()).isSimilarTo("{\"standalone\":false}");
  }

  @Test
  public void test_example_response() {
    settings.setProperty("sonar.lf.logoUrl", "http://example.com/my-custom-logo.png");
    settings.setProperty("sonar.lf.logoWidthPx", 135);
    settings.setProperty("sonar.lf.gravatarServerUrl", "http://some-server.tld/logo.png");
    settings.setProperty("sonar.lf.enableGravatar", true);
    settings.setProperty("sonar.updatecenter.activate", false);
    settings.setProperty("sonar.technicalDebt.ratingGrid", "0.05,0.1,0.2,0.5");
    init(createPages(), new ResourceTypeTree[] {
      ResourceTypeTree.builder()
        .addType(ResourceType.builder("POL").build())
        .addType(ResourceType.builder("LOP").build())
        .addRelations("POL", "LOP")
        .build(),
      ResourceTypeTree.builder()
        .addType(ResourceType.builder("PAL").build())
        .addType(ResourceType.builder("LAP").build())
        .addRelations("PAL", "LAP")
        .build()
    });
    when(server.getVersion()).thenReturn("6.2");
    when(dbClient.getDatabase().getDialect()).thenReturn(new MySql());
    when(webServer.isStandalone()).thenReturn(true);
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));

    String result = call();
    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void edition_is_not_returned_if_not_defined() {
    init();
    when(editionProvider.get()).thenReturn(Optional.empty());

    String json = call();
    assertThat(json).doesNotContain("edition");
  }

  @Test
  public void edition_is_returned_if_defined() {
    init();
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    String json = call();
    assertJson(json).isSimilarTo("{\"edition\":\"developer\"}");
  }

  private void init() {
    init(new org.sonar.api.web.page.Page[] {}, new ResourceTypeTree[] {});
  }

  private void init(org.sonar.api.web.page.Page[] pages, ResourceTypeTree[] resourceTypeTrees) {
    when(dbClient.getDatabase().getDialect()).thenReturn(new H2());
    when(server.getVersion()).thenReturn("6.42");
    PluginRepository pluginRepository = mock(PluginRepository.class);
    when(pluginRepository.hasPlugin(any())).thenReturn(true);
    when(pluginRepository.getPluginInfo(any())).thenReturn(new PluginInfo("unused").setVersion(Version.create("1.0")));
    CoreExtensionRepository coreExtensionRepository = mock(CoreExtensionRepository.class);
    when(coreExtensionRepository.isInstalled(any())).thenReturn(false);
    PageRepository pageRepository = new PageRepository(pluginRepository, coreExtensionRepository, new PageDefinition[] {context -> {
      for (Page page : pages) {
        context.addPage(page);
      }
    }});
    pageRepository.start();
    GlobalAction wsAction = new GlobalAction(pageRepository, settings.asConfig(), new ResourceTypes(resourceTypeTrees), server,
      webServer, dbClient, organizationFlags, defaultOrganizationProvider, branchFeature, userSession, editionProvider);
    ws = new WsActionTester(wsAction);
    wsAction.start();
  }

  private String call() {
    return ws.newRequest().execute().getInput();
  }

  private Page[] createPages() {
    Page page = Page.builder("my_plugin/page").setName("My Plugin Page").build();
    Page anotherPage = Page.builder("another_plugin/page").setName("My Another Page").build();
    Page adminPage = Page.builder("my_plugin/admin_page").setName("Admin Page").setAdmin(true).build();

    return new Page[] {page, anotherPage, adminPage};
  }
}
