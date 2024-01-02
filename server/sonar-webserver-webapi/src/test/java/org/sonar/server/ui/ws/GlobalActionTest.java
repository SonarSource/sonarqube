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
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.core.extension.CoreExtensionRepository;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.db.DbClient;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.authentication.DefaultAdminCredentialsVerifier;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ui.PageRepository;
import org.sonar.server.ui.WebAnalyticsLoader;
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

  private final MapSettings settings = new MapSettings();

  private final Server server = mock(Server.class);
  private final NodeInformation nodeInformation = mock(NodeInformation.class);
  private final DbClient dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);
  private final IssueIndexSyncProgressChecker indexSyncProgressChecker = mock(IssueIndexSyncProgressChecker.class);
  private final BranchFeatureRule branchFeature = new BranchFeatureRule();
  private final PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private final WebAnalyticsLoader webAnalyticsLoader = mock(WebAnalyticsLoader.class);
  private final DefaultAdminCredentialsVerifier defaultAdminCredentialsVerifier = mock(DefaultAdminCredentialsVerifier.class);
  private final DocumentationLinkGenerator documentationLinkGenerator = mock(DocumentationLinkGenerator.class);

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
    settings.setProperty("sonar.developerAggregatedInfo.disabled", false);
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
      "    \"sonar.developerAggregatedInfo.disabled\": \"false\"" +
      "  }" +
      "}");
  }

  @Test
  public void return_developer_info_disabled_setting() {
    init();
    settings.setProperty("sonar.developerAggregatedInfo.disabled", true);

    assertJson(call()).isSimilarTo("{" +
      "  \"settings\": {" +
      "    \"sonar.developerAggregatedInfo.disabled\": \"true\"" +
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
    when(dbClient.getDatabase().getDialect()).thenReturn(new PostgreSql());

    assertJson(call()).isSimilarTo("{" +
      "  \"productionDatabase\": true" +
      "}");
  }

  @Test
  public void return_need_issue_sync() {
    init();
    when(indexSyncProgressChecker.isIssueSyncInProgress(any())).thenReturn(true);
    assertJson(call()).isSimilarTo("{\"needIssueSync\": true}");

    when(indexSyncProgressChecker.isIssueSyncInProgress(any())).thenReturn(false);
    assertJson(call()).isSimilarTo("{\"needIssueSync\": false}");
  }

  @Test
  public void instance_uses_default_admin_credentials() {
    init();

    when(defaultAdminCredentialsVerifier.hasDefaultCredentialUser()).thenReturn(true);

    // Even if the default credentials are used, if the current user it not a system admin, the flag is not returned.
    assertJson(call()).isNotSimilarTo("{\"instanceUsesDefaultAdminCredentials\":true}");

    userSession.logIn().setSystemAdministrator();
    assertJson(call()).isSimilarTo("{\"instanceUsesDefaultAdminCredentials\":true}");

    when(defaultAdminCredentialsVerifier.hasDefaultCredentialUser()).thenReturn(false);
    assertJson(call()).isSimilarTo("{\"instanceUsesDefaultAdminCredentials\":false}");
  }

  @Test
  public void standalone_flag() {
    init();
    userSession.logIn().setSystemAdministrator();
    when(nodeInformation.isStandalone()).thenReturn(true);

    assertJson(call()).isSimilarTo("{\"standalone\":true}");
  }

  @Test
  public void not_standalone_flag() {
    init();
    userSession.logIn().setSystemAdministrator();
    when(nodeInformation.isStandalone()).thenReturn(false);

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
    when(dbClient.getDatabase().getDialect()).thenReturn(new PostgreSql());
    when(nodeInformation.isStandalone()).thenReturn(true);
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));
    when(documentationLinkGenerator.getDocumentationLink(null)).thenReturn("http://docs.example.com/10.0");

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

  @Test
  public void web_analytics_js_path_is_not_returned_if_not_defined() {
    init();
    when(webAnalyticsLoader.getUrlPathToJs()).thenReturn(Optional.empty());

    String json = call();
    assertThat(json).doesNotContain("webAnalyticsJsPath");
  }

  @Test
  public void web_analytics_js_path_is_returned_if_defined() {
    init();
    String path = "static/googleanalytics/analytics.js";
    when(webAnalyticsLoader.getUrlPathToJs()).thenReturn(Optional.of(path));

    String json = call();
    assertJson(json).isSimilarTo("{\"webAnalyticsJsPath\":\"" + path + "\"}");
  }

  @Test
  public void call_shouldReturnDocumentationUrl() {
    init();
    String url = "https://docs.sonarsource.com/sonarqube/10.0";
    when(documentationLinkGenerator.getDocumentationLink(null)).thenReturn(url);

    String json = call();
    assertJson(json).isSimilarTo("{\"documentationUrl\":\"" + url + "\"}");
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
      nodeInformation, dbClient, userSession, editionProvider, webAnalyticsLoader,
      indexSyncProgressChecker, defaultAdminCredentialsVerifier, documentationLinkGenerator);
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
