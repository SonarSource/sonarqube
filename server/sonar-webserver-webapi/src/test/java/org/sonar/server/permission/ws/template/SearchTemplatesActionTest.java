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
package org.sonar.server.permission.ws.template;

import java.util.Date;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.permission.DefaultTemplatesResolver;
import org.sonar.server.permission.DefaultTemplatesResolverImpl;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.ws.BasePermissionWsTest;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_10;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchTemplatesActionTest extends BasePermissionWsTest<SearchTemplatesAction> {

  private I18nRule i18n = new I18nRule();
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private ResourceTypesRule resourceTypesWithViews = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW, Qualifiers.APP);
  private ResourceTypesRule resourceTypesWithoutViews = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private PermissionService permissionServiceWithViews = new PermissionServiceImpl(resourceTypesWithViews);
  private PermissionService permissionServiceWithoutViews = new PermissionServiceImpl(resourceTypesWithoutViews);
  private DefaultTemplatesResolver defaultTemplatesResolverWithViews = new DefaultTemplatesResolverImpl(dbClient, resourceTypesWithViews);

  private WsActionTester underTestWithoutViews;

  @Override
  protected SearchTemplatesAction buildWsAction() {
    return new SearchTemplatesAction(dbClient, userSession, i18n, defaultTemplatesResolverWithViews, permissionServiceWithViews);
  }

  @Before
  public void setUp() {
    DefaultTemplatesResolver defaultTemplatesResolverWithViews = new DefaultTemplatesResolverImpl(dbClient, resourceTypesWithoutViews);
    underTestWithoutViews = new WsActionTester(
      new SearchTemplatesAction(dbClient, userSession, i18n, defaultTemplatesResolverWithViews, permissionServiceWithoutViews));
    i18n.setProjectPermissions();
    userSession.logIn().addPermission(ADMINISTER);
  }

  @Test
  public void search_project_permissions_without_views() {
    PermissionTemplateDto projectTemplate = insertProjectTemplate();

    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();

    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();

    addUserToTemplate(projectTemplate.getUuid(), user1.getUuid(), UserRole.ISSUE_ADMIN, projectTemplate.getName(), user1.getLogin());
    addUserToTemplate(projectTemplate.getUuid(), user2.getUuid(), UserRole.ISSUE_ADMIN, projectTemplate.getName(), user2.getLogin());
    addUserToTemplate(projectTemplate.getUuid(), user3.getUuid(), UserRole.ISSUE_ADMIN, projectTemplate.getName(), user3.getLogin());
    addUserToTemplate(projectTemplate.getUuid(), user1.getUuid(), UserRole.CODEVIEWER, projectTemplate.getName(), user1.getLogin());
    addGroupToTemplate(projectTemplate.getUuid(), group1.getUuid(), UserRole.ADMIN, projectTemplate.getName(), group1.getName());
    addPermissionTemplateWithProjectCreator(projectTemplate.getUuid(), UserRole.ADMIN, projectTemplate.getName());

    db.permissionTemplates().setDefaultTemplates(projectTemplate, null, null);

    String result = newRequest(underTestWithoutViews).execute().getInput();

    assertJson(result)
      .withStrictArrayOrder()
      .isSimilarTo(getClass().getResource("search_templates-example-without-views.json"));
  }

  @Test
  public void search_project_permissions_with_views() {
    PermissionTemplateDto projectTemplate = insertProjectTemplate();
    PermissionTemplateDto portfoliosTemplate = insertPortfoliosTemplate();
    PermissionTemplateDto applicationsTemplate = insertApplicationsTemplate();

    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();

    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();

    addUserToTemplate(projectTemplate.getUuid(), user1.getUuid(), UserRole.ISSUE_ADMIN, projectTemplate.getName(), user1.getLogin());
    addUserToTemplate(projectTemplate.getUuid(), user2.getUuid(), UserRole.ISSUE_ADMIN, projectTemplate.getName(), user2.getLogin());
    addUserToTemplate(projectTemplate.getUuid(), user3.getUuid(), UserRole.ISSUE_ADMIN, projectTemplate.getName(), user3.getLogin());
    addUserToTemplate(projectTemplate.getUuid(), user1.getUuid(), UserRole.CODEVIEWER, projectTemplate.getName(), user1.getLogin());
    addGroupToTemplate(projectTemplate.getUuid(), group1.getUuid(), UserRole.ADMIN, projectTemplate.getName(), group1.getName());
    addPermissionTemplateWithProjectCreator(projectTemplate.getUuid(), UserRole.ADMIN, projectTemplate.getName());

    addUserToTemplate(portfoliosTemplate.getUuid(), user1.getUuid(), UserRole.USER, portfoliosTemplate.getName(), user1.getLogin());
    addUserToTemplate(portfoliosTemplate.getUuid(), user2.getUuid(), UserRole.USER, portfoliosTemplate.getName(), user2.getLogin());
    addGroupToTemplate(portfoliosTemplate.getUuid(), group1.getUuid(), UserRole.ISSUE_ADMIN, portfoliosTemplate.getName(), group1.getName());
    addGroupToTemplate(portfoliosTemplate.getUuid(), group2.getUuid(), UserRole.ISSUE_ADMIN, portfoliosTemplate.getName(), group2.getName());
    addGroupToTemplate(portfoliosTemplate.getUuid(), group3.getUuid(), UserRole.ISSUE_ADMIN, portfoliosTemplate.getName(), group3.getName());

    db.permissionTemplates().setDefaultTemplates(projectTemplate, applicationsTemplate, portfoliosTemplate);

    String result = newRequest().execute().getInput();

    assertJson(result)
      .withStrictArrayOrder()
      .isSimilarTo(getClass().getResource("search_templates-example-with-views.json"));
  }

  @Test
  public void empty_result() {
    db.permissionTemplates().setDefaultTemplates("AU-Tpxb--iU5OvuD2FLy", "AU-Tpxb--iU5OvuD2FLz", "AU-TpxcA-iU5OvuD2FLx");
    String result = newRequest(wsTester).execute().getInput();

    assertJson(result)
      .withStrictArrayOrder()
      .ignoreFields("permissions")
      .isSimilarTo("{" +
        "  \"permissionTemplates\": []," +
        "  \"defaultTemplates\": [" +
        "    {" +
        "      \"templateId\": \"AU-Tpxb--iU5OvuD2FLy\"," +
        "      \"qualifier\": \"TRK\"" +
        "    }," +
        "    {" +
        "      \"templateId\": \"AU-Tpxb--iU5OvuD2FLz\"," +
        "      \"qualifier\": \"APP\"" +
        "    }," +
        "    {" +
        "      \"templateId\": \"AU-TpxcA-iU5OvuD2FLx\"," +
        "      \"qualifier\": \"VW\"" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void empty_result_without_views() {
    db.permissionTemplates().setDefaultTemplates("AU-Tpxb--iU5OvuD2FLy", "AU-TpxcA-iU5OvuD2FLz", "AU-TpxcA-iU5OvuD2FLx");
    String result = newRequest(underTestWithoutViews).execute().getInput();

    assertJson(result)
      .withStrictArrayOrder()
      .ignoreFields("permissions")
      .isSimilarTo("{" +
        "  \"permissionTemplates\": []," +
        "  \"defaultTemplates\": [" +
        "    {" +
        "      \"templateId\": \"AU-Tpxb--iU5OvuD2FLy\"," +
        "      \"qualifier\": \"TRK\"" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void search_by_name() {
    db.permissionTemplates().setDefaultTemplates(db.permissionTemplates().insertTemplate(), null, null);
    insertProjectTemplate();
    insertPortfoliosTemplate();

    String result = newRequest(wsTester)
      .setParam(TEXT_QUERY, "portfolio")
      .execute()
      .getInput();

    assertThat(result).contains("Default template for Portfolios")
      .doesNotContain("projects")
      .doesNotContain("developers");
  }

  @Test
  public void fail_if_not_logged_in() {
    assertThatThrownBy(() ->  {
      userSession.anonymous();
      newRequest().execute();
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void display_all_project_permissions() {
    db.permissionTemplates().setDefaultTemplates(db.permissionTemplates().insertTemplate(), null, null);

    String result = newRequest(underTestWithoutViews).execute().getInput();

    assertJson(result)
      .withStrictArrayOrder()
      .ignoreFields("defaultTemplates", "permissionTemplates")
      .isSimilarTo(
        "{" +
          "  \"permissions\": [" +
          "    {" +
          "      \"key\": \"admin\"," +
          "      \"name\": \"Administer\"," +
          "      \"description\": \"Ability to access project settings and perform administration tasks. (Users will also need \\\"Browse\\\" permission)\"" +
          "    }," +
          "    {" +
          "      \"key\": \"codeviewer\"," +
          "      \"name\": \"See Source Code\"," +
          "      \"description\": \"Ability to view the project\\u0027s source code. (Users will also need \\\"Browse\\\" permission)\"" +
          "    }," +
          "    {" +
          "      \"key\": \"issueadmin\"," +
          "      \"name\": \"Administer Issues\"," +
          "      \"description\": \"Grants the permission to perform advanced editing on issues: marking an issue False Positive / Won\\u0027t Fix or changing an Issue\\u0027s severity. (Users will also need \\\"Browse\\\" permission)\""
          +
          "    }," +
          "    {" +
          "      \"key\": \"securityhotspotadmin\"," +
          "      \"name\": \"Administer Security Hotspots\"," +
          "      \"description\": \"Detect a Vulnerability from a \\\"Security Hotspot\\\". Reject, clear, accept, reopen a \\\"Security Hotspot\\\" (users also need \\\"Browse\\\" permissions).\""
          +
          "    }," +
          "    {" +
          "      \"key\": \"scan\"," +
          "      \"name\": \"Execute Analysis\"," +
          "      \"description\": \"Ability to execute analyses, and to get all settings required to perform the analysis, even the secured ones like the scm account password, the jira account password, and so on.\""
          +
          "    }," +
          "    {" +
          "      \"key\": \"user\"," +
          "      \"name\": \"Browse\"," +
          "      \"description\": \"Ability to access a project, browse its measures, and create/edit issues for it.\"" +
          "    }" +
          "  ]" +
          "}");
  }

  @Test
  public void display_all_project_permissions_with_views() {
    db.permissionTemplates().setDefaultTemplates(db.permissionTemplates().insertTemplate(), null, null);

    String result = newRequest().execute().getInput();

    assertJson(result)
      .withStrictArrayOrder()
      .ignoreFields("defaultTemplates", "permissionTemplates")
      .isSimilarTo(
        "{" +
          "  \"permissions\": [" +
          "    {" +
          "      \"key\": \"admin\"," +
          "      \"name\": \"Administer\"," +
          "      \"description\": \"Ability to access project settings and perform administration tasks. (Users will also need \\\"Browse\\\" permission)\"" +
          "    }," +
          "    {" +
          "      \"key\": \"codeviewer\"," +
          "      \"name\": \"See Source Code\"," +
          "      \"description\": \"Ability to view the project\\u0027s source code. (Users will also need \\\"Browse\\\" permission)\"" +
          "    }," +
          "    {" +
          "      \"key\": \"issueadmin\"," +
          "      \"name\": \"Administer Issues\"," +
          "      \"description\": \"Grants the permission to perform advanced editing on issues: marking an issue False Positive / Won\\u0027t Fix or changing an Issue\\u0027s severity. (Users will also need \\\"Browse\\\" permission)\""
          +
          "    }," +
          "    {" +
          "      \"key\": \"securityhotspotadmin\"," +
          "      \"name\": \"Administer Security Hotspots\"," +
          "      \"description\": \"Detect a Vulnerability from a \\\"Security Hotspot\\\". Reject, clear, accept, reopen a \\\"Security Hotspot\\\" (users also need \\\"Browse\\\" permissions).\""
          +
          "    }," +
          "    {" +
          "      \"key\": \"scan\"," +
          "      \"name\": \"Execute Analysis\"," +
          "      \"description\": \"Ability to execute analyses, and to get all settings required to perform the analysis, even the secured ones like the scm account password, the jira account password, and so on.\""
          +
          "    }," +
          "    {" +
          "      \"key\": \"user\"," +
          "      \"name\": \"Browse\"," +
          "      \"description\": \"Ability to access a project, browse its measures, and create/edit issues for it.\"" +
          "    }" +
          "  ]" +
          "}");
  }

  private PermissionTemplateDto insertProjectTemplate() {
    return insertProjectTemplate(UUID_EXAMPLE_01);
  }

  private PermissionTemplateDto insertProjectTemplate(String uuid) {
    return insertTemplate(newPermissionTemplateDto()
      .setUuid(uuid)
      .setName("Default template for Projects")
      .setDescription("Template for new projects")
      .setKeyPattern(null)
      .setCreatedAt(new Date(1_000_000_000_000L))
      .setUpdatedAt(new Date(1_000_000_000_000L)));
  }

  private PermissionTemplateDto insertPortfoliosTemplate() {
    return insertTemplate(newPermissionTemplateDto()
      .setUuid(UUID_EXAMPLE_02)
      .setName("Default template for Portfolios")
      .setDescription("Template for new portfolios")
      .setKeyPattern(".*sonar.views.*")
      .setCreatedAt(new Date(1_000_000_000_000L))
      .setUpdatedAt(new Date(1_100_000_000_000L)));
  }

  private PermissionTemplateDto insertApplicationsTemplate() {
    return insertTemplate(newPermissionTemplateDto()
      .setUuid(UUID_EXAMPLE_10)
      .setName("Default template for Applications")
      .setDescription("Template for new applications")
      .setKeyPattern(".*sonar.views.*")
      .setCreatedAt(new Date(1_000_000_000_000L))
      .setUpdatedAt(new Date(1_100_000_000_000L)));
  }

  private PermissionTemplateDto insertTemplate(PermissionTemplateDto template) {
    PermissionTemplateDto insert = dbClient.permissionTemplateDao().insert(db.getSession(), template);
    db.getSession().commit();
    return insert;
  }

  private void addGroupToTemplate(String templateUuid, @Nullable String groupUuid, String permission, String templateName, String groupName) {
    dbClient.permissionTemplateDao().insertGroupPermission(db.getSession(), templateUuid, groupUuid, permission, templateName, groupName);
    db.getSession().commit();
  }

  private void addUserToTemplate(String templateUuid, String userId, String permission, String templateName, String userLogin) {
    dbClient.permissionTemplateDao().insertUserPermission(db.getSession(), templateUuid, userId, permission, templateName, userLogin);
    db.getSession().commit();
  }

  private void addPermissionTemplateWithProjectCreator(String templateUuid, String permission, String templateName) {
    dbClient.permissionTemplateCharacteristicDao().insert(dbSession, new PermissionTemplateCharacteristicDto()
        .setUuid(Uuids.createFast())
        .setWithProjectCreator(true)
        .setTemplateUuid(templateUuid)
        .setPermission(permission)
        .setCreatedAt(1_000_000_000L)
        .setUpdatedAt(2_000_000_000L),
      templateName);
    db.commit();
  }

  private TestRequest newRequest(WsActionTester underTest) {
    return underTest.newRequest().setMethod("POST");
  }
}
