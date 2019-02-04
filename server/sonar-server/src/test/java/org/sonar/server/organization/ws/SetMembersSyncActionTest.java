/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.organization.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.OrganizationAlmBindingDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_ORGANIZATION;

public class SetMembersSyncActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setRoot();
  @Rule
  public DbTester db = DbTester.create();

  private DbClient dbClient = db.getDbClient();

  private DbSession dbSession = db.getSession();

  private WsActionTester ws = new WsActionTester(new SetMembersSyncAction(dbClient, userSession));

  @Test
  public void definition() {
    OrganizationDto organization = db.organizations().insert();
    db.alm().insertOrganizationAlmBinding(organization, db.alm().insertAlmAppInstall());

    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("set_members_sync");
    assertThat(definition.since()).isEqualTo("7.7");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("organization", true), tuple("enabled", true));
  }

  @Test
  public void update_members_sync() {
    OrganizationDto organization = db.organizations().insert();
    db.alm().insertOrganizationAlmBinding(organization, db.alm().insertAlmAppInstall());

    sendRequest(organization.getKey(), true);

    Optional<OrganizationAlmBindingDto> dto = dbClient.organizationAlmBindingDao().selectByOrganization(dbSession, organization);
    assertThat(dto).isPresent();
    assertThat(dto.get().isMembersSyncEnable()).isTrue();
  }

  @Test
  public void returns_no_content() {
    OrganizationDto organization = db.organizations().insert();
    db.alm().insertOrganizationAlmBinding(organization, db.alm().insertAlmAppInstall());

    TestResponse result = sendRequest(organization.getKey(), true);

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    assertThat(result.getInput()).isEmpty();
  }

  @Test
  public void fail_if_org_is_not_admin_of_the_org() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    sendRequest(organization.getKey(), true);
  }

  @Test
  public void fail_if_org_is_not_bound_to_an_alm() {
    OrganizationDto organization = db.organizations().insert();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(String.format("Organization '%s' is not bound to an ALM", organization.getKey()));

    sendRequest(organization.getKey(), true);
  }

  @Test
  public void fail_if_org_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    db.alm().insertOrganizationAlmBinding(organization, db.alm().insertAlmAppInstall());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Organization '1234' does not exist");

    sendRequest("1234", true);
  }

  private TestResponse sendRequest(@Nullable String organizationKey, @Nullable Boolean enabled) {
    TestRequest request = ws.newRequest();
    ofNullable(organizationKey).ifPresent(o -> request.setParam(PARAM_ORGANIZATION, o));
    ofNullable(enabled).ifPresent(e -> request.setParam("enabled", String.valueOf(e)));

    return request.execute();
  }
}
