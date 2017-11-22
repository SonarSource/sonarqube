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

package org.sonar.server.qualitygate.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.ListWsResponse.QualityGate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.Qualitygates.ListWsResponse;

public class ListActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private WsActionTester ws = new WsActionTester(new ListAction(db.getDbClient()));

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.key()).isEqualTo("list");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription)
      .containsExactlyInAnyOrder(
        tuple("7.0", "'isDefault' field is added on quality gate level"),
        tuple("7.0", "'default' field on root level is deprecated"));
    assertThat(action.params()).isEmpty();
  }

  @Test
  public void json_example() {
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate("Sonar way");
    db.qualityGates().insertQualityGate("Sonar way - Without Coverage");
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    String response = ws.newRequest()
      .execute()
      .getInput();

    assertJson(response).ignoreFields("id", "default")
      .isSimilarTo(getClass().getResource("list-example.json"));
  }

  @Test
  public void list_quality_gates() {
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate("Sonar way");
    QualityGateDto otherQualityGate = db.qualityGates().insertQualityGate("Sonar way - Without Coverage");
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getId, QualityGate::getName, QualityGate::getIsDefault)
    .containsExactlyInAnyOrder(
      tuple(defaultQualityGate.getId(), defaultQualityGate.getName(), true),
      tuple(otherQualityGate.getId(), otherQualityGate.getName(), false));
  }

  @Test
  public void test_deprecated_default_field() {
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate("Sonar way");
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getDefault()).isEqualTo(defaultQualityGate.getId());
  }

  @Test
  public void no_default_quality_gate() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate("Sonar way");

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getId, QualityGate::getName, QualityGate::getIsDefault)
      .containsExactlyInAnyOrder(tuple(qualityGate.getId(), qualityGate.getName(), false));
  }

  @Test
  public void empty() {
    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList()).isEmpty();
  }
}
