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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualitygate.QualityGateUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.QualityGate;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_SOURCE_NAME;

@RunWith(DataProviderRunner.class)
public class CopyActionIT {

  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final QualityGateUpdater qualityGateUpdater = new QualityGateUpdater(dbClient);
  private final QualityGatesWsSupport wsSupport = new QualityGatesWsSupport(dbClient, userSession, TestComponentFinder.from(db));

  private final CopyAction underTest = new CopyAction(dbClient, userSession, qualityGateUpdater, wsSupport);
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();

    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isTrue();
    assertThat(action.responseExampleAsString()).isNullOrEmpty();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("sourceName", true),
        tuple("name", true));
  }

  @Test
  public void copy() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric);

    ws.newRequest()
      .setParam(PARAM_SOURCE_NAME, qualityGate.getName())
      .setParam(PARAM_NAME, "new-name")
      .execute();

    QualityGateDto actual = db.getDbClient().qualityGateDao().selectByName(dbSession, "new-name");
    assertThat(actual).isNotNull();
    assertThat(actual.isBuiltIn()).isFalse();
    assertThat(actual.getUuid()).isNotEqualTo(qualityGate.getUuid());
    assertThat(actual.getUuid()).isNotEqualTo(qualityGate.getUuid());

    assertThat(db.getDbClient().gateConditionDao().selectForQualityGate(dbSession, qualityGate.getUuid()))
      .extracting(QualityGateConditionDto::getMetricUuid, QualityGateConditionDto::getErrorThreshold)
      .containsExactlyInAnyOrder(tuple(metric.getUuid(), condition.getErrorThreshold()));
  }

  @Test
  public void copy_of_builtin_should_not_be_builtin() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qualityGateDto -> qualityGateDto.setBuiltIn(true));

    ws.newRequest()
      .setParam(PARAM_SOURCE_NAME, qualityGate.getName())
      .setParam(PARAM_NAME, "new-name")
      .execute();

    QualityGateDto actual = db.getDbClient().qualityGateDao().selectByName(dbSession, "new-name");
    assertThat(actual).isNotNull();
    assertThat(actual.isBuiltIn()).isFalse();
  }

  @Test
  public void response_contains_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    QualityGate response = ws.newRequest()
      .setParam(PARAM_SOURCE_NAME, qualityGate.getName())
      .setParam(PARAM_NAME, "new-name")
      .executeProtobuf(QualityGate.class);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isNotEqualTo(qualityGate.getUuid());
    assertThat(response.getName()).isEqualTo("new-name");
  }

  @Test
  public void fail_when_missing_administer_quality_gate_permission() {
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES);

    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_SOURCE_NAME, qualityGate.getName())
      .setParam(PARAM_NAME, "new-name")
      .execute())
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_source_name_parameter_is_missing() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_NAME, "new-name")
      .execute())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("The 'sourceName' parameter is missing");
  }

  @Test
  public void fail_when_quality_gate_name_is_not_found() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_SOURCE_NAME, "unknown")
      .setParam(PARAM_NAME, "new-name")
      .execute())
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("No quality gate has been found for name unknown");
  }

  @Test
  @UseDataProvider("nullOrEmpty")
  public void fail_when_name_parameter_is_missing(@Nullable String nameParameter) {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    TestRequest request = ws.newRequest()
      .setParam(PARAM_SOURCE_NAME, qualityGate.getName());
    ofNullable(nameParameter).ifPresent(t -> request.setParam(PARAM_NAME, t));

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The 'name' parameter is missing");
  }

  @DataProvider
  public static Object[][] nullOrEmpty() {
    return new Object[][] {
      {null},
      {""},
      {"  "}
    };
  }

  @Test
  public void fail_when_name_parameter_match_existing_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto existingQualityGate = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_SOURCE_NAME, qualityGate.getName())
      .setParam(PARAM_NAME, existingQualityGate.getName())
      .execute())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Name has already been taken");
  }
}
