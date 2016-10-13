/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateTesting;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.permission.GroupPermissionChanger;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.permission.UserPermissionChanger;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.ws.GroupWsSupport;
import org.sonar.server.ws.WsTester;

import static org.mockito.Mockito.mock;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;

public abstract class BasePermissionWsTest<A extends PermissionsWsAction> {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  protected UserSessionRule userSession = UserSessionRule.standalone();
  protected WsTester wsTester;

  @Before
  public void initWsTester() {
    wsTester = new WsTester(new PermissionsWs(buildWsAction()));
  }

  protected abstract A buildWsAction();

  protected GroupWsSupport newGroupWsSupport() {
    return new GroupWsSupport(db.getDbClient(), defaultOrganizationProvider);
  }

  protected PermissionWsSupport newPermissionWsSupport() {
    DbClient dbClient = db.getDbClient();
    return new PermissionWsSupport(dbClient, new ComponentFinder(dbClient), newGroupWsSupport(), newRootResourceTypes());
  }

  protected ResourceTypesRule newRootResourceTypes() {
    return new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW, "DEV");
  }

  protected PermissionUpdater newPermissionUpdater() {
    return new PermissionUpdater(db.getDbClient(),
      mock(IssueAuthorizationIndexer.class),
      new UserPermissionChanger(db.getDbClient()),
      new GroupPermissionChanger(db.getDbClient()));
  }

  protected PermissionTemplateDto insertTemplate() {
    PermissionTemplateDto dto = PermissionTemplateTesting.newPermissionTemplateDto()
      .setOrganizationUuid(db.getDefaultOrganization().getUuid());
    dto = db.getDbClient().permissionTemplateDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  protected void loginAsAdminOnDefaultOrganization() {
    loginAsAdmin(db.getDefaultOrganization());
  }

  protected void loginAsAdmin(OrganizationDto org) {
    userSession.login().addOrganizationPermission(org.getUuid(), SYSTEM_ADMIN);
  }

  protected PermissionTemplateDto selectTemplateInDefaultOrganization(String name) {
    return db.getDbClient().permissionTemplateDao().selectByName(db.getSession(), db.getDefaultOrganization().getUuid(), name);
  }

  protected PermissionTemplateDto addTemplateToDefaultOrganization() {
    PermissionTemplateDto dto = newPermissionTemplateDto()
      .setOrganizationUuid(db.getDefaultOrganization().getUuid());
    db.getDbClient().permissionTemplateDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }
}
