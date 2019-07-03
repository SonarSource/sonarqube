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

import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.ProjectIndexersImpl;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.organization.BillingValidationsProxyImpl;
import org.sonar.server.project.ProjectLifeCycleListener;
import org.sonar.server.project.ProjectLifeCycleListenersImpl;
import org.sonar.server.qualityprofile.QProfileFactoryImpl;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;

public class DeleteEmptyPersonalOrgsActionTest {

  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public final DbTester db = DbTester.create(new System2());
  private final DbClient dbClient = db.getDbClient();

  @Rule
  public final EsTester es = EsTester.create();
  private final EsClient esClient = es.client();

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  private final OrganizationDeleter organizationDeleter = new OrganizationDeleter(dbClient,
    new ComponentCleanerService(dbClient, new ResourceTypesRule(), new ProjectIndexersImpl()),
    new UserIndexer(dbClient, esClient),
    new QProfileFactoryImpl(dbClient, UuidFactoryFast.getInstance(), new System2(), new ActiveRuleIndexer(dbClient, esClient)),
    new ProjectLifeCycleListenersImpl(new ProjectLifeCycleListener[0]),
    new BillingValidationsProxyImpl());

  private final DeleteEmptyPersonalOrgsAction underTest = new DeleteEmptyPersonalOrgsAction(userSession, organizationDeleter);
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void request_fails_if_user_is_not_root() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest().execute();
  }

  @Test
  public void delete_empty_personal_orgs() {
    OrganizationDto emptyPersonal = db.organizations().insert(o -> o.setGuarded(true));
    db.users().insertUser(u -> u.setOrganizationUuid(emptyPersonal.getUuid()));

    OrganizationDto nonEmptyPersonal = db.organizations().insert(o -> o.setGuarded(true));
    db.users().insertUser(u -> u.setOrganizationUuid(nonEmptyPersonal.getUuid()));
    db.components().insertPublicProject(nonEmptyPersonal);

    OrganizationDto emptyRegular = db.organizations().insert();

    OrganizationDto nonEmptyRegular = db.organizations().insert();
    db.components().insertPublicProject(nonEmptyRegular);

    UserDto admin = db.users().insertUser();
    db.users().insertPermissionOnUser(admin, ADMINISTER);
    userSession.logIn().setSystemAdministrator();
    ws.newRequest().execute();

    List<String> notDeleted = Arrays.asList(
      db.getDefaultOrganization().getUuid(),
      nonEmptyPersonal.getUuid(),
      emptyRegular.getUuid(),
      nonEmptyRegular.getUuid());

    assertThat(dbClient.organizationDao().selectAllUuids(db.getSession()))
      .containsExactlyInAnyOrderElementsOf(notDeleted);
  }

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.handler()).isNotNull();
  }
}
