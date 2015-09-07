/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission.ws;

import java.util.List;
import java.util.Properties;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.permission.PermissionTemplateTesting;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.platform.PersistentSettings;
import org.sonar.server.platform.ServerSettings;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.server.permission.DefaultPermissionTemplates.DEFAULT_TEMPLATE_PROPERTY;
import static org.sonar.server.permission.DefaultPermissionTemplates.defaultRootQualifierTemplateProperty;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_QUALIFIER;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_TEMPLATE_ID;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_TEMPLATE_NAME;

@Category(DbTests.class)
public class SetDefaultTemplateActionTest {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  I18nRule i18n = new I18nRule();

  WsActionTester ws;
  PersistentSettings persistentSettings;
  ResourceTypes resourceTypes = mock(ResourceTypes.class);

  PermissionTemplateDto template;

  @Before
  public void setUp() {
    DbClient dbClient = db.getDbClient();
    persistentSettings = new PersistentSettings(dbClient.propertiesDao(), new ServerSettings(new PropertyDefinitions(), new Properties()));
    persistentSettings.saveProperty(DEFAULT_TEMPLATE_PROPERTY, "any-template-uuid");
    persistentSettings.saveProperty(defaultRootQualifierTemplateProperty(PROJECT), "any-template-uuid");
    persistentSettings.saveProperty(defaultRootQualifierTemplateProperty(VIEW), "any-view-template-uuid");
    persistentSettings.saveProperty(defaultRootQualifierTemplateProperty("DEV"), "any-dev-template-uuid");
    when(resourceTypes.getRoots()).thenReturn(rootResourceTypes());
    userSession.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    ws = new WsActionTester(new SetDefaultTemplateAction(
      dbClient,
      new PermissionDependenciesFinder(dbClient, new ComponentFinder(dbClient)),
      resourceTypes,
      persistentSettings,
      userSession, i18n));

    template = dbClient.permissionTemplateDao().insert(db.getSession(), PermissionTemplateTesting.newPermissionTemplateDto().setUuid("permission-template-uuid"));
  }

  @Test
  public void update_settings_for_project_qualifier() {
    // default value is project qualifier's value
    String result = newRequest(template.getUuid(), null);

    assertThat(result).isEmpty();
    assertThat(persistentSettings.getString(DEFAULT_TEMPLATE_PROPERTY)).isEqualTo("any-template-uuid");
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty(PROJECT))).isEqualTo(template.getUuid());
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty(VIEW))).isEqualTo("any-view-template-uuid");
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty("DEV"))).isEqualTo("any-dev-template-uuid");
  }

  @Test
  public void update_settings_for_project_qualifier_by_template_name() {
    ws.newRequest()
      .setParam(PARAM_TEMPLATE_NAME, template.getName().toUpperCase())
      .execute();
    db.getSession().commit();

    assertThat(persistentSettings.getString(DEFAULT_TEMPLATE_PROPERTY)).isEqualTo("any-template-uuid");
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty(PROJECT))).isEqualTo(template.getUuid());
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty(VIEW))).isEqualTo("any-view-template-uuid");
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty("DEV"))).isEqualTo("any-dev-template-uuid");
  }

  @Test
  public void update_settings_of_views_property() {
    newRequest(template.getUuid(), VIEW);

    assertThat(persistentSettings.getString(DEFAULT_TEMPLATE_PROPERTY)).isEqualTo("any-template-uuid");
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty(PROJECT))).isEqualTo("any-template-uuid");
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty(VIEW))).isEqualTo(template.getUuid());
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty("DEV"))).isEqualTo("any-dev-template-uuid");
  }

  @Test
  public void fail_if_anonymous() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest(template.getUuid(), PROJECT);
  }

  @Test
  public void fail_if_not_admin() {
    expectedException.expect(ForbiddenException.class);
    userSession.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    newRequest(template.getUuid(), PROJECT);
  }

  @Test
  public void fail_if_template_not_provided() {
    expectedException.expect(BadRequestException.class);

    newRequest(null, PROJECT);
  }

  @Test
  public void fail_if_template_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    newRequest("unknown-template-uuid", PROJECT);
  }

  @Test
  public void fail_if_qualifier_is_not_root() {
    expectedException.expect(BadRequestException.class);
    when(resourceTypes.getRoots()).thenReturn(singletonList(ResourceType.builder(PROJECT).build()));

    newRequest(template.getUuid(), VIEW);
  }

  private String newRequest(@Nullable String templateUuid, @Nullable String qualifier) {
    TestRequest request = ws.newRequest();
    if (templateUuid != null) {
      request.setParam(PARAM_TEMPLATE_ID, templateUuid);
    }
    if (qualifier != null) {
      request.setParam(PARAM_QUALIFIER, qualifier);
    }

    return request.execute().getInput();
  }

  private static List<ResourceType> rootResourceTypes() {
    ResourceType project = ResourceType.builder(PROJECT).build();
    ResourceType view = ResourceType.builder(Qualifiers.VIEW).build();
    ResourceType dev = ResourceType.builder("DEV").build();

    return asList(project, view, dev);
  }
}
