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
package org.sonar.server.component.ws;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsComponents.SearchWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;

public class SearchActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  ComponentDbTester componentDb = new ComponentDbTester(db);
  I18nRule i18n = new I18nRule();

  WsActionTester ws;
  ResourceTypesRule resourceTypes = new ResourceTypesRule();
  Languages languages;

  @Before
  public void setUp() {
    userSession.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    resourceTypes.setAllQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE, Qualifiers.DIRECTORY, Qualifiers.FILE);
    languages = mock(Languages.class);
    when(languages.all()).thenReturn(javaLanguage());

    ws = new WsActionTester(new SearchAction(db.getDbClient(), resourceTypes, i18n, userSession, languages));
  }

  @Test
  public void search_json_example() {
    componentDb.insertComponent(newView());
    ComponentDto project = componentDb.insertComponent(
      newProjectDto("project-uuid")
        .setName("Project Name")
        .setKey("project-key"));
    ComponentDto module = componentDb.insertComponent(
      newModuleDto("module-uuid", project)
        .setName("Module Name")
        .setKey("module-key"));
    ComponentDto directory = newDirectory(module, "path/to/directoy")
      .setUuid("directory-uuid")
      .setKey("directory-key")
      .setName("Directory Name");
    componentDb.insertComponent(directory);
    componentDb.insertComponent(
      newFileDto(module, directory, "file-uuid")
        .setKey("file-key")
        .setLanguage("java")
        .setName("File Name"));
    db.commit();

    String response = newRequest(Qualifiers.PROJECT, Qualifiers.MODULE, Qualifiers.DIRECTORY, Qualifiers.FILE)
      .setMediaType(MediaTypes.JSON)
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("search-components-example.json"));
  }

  @Test
  public void search_with_pagination() throws IOException {
    for (int i = 1; i <= 9; i++) {
      componentDb.insertComponent(
        newProjectDto("project-uuid-" + i)
          .setName("Project Name " + i));
    }
    db.commit();

    InputStream responseStream = newRequest(Qualifiers.PROJECT)
      .setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "3")
      .execute()
      .getInputStream();
    SearchWsResponse response = SearchWsResponse.parseFrom(responseStream);

    assertThat(response.getComponentsCount()).isEqualTo(3);
    assertThat(response.getComponentsList()).extracting("id").containsExactly("project-uuid-4", "project-uuid-5", "project-uuid-6");
  }

  @Test
  public void search_with_key_query() throws IOException {
    componentDb.insertComponent(newProjectDto().setKey("project-_%-key"));
    componentDb.insertComponent(newProjectDto().setKey("project-key-without-escaped-characters"));
    db.commit();

    InputStream responseStream = newRequest(Qualifiers.PROJECT)
      .setParam(Param.TEXT_QUERY, "project-_%-key")
      .execute().getInputStream();
    SearchWsResponse response = SearchWsResponse.parseFrom(responseStream);

    assertThat(response.getComponentsCount()).isEqualTo(1);
    assertThat(response.getComponentsList()).extracting("key").containsExactly("project-_%-key");
  }

  @Test
  public void search_with_language() throws IOException {
    componentDb.insertComponent(newProjectDto().setKey("java-project").setLanguage("java"));
    componentDb.insertComponent(newProjectDto().setKey("cpp-project").setLanguage("cpp"));
    db.commit();

    InputStream responseStream = newRequest(Qualifiers.PROJECT)
      .setParam(PARAM_LANGUAGE, "java")
      .execute().getInputStream();
    SearchWsResponse response = SearchWsResponse.parseFrom(responseStream);

    assertThat(response.getComponentsCount()).isEqualTo(1);
    assertThat(response.getComponentsList().get(0).getKey()).isEqualTo("java-project");
  }

  @Test
  public void fail_if_unknown_qualifier_provided() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter 'qualifiers' (Unknown-Qualifier) must be one of: [BRC, DIR, FIL, TRK]");

    newRequest("Unknown-Qualifier").execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest(Qualifiers.PROJECT).execute();
  }

  @Test
  public void fail_if_insufficient_privileges() {
    expectedException.expect(ForbiddenException.class);
    userSession.login().setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    newRequest(Qualifiers.PROJECT).execute();
  }

  private TestRequest newRequest(String... qualifiers) {
    return ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_QUALIFIERS, Joiner.on(",").join(qualifiers));
  }

  private static Language[] javaLanguage() {
    return new Language[] {new Language() {
      @Override
      public String getKey() {
        return "java";
      }

      @Override
      public String getName() {
        return "Java";
      }

      @Override
      public String[] getFileSuffixes() {
        return new String[0];
      }
    }};
  }
}
