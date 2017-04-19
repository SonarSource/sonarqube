/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.source.ws;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.test.JsonAssert.assertJson;

public class IndexActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  WsActionTester tester = new WsActionTester(
    new IndexAction(db.getDbClient(), new SourceService(db.getDbClient(), new HtmlSourceDecorator()), userSession, new ComponentFinder(db.getDbClient())));

  @Test
  public void get_json() throws Exception {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(CODEVIEWER, project);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    insertFileWithData(file, newData("public class HelloWorld {", "}"));

    TestResponse request = tester.newRequest()
      .setParam("resource", file.getKey())
      .execute();

    assertJson(request.getInput()).isSimilarTo("[\n" +
      "  {\n" +
      "    \"1\": \"public class HelloWorld {\",\n" +
      "    \"2\": \"}\"\n" +
      "  }\n" +
      "]");
  }

  @Test
  public void limit_range() throws Exception {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(CODEVIEWER, project);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    insertFileWithData(file, newData("/**", " */", "public class HelloWorld {", "}", "", "foo"));

    TestResponse request = tester.newRequest()
      .setParam("resource", file.getKey())
      .setParam("from", "3")
      .setParam("to", "5")
      .execute();

    assertJson(request.getInput()).isSimilarTo("[\n" +
      "  {\n" +
      "    \"3\": \"public class HelloWorld {\",\n" +
      "    \"4\": \"}\"\n" +
      "  }\n" +
      "]");
  }

  @Test
  public void fail_when_missing_code_viewer_permission() throws Exception {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(USER, project);
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    expectedException.expect(ForbiddenException.class);

    tester.newRequest()
      .setParam("resource", file.getKey())
      .execute();
  }

  @Test
  public void fail_when_component_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    tester.newRequest()
      .setParam("resource", "unknown")
      .execute();
  }

  private static DbFileSources.Data newData(String... lines) throws IOException {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    for (int i = 1; i <= lines.length; i++) {
      dataBuilder.addLinesBuilder()
        .setLine(i)
        .setSource(lines[i - 1])
        .build();
    }
    return dataBuilder.build();
  }

  private void insertFileWithData(ComponentDto file, DbFileSources.Data fileData) throws IOException {
    db.getDbClient().fileSourceDao().insert(db.getSession(), new FileSourceDto()
      .setProjectUuid(file.projectUuid())
      .setFileUuid(file.uuid())
      .setSourceData(fileData));
    db.commit();
  }

}
