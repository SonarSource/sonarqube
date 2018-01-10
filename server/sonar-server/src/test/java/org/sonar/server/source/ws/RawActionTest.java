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
package org.sonar.server.source.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.SourceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.protobuf.DbFileSources.Data;
import static org.sonar.db.protobuf.DbFileSources.Line;

public class RawActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);

  private WsActionTester ws = new WsActionTester(new RawAction(db.getDbClient(),
    new SourceService(db.getDbClient(), null), userSession,
    new ComponentFinder(db.getDbClient(), resourceTypes)));

  @Test
  public void raw_from_file() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.CODEVIEWER, project);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    db.fileSources().insertFileSource(file, s -> s.setSourceData(
      Data.newBuilder()
        .addLines(Line.newBuilder().setLine(1).setSource("public class HelloWorld {").build())
        .addLines(Line.newBuilder().setLine(2).setSource("}").build())
        .build()));

    String result = ws.newRequest()
      .setParam("key", file.getKey())
      .execute().getInput();

    assertThat(result).isEqualTo("public class HelloWorld {\n}\n");
  }

  @Test
  public void raw_from_branch_file() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.addProjectPermission(UserRole.CODEVIEWER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    db.fileSources().insertFileSource(file, s -> s.setSourceData(
      Data.newBuilder()
        .addLines(Line.newBuilder().setLine(1).setSource("public class HelloWorld {").build())
        .addLines(Line.newBuilder().setLine(2).setSource("}").build())
        .build()));

    String result = ws.newRequest()
      .setParam("key", file.getKey())
      .setParam("branch", file.getBranch())
      .execute().getInput();

    assertThat(result).isEqualTo("public class HelloWorld {\n}\n");
  }

  @Test
  public void fail_on_unknown_file() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'unknown' not found");

    ws.newRequest()
      .setParam("key", "unknown")
      .execute();
  }

  @Test
  public void fail_on_unknown_branch() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.addProjectPermission(UserRole.CODEVIEWER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    db.fileSources().insertFileSource(file);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component '%s' on branch 'unknown' not found", file.getKey()));

    ws.newRequest()
      .setParam("key", file.getKey())
      .setParam("branch", "unknown")
      .execute();
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    ComponentDto project = db.components().insertMainBranch();
    userSession.addProjectPermission(UserRole.CODEVIEWER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    db.fileSources().insertFileSource(file);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", file.getDbKey()));

    ws.newRequest()
      .setParam("key", file.getDbKey())
      .execute();
  }

  @Test
  public void fail_when_wrong_permission() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.ISSUE_ADMIN, project);
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("key", file.getKey())
      .execute();
  }
}
