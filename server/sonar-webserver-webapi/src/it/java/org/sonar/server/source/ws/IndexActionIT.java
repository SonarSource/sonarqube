/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.ProjectPermission.CODEVIEWER;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.test.JsonAssert.assertJson;

public class IndexActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  WsActionTester tester = new WsActionTester(
    new IndexAction(db.getDbClient(), new SourceService(db.getDbClient(), new HtmlSourceDecorator()), userSession, TestComponentFinder.from(db)));

  @Test
  public void get_json() throws Exception {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(CODEVIEWER, project);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    insertFileWithData(file, newData("public class HelloWorld {", "}"));

    TestResponse request = tester.newRequest()
      .setParam("resource", file.getKey())
      .execute();

    assertJson(request.getInput()).isSimilarTo("""
      [
        {
          "1": "public class HelloWorld {",
          "2": "}"
        }
      ]""");
  }

  @Test
  public void limit_range() throws Exception {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(CODEVIEWER, project);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    insertFileWithData(file, newData("/**", " */", "public class HelloWorld {", "}", "", "foo"));

    TestResponse request = tester.newRequest()
      .setParam("resource", file.getKey())
      .setParam("from", "3")
      .setParam("to", "5")
      .execute();

    assertJson(request.getInput()).isSimilarTo("""
      [
        {
          "3": "public class HelloWorld {",
          "4": "}"
        }
      ]""");
  }

  @Test
  public void fail_when_missing_code_viewer_permission() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(USER, project);
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("resource", file.getKey())
      .execute())
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_component_does_not_exist() {
    assertThatThrownBy(() -> tester.newRequest()
      .setParam("resource", "unknown")
      .execute())
        .isInstanceOf(NotFoundException.class);
  }

  private static DbFileSources.Data newData(String... lines) {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    for (int i = 1; i <= lines.length; i++) {
      dataBuilder.addLinesBuilder()
        .setLine(i)
        .setSource(lines[i - 1])
        .build();
    }
    return dataBuilder.build();
  }

  private void insertFileWithData(ComponentDto file, DbFileSources.Data fileData) {
    db.getDbClient().fileSourceDao().insert(db.getSession(), new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid(file.branchUuid())
      .setFileUuid(file.uuid())
      .setSourceData(fileData));
    db.commit();
  }

}
