/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.qualitygate.QualityGateUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.CreateResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;

@RunWith(DataProviderRunner.class)
public class CreateActionTest {


  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final CreateAction underTest = new CreateAction(dbClient, userSession, new QualityGateUpdater(dbClient, UuidFactoryFast.getInstance()));
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void default_is_used_when_no_parameter() {
    logInAsQualityGateAdmin();

    String qgName = "Default";
    CreateResponse response = executeRequest(qgName);

    assertThat(response.getName()).isEqualTo(qgName);
    assertThat(response.getId()).isNotNull();
    dbSession.commit();

    QualityGateDto qualityGateDto = dbClient.qualityGateDao().selectByName(dbSession, qgName);
    assertThat(qualityGateDto).isNotNull();
  }

  @Test
  public void create_quality_gate() {
    logInAsQualityGateAdmin();

    String qgName = "Default";
    CreateResponse response = executeRequest(qgName);

    assertThat(response.getName()).isEqualTo(qgName);
    assertThat(response.getId()).isNotNull();
    dbSession.commit();

    QualityGateDto qualityGateDto = dbClient.qualityGateDao().selectByName(dbSession, qgName);
    assertThat(qualityGateDto).isNotNull();
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isTrue();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(1);
  }

  @Test
  public void throw_ForbiddenException_if_not_gate_administrator() {
    userSession.logIn();

    assertThatThrownBy(() -> executeRequest("Default"))
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void throw_BadRequestException_if_name_is_already_used() {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    executeRequest("Default");

    assertThatThrownBy(() -> executeRequest("Default"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Name has already been taken");
  }

  @Test
  @UseDataProvider("nullOrEmpty")
  public void fail_when_name_parameter_is_empty(@Nullable String nameParameter) {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    TestRequest request = ws.newRequest();
    Optional.ofNullable(nameParameter).ifPresent(t -> request.setParam(PARAM_NAME, ""));

    assertThatThrownBy(() -> request.execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The 'name' parameter is missing");
  }

  @DataProvider
  public static Object[][] nullOrEmpty() {
    return new Object[][]{
      {null},
      {""},
      {"  "}
    };
  }

  private CreateResponse executeRequest(String qualitGateName) {
    return ws.newRequest()
      .setParam("name", qualitGateName)
      .executeProtobuf(CreateResponse.class);
  }

  private void logInAsQualityGateAdmin() {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);
  }
}
