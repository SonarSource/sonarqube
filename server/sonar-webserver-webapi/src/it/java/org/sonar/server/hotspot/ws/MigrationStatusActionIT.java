/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.hotspot.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.rule.RuleType;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class MigrationStatusActionIT {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final WsActionTester tester = new WsActionTester(new MigrationStatusAction(userSession, dbClient));

  @Test
  public void handle_whenNotSystemAdministrator_shouldThrowForbidden() {
    userSession.logIn();
    TestRequest request = tester.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void handle_whenNoHotspotLeft_shouldReportComplete() {
    logInAdmin();

    TestResponse response = tester.newRequest().execute();

    assertThat(response.getInput()).contains("\"remainingHotspots\":0", "\"complete\":true");
  }

  @Test
  public void handle_shouldReportRemainingHotspots() {
    logInAdmin();
    RuleDto rule = db.rules().insert();
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    insertHotspot(rule, branch, file);
    insertHotspot(rule, branch, file);
    db.commit();

    TestResponse response = tester.newRequest().execute();

    assertThat(response.getInput()).contains("\"remainingHotspots\":2", "\"complete\":false");
  }

  @Test
  public void handle_whenScopedToProject_shouldCountOnlyThatProjectHotspots() {
    logInAdmin();
    RuleDto rule = db.rules().insert();
    ProjectData projectA = db.components().insertPrivateProject();
    insertHotspot(rule, projectA.getMainBranchComponent(), db.components().insertComponent(newFileDto(projectA.getMainBranchComponent())));
    insertHotspot(rule, projectA.getMainBranchComponent(), db.components().insertComponent(newFileDto(projectA.getMainBranchComponent())));
    ProjectData projectB = db.components().insertPrivateProject();
    insertHotspot(rule, projectB.getMainBranchComponent(), db.components().insertComponent(newFileDto(projectB.getMainBranchComponent())));
    db.commit();

    TestResponse response = tester.newRequest().setParam("project", projectA.getProjectDto().getKey()).execute();

    assertThat(response.getInput()).contains("\"remainingHotspots\":2", "\"complete\":false");
  }

  @Test
  public void handle_whenProjectUnknown_shouldThrowNotFound() {
    logInAdmin();
    TestRequest request = tester.newRequest().setParam("project", "does-not-exist");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("does-not-exist");
  }

  @Test
  public void handle_whenRuleIsStillAHotspot_shouldNotCountItAsRemaining() {
    logInAdmin();
    RuleDto stillHotspotRule = db.rules().insertHotspotRule();
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    insertHotspot(stillHotspotRule, branch, db.components().insertComponent(newFileDto(branch)));
    db.commit();

    TestResponse response = tester.newRequest().execute();

    // Regression guard for SONAR-32194. A finding whose rule is still a Security Hotspot has no target type, so the
    // migrator skips it. Counting it here held the total above zero forever, which made the documented "re-run
    // until it reaches zero" procedure an endless loop of full-table scans.
    //
    // It is reported as notConvertedHotspots instead, so a zero remaining count cannot be read as "every hotspot
    // has been migrated" when the real state is "nothing is migratable yet".
    assertThat(response.getInput())
      .contains("\"remainingHotspots\":0", "\"notConvertedHotspots\":1", "\"complete\":true");
  }

  @Test
  public void handle_whenBranchRowIsGone_shouldNotCountFindingAsRemaining() {
    logInAdmin();
    RuleDto rule = db.rules().insert();
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    insertHotspot(rule, branch, db.components().insertComponent(newFileDto(branch)));
    db.commit();
    // Such rows exist on real instances and can never be migrated, so counting them would also stop the total
    // from reaching zero.
    db.executeUpdateSql("update issues set project_uuid = 'missing-branch-uuid'");

    TestResponse response = tester.newRequest().execute();

    // Deliberately counted by neither field: the project_branches predicate is shared by every migration query.
    // Unlike the not-converted class these really are permanently unmigratable, and they are a rounding error in
    // practice, so they are not surfaced.
    assertThat(response.getInput())
      .contains("\"remainingHotspots\":0", "\"notConvertedHotspots\":0", "\"complete\":true");
  }

  private void insertHotspot(RuleDto rule, ComponentDto branch, ComponentDto file) {
    db.issues().insert(rule, branch, file, i -> i.setType(RuleType.SECURITY_HOTSPOT));
  }

  private void logInAdmin() {
    userSession.logIn().setSystemAdministrator();
  }
}
