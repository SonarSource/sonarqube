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
package org.sonar.server.projectlink.ws;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentLinkDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsProjectLinks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_URL;

public class CreateActionTest {

  private final String PROJECT_KEY = KEY_PROJECT_EXAMPLE_001;
  private final String PROJECT_UUID = UUID_EXAMPLE_01;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  WsActionTester ws;

  CreateAction underTest;

  @Before
  public void setUp() {
    ComponentFinder componentFinder = new ComponentFinder(dbClient);
    underTest = new CreateAction(dbClient, userSession, componentFinder);
    ws = new WsActionTester(underTest);

    userSession.login("login").setGlobalPermissions(SYSTEM_ADMIN);
  }

  @Test
  public void example_with_key() {
    insertProject();

    String result = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROJECT_KEY, PROJECT_KEY)
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute().getInput();

    assertJson(result).ignoreFields("id").isSimilarTo(getClass().getResource("create-example.json"));
  }

  @Test
  public void example_with_id() {
    insertProject();

    String result = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROJECT_ID, PROJECT_UUID)
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute().getInput();

    assertJson(result).ignoreFields("id").isSimilarTo(getClass().getResource("create-example.json"));
  }

  @Test
  public void global_admin() throws IOException {
    userSession.login("login").setGlobalPermissions(SYSTEM_ADMIN);
    insertProject();
    createAndTest();
  }

  @Test
  public void project_admin() throws IOException {
    userSession.login("login");
    ComponentDto project = insertProject();
    userSession.addProjectUuidPermissions(UserRole.ADMIN, project.uuid());
    createAndTest();
  }

  @Test
  public void with_long_name() throws IOException {
    insertProject();

    String longName = StringUtils.leftPad("", 60, "a");
    String expectedType = StringUtils.leftPad("", 20, "a");
    createAndTest(longName, "http://example.org", expectedType);
  }

  @Test
  public void fail_if_no_name() {
    expectedException.expect(IllegalArgumentException.class);
    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_URL, "http://example.org")
      .execute();
  }

  @Test
  public void fail_if_long_name() {
    expectedException.expect(IllegalArgumentException.class);
    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_NAME, StringUtils.leftPad("", 129, "*"))
      .setParam(PARAM_URL, "http://example.org")
      .execute();
  }

  @Test
  public void fail_if_no_url() {
    expectedException.expect(IllegalArgumentException.class);
    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_NAME, "Custom")
      .execute();
  }

  @Test
  public void fail_if_long_url() {
    expectedException.expect(IllegalArgumentException.class);
    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_NAME, "random")
      .setParam(PARAM_URL, StringUtils.leftPad("", 2049, "*"))
      .execute();
  }

  @Test
  public void fail_when_no_project() {
    expectedException.expect(NotFoundException.class);
    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute();
  }

  @Test
  public void fail_if_anonymous() {
    userSession.anonymous();
    insertProject();

    expectedException.expect(ForbiddenException.class);
    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, PROJECT_KEY)
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute();
  }

  @Test
  public void fail_if_not_project_admin() {
    userSession.login("login");
    insertProject();

    expectedException.expect(ForbiddenException.class);
    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, PROJECT_KEY)
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute();
  }

  private ComponentDto insertProject() {
    ComponentDto project = new ComponentDto()
      .setOrganizationUuid("org1")
      .setUuid(PROJECT_UUID)
      .setKey(PROJECT_KEY)
      .setUuidPath("")
      .setRootUuid("");
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();
    return project;
  }

  private void createAndTest(String name, String url, String type) throws IOException {
    InputStream responseStream = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROJECT_KEY, PROJECT_KEY)
      .setParam(PARAM_NAME, name)
      .setParam(PARAM_URL, url)
      .setMediaType(PROTOBUF)
      .execute().getInputStream();

    WsProjectLinks.CreateWsResponse response = WsProjectLinks.CreateWsResponse.parseFrom(responseStream);

    long newId = Long.valueOf(response.getLink().getId());

    ComponentLinkDto link = dbClient.componentLinkDao().selectById(dbSession, newId);
    assertThat(link.getName()).isEqualTo(name);
    assertThat(link.getHref()).isEqualTo(url);
    assertThat(link.getType()).isEqualTo(type);
  }

  private void createAndTest() throws IOException {
    createAndTest("Custom", "http://example.org", "custom");
  }
}
