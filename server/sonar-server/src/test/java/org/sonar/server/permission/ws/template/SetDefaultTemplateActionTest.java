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
package org.sonar.server.permission.ws.template;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateTesting;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.permission.ws.BasePermissionWsTest;
import org.sonar.server.platform.PersistentSettings;
import org.sonar.server.platform.SettingsChangeNotifier;
import org.sonar.server.ws.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.server.permission.DefaultPermissionTemplates.DEFAULT_TEMPLATE_PROPERTY;
import static org.sonar.server.permission.DefaultPermissionTemplates.defaultRootQualifierTemplateProperty;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class SetDefaultTemplateActionTest extends BasePermissionWsTest<SetDefaultTemplateAction> {

  private I18nRule i18n = new I18nRule();
  private PersistentSettings persistentSettings = new PersistentSettings(new MapSettings(), db.getDbClient(), new SettingsChangeNotifier());
  private PermissionTemplateDto template;

  @Override
  protected SetDefaultTemplateAction buildWsAction() {
    return new SetDefaultTemplateAction(db.getDbClient(), newPermissionWsSupport(), newRootResourceTypes(), persistentSettings, userSession, i18n);
  }

  @Before
  public void setUp() {
    DbClient dbClient = db.getDbClient();
    persistentSettings.saveProperty(DEFAULT_TEMPLATE_PROPERTY, "any-template-uuid");
    persistentSettings.saveProperty(defaultRootQualifierTemplateProperty(PROJECT), "any-template-uuid");
    persistentSettings.saveProperty(defaultRootQualifierTemplateProperty(VIEW), "any-view-template-uuid");
    persistentSettings.saveProperty(defaultRootQualifierTemplateProperty("DEV"), "any-dev-template-uuid");
    loginAsAdminOnDefaultOrganization();

    template = dbClient.permissionTemplateDao().insert(db.getSession(), PermissionTemplateTesting.newPermissionTemplateDto()
      .setOrganizationUuid(db.getDefaultOrganization().getUuid())
      .setUuid("permission-template-uuid"));
    db.commit();
  }

  @Test
  public void update_settings_for_project_qualifier() throws Exception {
    // default value is project qualifier's value
    String result = newRequest(template.getUuid(), null);

    assertThat(result).isEmpty();
    assertThat(persistentSettings.getString(DEFAULT_TEMPLATE_PROPERTY)).isEqualTo("any-template-uuid");
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty(PROJECT))).isEqualTo(template.getUuid());
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty(VIEW))).isEqualTo("any-view-template-uuid");
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty("DEV"))).isEqualTo("any-dev-template-uuid");
  }

  @Test
  public void update_settings_for_project_qualifier_by_template_name() throws Exception {
    newRequest()
      .setParam(PARAM_TEMPLATE_NAME, template.getName().toUpperCase())
      .execute();
    db.getSession().commit();

    assertThat(persistentSettings.getString(DEFAULT_TEMPLATE_PROPERTY)).isEqualTo("any-template-uuid");
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty(PROJECT))).isEqualTo(template.getUuid());
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty(VIEW))).isEqualTo("any-view-template-uuid");
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty("DEV"))).isEqualTo("any-dev-template-uuid");
  }

  @Test
  public void update_settings_of_views_property() throws Exception {
    newRequest(template.getUuid(), VIEW);

    assertThat(persistentSettings.getString(DEFAULT_TEMPLATE_PROPERTY)).isEqualTo("any-template-uuid");
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty(PROJECT))).isEqualTo("any-template-uuid");
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty(VIEW))).isEqualTo(template.getUuid());
    assertThat(persistentSettings.getString(defaultRootQualifierTemplateProperty("DEV"))).isEqualTo("any-dev-template-uuid");
  }

  @Test
  public void fail_if_anonymous() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest(template.getUuid(), PROJECT);
  }

  @Test
  public void fail_if_not_admin() throws Exception {
    expectedException.expect(ForbiddenException.class);
    userSession.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    newRequest(template.getUuid(), PROJECT);
  }

  @Test
  public void fail_if_template_not_provided() throws Exception {
    expectedException.expect(BadRequestException.class);

    newRequest(null, PROJECT);
  }

  @Test
  public void fail_if_template_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);

    newRequest("unknown-template-uuid", PROJECT);
  }

  @Test
  public void fail_if_qualifier_is_not_root() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter 'qualifier' (FIL) must be one of: [DEV, TRK, VW]");

    newRequest(template.getUuid(), Qualifiers.FILE);
  }

  private String newRequest(@Nullable String templateUuid, @Nullable String qualifier) throws Exception {
    TestRequest request = newRequest();
    if (templateUuid != null) {
      request.setParam(PARAM_TEMPLATE_ID, templateUuid);
    }
    if (qualifier != null) {
      request.setParam(PARAM_QUALIFIER, qualifier);
    }

    return request.execute().getInput();
  }
}
