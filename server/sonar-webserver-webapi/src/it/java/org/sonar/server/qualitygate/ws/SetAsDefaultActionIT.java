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
package org.sonar.server.qualitygate.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;

public class SetAsDefaultActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final WsActionTester ws = new WsActionTester(
    new SetAsDefaultAction(db.getDbClient(), userSession, new QualityGatesWsSupport(db.getDbClient(), userSession, TestComponentFinder.from(db))));

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.key()).isEqualTo("set_as_default");
    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.changelog()).isNotEmpty();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("name", true));
  }

  @Test
  public void set_default() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("name"));

    ws.newRequest()
      .setParam(PARAM_NAME, "name")
      .execute();

    assertThat(db.getDbClient().propertiesDao().selectGlobalProperty(db.getSession(), "qualitygate.default").getValue())
      .isEqualTo(qualityGate.getUuid());
  }
}
