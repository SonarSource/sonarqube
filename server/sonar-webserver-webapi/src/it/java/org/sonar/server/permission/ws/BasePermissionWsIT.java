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
package org.sonar.server.permission.ws;

import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.IndexersImpl;
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

import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;

public abstract class BasePermissionWsIT<A extends PermissionsWsAction> {

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());

  @Rule
  public EsTester es = EsTester.createCustom(new FooIndexDefinition());

  protected UserSessionRule userSession = UserSessionRule.standalone();
  protected WsActionTester wsTester;
  protected Configuration configuration = mock(Configuration.class);

  @Before
  public void initWsTester() {
    wsTester = new WsActionTester(buildWsAction());
  }

  protected abstract A buildWsAction();

  protected GroupWsSupport newGroupWsSupport() {
    return new GroupWsSupport(db.getDbClient(), new DefaultGroupFinder(db.getDbClient()));
  }

  protected PermissionWsSupport newPermissionWsSupport() {
    DbClient dbClient = db.getDbClient();
    return new PermissionWsSupport(dbClient, configuration, newGroupWsSupport());
  }

  protected ResourceTypesRule newRootResourceTypes() {
    return new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW, Qualifiers.APP);
  }

  protected PermissionUpdater newPermissionUpdater() {
    return new PermissionUpdater(
      new IndexersImpl(new PermissionIndexer(db.getDbClient(), es.client())),
      Set.of(new UserPermissionChanger(db.getDbClient(), new SequenceUuidFactory()),
        new GroupPermissionChanger(db.getDbClient(), new SequenceUuidFactory())));
  }

  protected TestRequest newRequest() {
    return wsTester.newRequest().setMethod("POST");
  }

  protected void loginAsAdmin() {
    userSession.logIn().addPermission(ADMINISTER).setSystemAdministrator();
  }

  protected PermissionTemplateDto selectPermissionTemplate(String name) {
    return db.getDbClient().permissionTemplateDao().selectByName(db.getSession(), name);
  }

  protected PermissionTemplateDto addTemplate() {
    PermissionTemplateDto dto = newPermissionTemplateDto();
    db.getDbClient().permissionTemplateDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }
}
