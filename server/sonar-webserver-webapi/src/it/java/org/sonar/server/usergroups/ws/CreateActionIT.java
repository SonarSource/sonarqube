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
package org.sonar.server.usergroups.ws;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;

public class CreateActionIT {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private ManagedInstanceChecker managedInstanceChecker = mock(ManagedInstanceChecker.class);

  private final ManagedInstanceService managedInstanceService = mock();
  private final DefaultGroupFinder defaultGroupFinder = new DefaultGroupFinder(db.getDbClient());
  private final GroupService groupService = new GroupService(db.getDbClient(), new SequenceUuidFactory(), defaultGroupFinder, managedInstanceService);
  private final CreateAction underTest = new CreateAction(db.getDbClient(), userSession, groupService, managedInstanceChecker);
  private final WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    db.users().insertDefaultGroup();
  }
  @Test
  public void define_create_action() {
    WebService.Action action = tester.getDef();
    assertThat(action).isNotNull();
    assertThat(action.key()).isEqualTo("create");
    assertThat(action.isPost()).isTrue();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(2);
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription).containsOnly(
      tuple("10.4", "Deprecated. Use POST /api/v2/authorizations/groups instead"),
      tuple("8.4", "Field 'id' format in the response changes from integer to string."));
  }

  @Test
  public void create_group() {
    loginAsAdmin();

    tester.newRequest()
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute()
      .assertJson("{" +
                  "  \"group\": {" +
                  "    \"name\": \"some-product-bu\"," +
                  "    \"description\": \"Business Unit for Some Awesome Product\"," +
                  "    \"membersCount\": 0" +
                  "  }" +
                  "}");

    assertThat(db.users().selectGroup("some-product-bu")).isPresent();
  }

  @Test
  public void return_default_field() {
    loginAsAdmin();

    tester.newRequest()
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute()
      .assertJson("{" +
                  "  \"group\": {" +
                  "    \"name\": \"some-product-bu\"," +
                  "    \"description\": \"Business Unit for Some Awesome Product\"," +
                  "    \"membersCount\": 0," +
                  "    \"default\": false" +
                  "  }" +
                  "}");
  }

  @Test
  public void fail_if_not_administrator() {
    userSession.logIn("not-admin");

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setParam("name", "some-product-bu")
        .setParam("description", "Business Unit for Some Awesome Product")
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_name_is_too_short() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setParam("name", "")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_name_is_too_long() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setParam("name", StringUtils.repeat("a", 255 + 1))
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_name_is_anyone() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setParam("name", "AnYoNe")
        .execute();
    })
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_if_group_with_same_name_already_exists() {
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setParam("name", group.getName())
        .execute();
    })
      .isInstanceOf(ServerException.class)
      .hasMessage("Group '" + group.getName() + "' already exists");
  }

  @Test
  public void fail_if_description_is_too_long() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setParam("name", "long-desc")
        .setParam("description", StringUtils.repeat("a", 1_000))
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_instance_is_externally_managed() {
    loginAsAdmin();
    BadRequestException exception = BadRequestException.create("Not allowed");
    doThrow(exception).when(managedInstanceChecker).throwIfInstanceIsManaged();
    TestRequest testRequest = tester.newRequest();
    assertThatThrownBy(testRequest::execute)
      .isEqualTo(exception);
  }

  private void loginAsAdmin() {
    userSession.logIn().addPermission(ADMINISTER);
  }

}
