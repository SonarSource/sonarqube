/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.QualityGate;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_CURRENT_NAME;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;

public class RenameActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final WsActionTester ws = new WsActionTester(
    new RenameAction(db.getDbClient(), new QualityGatesWsSupport(db.getDbClient(), userSession, TestComponentFinder.from(db))));

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.key()).isEqualTo("rename");
    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.changelog()).isNotEmpty();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple(PARAM_CURRENT_NAME, true),
        tuple(PARAM_NAME, true));
  }

  @Test
  public void rename() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("old name"));
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_CURRENT_NAME, qualityGate.getName())
      .setParam(PARAM_NAME, "new name")
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectByUuid(db.getSession(), qualityGate.getUuid()).getName()).isEqualTo("new name");
  }

  @Test
  public void response_contains_quality_gate() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("old name"));

    QualityGate result = ws.newRequest()
      .setParam(PARAM_CURRENT_NAME, qualityGate.getName())
      .setParam(PARAM_NAME, "new name")
      .executeProtobuf(QualityGate.class);

    assertThat(result.getId()).isEqualTo(qualityGate.getUuid());
    assertThat(result.getName()).isEqualTo("new name");
  }

  @Test
  public void rename_with_same_name() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName(PARAM_NAME));

    ws.newRequest()
      .setParam(PARAM_CURRENT_NAME, qualityGate.getName())
      .setParam(PARAM_NAME, "name")
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectByUuid(db.getSession(), qualityGate.getUuid()).getName()).isEqualTo(PARAM_NAME);
  }

  @Test
  public void fail_on_built_in_quality_gate() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setBuiltIn(true));

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_CURRENT_NAME, qualityGate.getName())
      .setParam(PARAM_NAME, "name")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(format("Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName()));
  }

  @Test
  public void fail_on_empty_name() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_CURRENT_NAME, qualityGate.getName())
      .setParam(PARAM_NAME, "")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The 'name' parameter is missing");
  }

  @Test
  public void fail_when_using_existing_name() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_CURRENT_NAME, qualityGate1.getName())
      .setParam(PARAM_NAME, qualityGate2.getName())
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(format("Name '%s' has already been taken", qualityGate2.getName()));
  }

  @Test
  public void fail_on_unknown_quality_gate() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_CURRENT_NAME, "unknown")
      .setParam(PARAM_NAME, "new name")
      .execute())
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_not_quality_gates_administer() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_PROFILES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("old name"));

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_CURRENT_NAME, qualityGate.getName())
      .setParam(PARAM_NAME, "new name")
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

}
