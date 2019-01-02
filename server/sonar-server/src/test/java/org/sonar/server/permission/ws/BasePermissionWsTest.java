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
package org.sonar.server.permission.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.ProjectIndexersImpl;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.permission.GroupPermissionChanger;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.permission.UserPermissionChanger;
import org.sonar.server.permission.index.FooIndexDefinition;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.usergroups.ws.GroupWsSupport;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;

public abstract class BasePermissionWsTest<A extends PermissionsWsAction> {

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());

  @Rule
  public EsTester es = EsTester.createCustom(new FooIndexDefinition());

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  protected UserSessionRule userSession = UserSessionRule.standalone();
  protected WsActionTester wsTester;

  @Before
  public void initWsTester() {
    wsTester = new WsActionTester(buildWsAction());
  }

  protected abstract A buildWsAction();

  protected GroupWsSupport newGroupWsSupport() {
    return new GroupWsSupport(db.getDbClient(), defaultOrganizationProvider, new DefaultGroupFinder(db.getDbClient()));
  }

  protected PermissionWsSupport newPermissionWsSupport() {
    DbClient dbClient = db.getDbClient();
    return new PermissionWsSupport(dbClient, new ComponentFinder(dbClient, newRootResourceTypes()), newGroupWsSupport());
  }

  protected ResourceTypesRule newRootResourceTypes() {
    return new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW, Qualifiers.APP);
  }

  protected PermissionUpdater newPermissionUpdater() {
    return new PermissionUpdater(
      new ProjectIndexersImpl(new PermissionIndexer(db.getDbClient(), es.client())),
      new UserPermissionChanger(db.getDbClient()),
      new GroupPermissionChanger(db.getDbClient()));
  }

  protected TestRequest newRequest() {
    return wsTester.newRequest().setMethod("POST");
  }

  protected void loginAsAdmin(OrganizationDto org, OrganizationDto... otherOrgs) {
    userSession.logIn().addPermission(ADMINISTER, org);
    for (OrganizationDto otherOrg : otherOrgs) {
      userSession.addPermission(ADMINISTER, otherOrg);
    }
  }

  protected PermissionTemplateDto selectTemplateInDefaultOrganization(String name) {
    return db.getDbClient().permissionTemplateDao().selectByName(db.getSession(), db.getDefaultOrganization().getUuid(), name);
  }

  protected PermissionTemplateDto addTemplate(OrganizationDto organizationDto) {
    PermissionTemplateDto dto = newPermissionTemplateDto()
      .setOrganizationUuid(organizationDto.getUuid());
    db.getDbClient().permissionTemplateDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  protected PermissionTemplateDto addTemplateToDefaultOrganization() {
    return addTemplate(db.getDefaultOrganization());
  }
}
