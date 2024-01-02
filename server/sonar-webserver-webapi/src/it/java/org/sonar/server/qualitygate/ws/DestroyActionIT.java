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
package org.sonar.server.qualitygate.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.qualitygate.SearchQualityGatePermissionQuery.builder;
import static org.sonar.db.user.SearchPermissionQuery.IN;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;

public class DestroyActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final QualityGateFinder qualityGateFinder = new QualityGateFinder(dbClient);
  private final QualityGatesWsSupport wsSupport = new QualityGatesWsSupport(db.getDbClient(), userSession, TestComponentFinder.from(db));

  private final DbSession dbSession = db.getSession();
  private final DestroyAction underTest = new DestroyAction(dbClient, wsSupport, qualityGateFinder);
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void delete_quality_gate() {
    db.qualityGates().createDefaultQualityGate();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_NAME, qualityGate.getName())
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectByUuid(dbSession, qualityGate.getUuid())).isNull();
  }

  @Test
  public void delete_quality_gate_if_non_default_when_a_default_exist() {
    db.qualityGates().createDefaultQualityGate();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_NAME, qualityGate.getName())
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectByUuid(dbSession, qualityGate.getUuid())).isNull();
  }

  @Test
  public void delete_quality_gate_and_any_association_to_any_project() {
    db.qualityGates().createDefaultQualityGate();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto prj1 = db.components().insertPublicProject().getProjectDto();
    ProjectDto prj2 = db.components().insertPublicProject().getProjectDto();
    db.qualityGates().associateProjectToQualityGate(prj1, qualityGate);
    db.qualityGates().associateProjectToQualityGate(prj2, qualityGate);
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_NAME, qualityGate.getName())
      .execute();

    assertThat(db.getDbClient().projectQgateAssociationDao().selectQGateUuidByProjectUuid(dbSession, prj1.getUuid()))
      .isEmpty();
    assertThat(db.getDbClient().projectQgateAssociationDao().selectQGateUuidByProjectUuid(dbSession, prj2.getUuid()))
      .isEmpty();

    assertThat(db.getDbClient().projectQgateAssociationDao().selectQGateUuidByProjectUuid(dbSession, prj1.getUuid()))
      .isEmpty();
    assertThat(db.getDbClient().projectQgateAssociationDao().selectQGateUuidByProjectUuid(dbSession, prj2.getUuid()))
      .isEmpty();
  }

  @Test
  public void delete_quality_gate_and_any_associated_group_permissions() {
    db.qualityGates().createDefaultQualityGate();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();

    db.qualityGates().addGroupPermission(qualityGate, group1);
    db.qualityGates().addGroupPermission(qualityGate, group2);
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_NAME, qualityGate.getName())
      .execute();

    assertThat(db.getDbClient().qualityGateGroupPermissionsDao().selectByQuery(dbSession, builder()
        .setQualityGate(qualityGate)
        .setMembership(IN).build(),
      Pagination.all()))
      .isEmpty();
  }

  @Test
  public void delete_quality_gate_and_any_associated_user_permissions() {
    db.qualityGates().createDefaultQualityGate();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();

    db.qualityGates().addUserPermission(qualityGate, user1);
    db.qualityGates().addUserPermission(qualityGate, user2);
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_NAME, qualityGate.getName())
      .execute();

    assertThat(db.getDbClient().qualityGateUserPermissionDao().selectByQuery(dbSession, builder()
        .setQualityGate(qualityGate)
        .setMembership(IN).build(),
      Pagination.all()))
      .isEmpty();
  }

  @Test
  public void does_not_delete_built_in_quality_gate() {
    db.qualityGates().createDefaultQualityGate();
    QualityGateDto builtInQualityGate = db.qualityGates().insertBuiltInQualityGate();
    db.commit();
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_NAME, valueOf(builtInQualityGate.getName()))
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(format("Operation forbidden for built-in Quality Gate '%s'", builtInQualityGate.getName()));
  }

  @Test
  public void fail_when_missing_name() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> ws.newRequest()
      .execute())
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_to_delete_default_quality_gate() {
    QualityGateDto defaultQualityGate = db.qualityGates().createDefaultQualityGate();
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_NAME, valueOf(defaultQualityGate.getName()))
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The default quality gate cannot be removed");
  }

  @Test
  public void fail_on_unknown_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_NAME, "unknown")
      .execute())
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_not_quality_gates_administer() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().createDefaultQualityGate();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_PROFILES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_NAME, qualityGate.getName())
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.isPost()).isTrue();

    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("name", true));
  }

}
