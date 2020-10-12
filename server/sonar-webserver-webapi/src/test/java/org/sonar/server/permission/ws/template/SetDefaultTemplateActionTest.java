/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.permission.ws.template;

import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.DefaultTemplates;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateTesting;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.permission.ws.BasePermissionWsTest;
import org.sonar.server.ws.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class SetDefaultTemplateActionTest extends BasePermissionWsTest<SetDefaultTemplateAction> {

  private DbClient dbClient = db.getDbClient();
  private I18nRule i18n = new I18nRule();

  @Override
  protected SetDefaultTemplateAction buildWsAction() {
    return new SetDefaultTemplateAction(db.getDbClient(), newPermissionWsSupport(), newRootResourceTypes(), userSession, i18n, defaultOrganizationProvider);
  }

  @Test
  public void update_project_default_template() {
    PermissionTemplateDto portfolioDefaultTemplate = db.permissionTemplates().insertTemplate();
    PermissionTemplateDto applicationDefaultTemplate = db.permissionTemplates().insertTemplate();
    db.organizations().setDefaultTemplates(db.permissionTemplates().insertTemplate(),
      applicationDefaultTemplate, portfolioDefaultTemplate);
    PermissionTemplateDto template = insertTemplate();
    loginAsAdmin();

    newRequest(template.getUuid(), Qualifiers.PROJECT);

    assertDefaultTemplates(template.getUuid(), applicationDefaultTemplate.getUuid(), portfolioDefaultTemplate.getUuid());
  }

  @Test
  public void update_project_default_template_without_qualifier_param() {
    db.organizations().setDefaultTemplates("any-project-template-uuid", "any-view-template-uuid", null);
    PermissionTemplateDto template = insertTemplate();
    loginAsAdmin();

    // default value is project qualifier's value
    newRequest(template.getUuid(), null);

    assertDefaultTemplates(template.getUuid(), "any-view-template-uuid", null);
  }

  @Test
  public void update_project_default_template_by_template_name() {
    PermissionTemplateDto portfolioDefaultTemplate = db.permissionTemplates().insertTemplate();
    PermissionTemplateDto applicationDefaultTemplate = db.permissionTemplates().insertTemplate();
    db.organizations().setDefaultTemplates(db.permissionTemplates().insertTemplate(), applicationDefaultTemplate, portfolioDefaultTemplate);
    PermissionTemplateDto template = insertTemplate();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_NAME, template.getName().toUpperCase())
      .execute();
    db.getSession().commit();

    assertDefaultTemplates(template.getUuid(), applicationDefaultTemplate.getUuid(), portfolioDefaultTemplate.getUuid());
  }

  @Test
  public void update_view_default_template() {
    PermissionTemplateDto projectDefaultTemplate = db.permissionTemplates().insertTemplate();
    db.organizations().setDefaultTemplates(projectDefaultTemplate, null, null);
    PermissionTemplateDto template = insertTemplate();
    loginAsAdmin();

    newRequest(template.getUuid(), VIEW);

    assertDefaultTemplates(projectDefaultTemplate.getUuid(), null, template.getUuid());
  }

  @Test
  public void update_app_default_template() {
    PermissionTemplateDto projectDefaultTemplate = db.permissionTemplates().insertTemplate();
    db.organizations().setDefaultTemplates(projectDefaultTemplate, null, null);
    PermissionTemplateDto template = insertTemplate();
    loginAsAdmin();

    newRequest(template.getUuid(), APP);

    assertDefaultTemplates(projectDefaultTemplate.getUuid(), template.getUuid(), null);
  }

  @Test
  public void fail_if_anonymous() {
    PermissionTemplateDto template = insertTemplate();
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    newRequest(template.getUuid(), PROJECT);
  }

  @Test
  public void fail_if_not_admin() {
    PermissionTemplateDto template = insertTemplate();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest(template.getUuid(), null);
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
    PermissionTemplateDto template = insertTemplate();
    loginAsAdmin();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter 'qualifier' (FIL) must be one of: [APP, TRK, VW]");

    newRequest(template.getUuid(), Qualifiers.FILE);
  }

  @Test
  public void fail_if_no_default_templates() {
    PermissionTemplateDto template = insertTemplate();
    loginAsAdmin();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No Default templates");

    newRequest(template.getUuid(), null);
  }

  private String newRequest(@Nullable String templateUuid, @Nullable String qualifier) {
    TestRequest request = newRequest();
    if (templateUuid != null) {
      request.setParam(PARAM_TEMPLATE_ID, templateUuid);
    }
    if (qualifier != null) {
      request.setParam(PARAM_QUALIFIER, qualifier);
    }

    return request.execute().getInput();
  }

  private PermissionTemplateDto insertTemplate() {
    PermissionTemplateDto res = dbClient.permissionTemplateDao().insert(db.getSession(), PermissionTemplateTesting.newPermissionTemplateDto()
      .setUuid("permission-template-uuid"));
    db.commit();
    return res;
  }

  private void assertDefaultTemplates(@Nullable String projectDefaultTemplateUuid, @Nullable String applicationDefaultTemplateUuid, @Nullable String portfolioDefaultTemplateUuid) {
    DbSession dbSession = db.getSession();
    DefaultTemplates defaultTemplates = db.getDbClient().organizationDao().getDefaultTemplates(dbSession, db.getDefaultOrganization().getUuid())
      .orElseThrow(() -> new IllegalStateException("No default templates"));

    assertThat(defaultTemplates.getProjectUuid()).isEqualTo(projectDefaultTemplateUuid);
    assertThat(defaultTemplates.getApplicationsUuid()).isEqualTo(applicationDefaultTemplateUuid);
    assertThat(defaultTemplates.getPortfoliosUuid()).isEqualTo(portfolioDefaultTemplateUuid);
  }
}
