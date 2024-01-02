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

import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.RequestValidator;
import org.sonar.server.permission.ws.BasePermissionWsTest;
import org.sonar.server.permission.ws.WsParameters;
import org.sonarqube.ws.Permissions.WsGroupsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.permission.PermissionQuery.DEFAULT_PAGE_SIZE;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateGroupDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class TemplateGroupsActionTest extends BasePermissionWsTest<TemplateGroupsAction> {

  private final ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private final WsParameters wsParameters = new WsParameters(permissionService);
  private final RequestValidator requestValidator = new RequestValidator(permissionService);

  @Override
  protected TemplateGroupsAction buildWsAction() {
    return new TemplateGroupsAction(db.getDbClient(), userSession, newPermissionWsSupport(), wsParameters, requestValidator);
  }

  @Test
  public void define_template_groups() {
    WebService.Action action = wsTester.getDef();

    assertThat(action).isNotNull();
    assertThat(action.key()).isEqualTo("template_groups");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("5.2");
  }

  @Test
  public void template_groups_of_json_example() {
    GroupDto adminGroup = insertGroup("sonar-administrators", "System administrators");
    GroupDto userGroup = insertGroup("sonar-users", "Every authenticated user automatically belongs to this group");

    PermissionTemplateDto template = addTemplate();
    addGroupToTemplate(newPermissionTemplateGroup(ISSUE_ADMIN, template.getUuid(), adminGroup.getUuid()), template.getName());
    addGroupToTemplate(newPermissionTemplateGroup(ISSUE_ADMIN, template.getUuid(), userGroup.getUuid()), template.getName());
    // Anyone group
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getUuid(), null), template.getName());
    addGroupToTemplate(newPermissionTemplateGroup(ISSUE_ADMIN, template.getUuid(), null), template.getName());
    loginAsAdmin();

    String response = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .execute()
      .getInput();

    assertJson(response)
      .ignoreFields("id")
      .withStrictArrayOrder()
      .isSimilarTo(getClass().getResource("template_groups-example.json"));
  }

  @Test
  public void return_all_permissions_of_matching_groups() {
    PermissionTemplateDto template = addTemplate();

    GroupDto group1 = db.users().insertGroup("group-1-name");
    addGroupToTemplate(newPermissionTemplateGroup(CODEVIEWER, template.getUuid(), group1.getUuid()), template.getName());
    addGroupToTemplate(newPermissionTemplateGroup(ADMIN, template.getUuid(), group1.getUuid()), template.getName());

    GroupDto group2 = db.users().insertGroup("group-2-name");
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getUuid(), group2.getUuid()), template.getName());
    addGroupToTemplate(newPermissionTemplateGroup(ADMIN, template.getUuid(), group2.getUuid()), template.getName());

    GroupDto group3 = db.users().insertGroup("group-3-name");

    // Anyone
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getUuid(), null), template.getName());
    addGroupToTemplate(newPermissionTemplateGroup(ISSUE_ADMIN, template.getUuid(), null), template.getName());

    PermissionTemplateDto anotherTemplate = addTemplate();
    GroupDto group4 = db.users().insertGroup("group-4-name");
    addGroupToTemplate(newPermissionTemplateGroup(ADMIN, anotherTemplate.getUuid(), group3.getUuid()), anotherTemplate.getName());
    addGroupToTemplate(newPermissionTemplateGroup(ADMIN, anotherTemplate.getUuid(), group4.getUuid()), anotherTemplate.getName());
    loginAsAdmin();

    WsGroupsResponse response = newRequest()
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .executeProtobuf(WsGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("Anyone", "group-1-name", "group-2-name", "group-3-name", "group-4-name");
    assertThat(response.getGroups(0).getPermissionsList()).containsOnly("user", "issueadmin");
    assertThat(response.getGroups(1).getPermissionsList()).containsOnly("codeviewer", "admin");
    assertThat(response.getGroups(2).getPermissionsList()).containsOnly("user", "admin");
    assertThat(response.getGroups(3).getPermissionsList()).isEmpty();
  }

  @Test
  public void search_by_permission() {
    PermissionTemplateDto template = addTemplate();

    GroupDto group1 = db.users().insertGroup("group-1-name");
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getUuid(), group1.getUuid()), template.getName());
    addGroupToTemplate(newPermissionTemplateGroup(CODEVIEWER, template.getUuid(), group1.getUuid()), template.getName());

    GroupDto group2 = db.users().insertGroup("group-2-name");
    addGroupToTemplate(newPermissionTemplateGroup(ADMIN, template.getUuid(), group2.getUuid()), template.getName());

    GroupDto group3 = db.users().insertGroup("group-3-name");

    // Anyone
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getUuid(), null), template.getName());

    PermissionTemplateDto anotherTemplate = addTemplate();
    addGroupToTemplate(newPermissionTemplateGroup(ADMIN, anotherTemplate.getUuid(), group3.getUuid()), anotherTemplate.getName());
    loginAsAdmin();

    WsGroupsResponse response = newRequest()
      .setParam(PARAM_PERMISSION, USER)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .executeProtobuf(WsGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("Anyone", "group-1-name");
    assertThat(response.getGroups(0).getPermissionsList()).containsOnly("user");
    assertThat(response.getGroups(1).getPermissionsList()).containsOnly("user", "codeviewer");
  }

  @Test
  public void search_by_template_name() {
    GroupDto group1 = db.users().insertGroup("group-1-name");
    GroupDto group2 = db.users().insertGroup("group-2-name");
    GroupDto group3 = db.users().insertGroup("group-3-name");

    PermissionTemplateDto template = addTemplate();
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getUuid(), group1.getUuid()), template.getName());
    addGroupToTemplate(newPermissionTemplateGroup(ADMIN, template.getUuid(), group2.getUuid()), template.getName());
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getUuid(), null), template.getName());

    PermissionTemplateDto anotherTemplate = addTemplate();
    addGroupToTemplate(newPermissionTemplateGroup(USER, anotherTemplate.getUuid(), group1.getUuid()), anotherTemplate.getName());
    loginAsAdmin();

    WsGroupsResponse response = newRequest()
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .executeProtobuf(WsGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("Anyone", "group-1-name", "group-2-name", "group-3-name");
  }

  @Test
  public void search_with_pagination() {
    PermissionTemplateDto template = addTemplate();
    GroupDto group1 = db.users().insertGroup("group-1-name");
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getUuid(), group1.getUuid()), template.getName());
    GroupDto group2 = db.users().insertGroup("group-2-name");
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getUuid(), group2.getUuid()), template.getName());
    loginAsAdmin();

    WsGroupsResponse response = newRequest()
      .setParam(PARAM_PERMISSION, USER)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .setParam(PAGE, "2")
      .setParam(PAGE_SIZE, "1")
      .executeProtobuf(WsGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("group-2-name");
  }

  @Test
  public void search_with_text_query() {
    PermissionTemplateDto template = addTemplate();
    GroupDto group1 = db.users().insertGroup("group-1-name");
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getUuid(), group1.getUuid()), template.getName());
    GroupDto group2 = db.users().insertGroup("group-2-name");
    GroupDto group3 = db.users().insertGroup("group-3");
    loginAsAdmin();

    WsGroupsResponse response = newRequest()
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .setParam(TEXT_QUERY, "-nam")
      .executeProtobuf(WsGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("group-1-name", "group-2-name");
  }

  @Test
  public void search_with_text_query_return_all_groups_even_when_no_permission_set() {
    PermissionTemplateDto template = addTemplate();
    db.users().insertGroup("group-1-name");
    db.users().insertGroup("group-2-name");
    db.users().insertGroup("group-3-name");
    loginAsAdmin();

    WsGroupsResponse response = newRequest()
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .setParam(TEXT_QUERY, "-name")
      .executeProtobuf(WsGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("group-1-name", "group-2-name", "group-3-name");
    assertThat(response.getGroups(0).getPermissionsList()).isEmpty();
    assertThat(response.getGroups(1).getPermissionsList()).isEmpty();
    assertThat(response.getGroups(2).getPermissionsList()).isEmpty();
  }

  @Test
  public void search_with_text_query_return_anyone_group_even_when_no_permission_set() {
    PermissionTemplateDto template = addTemplate();
    GroupDto group = db.users().insertGroup("group");
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getUuid(), group.getUuid()), template.getName());
    loginAsAdmin();

    WsGroupsResponse response = newRequest()
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .setParam(TEXT_QUERY, "nyo")
      .executeProtobuf(WsGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("Anyone");
    assertThat(response.getGroups(0).getPermissionsList()).isEmpty();
  }

  @Test
  public void search_ignores_other_template_and_is_ordered_by_groups_with_permission_then_by_name_when_many_groups() {
    PermissionTemplateDto template = addTemplate();
    PermissionTemplateDto otherTemplate = db.permissionTemplates().insertTemplate();
    IntStream.rangeClosed(1, DEFAULT_PAGE_SIZE + 1).forEach(i -> {
      GroupDto group = db.users().insertGroup("Group-" + i);
      db.permissionTemplates().addGroupToTemplate(otherTemplate, group, UserRole.USER);
    });
    String lastGroupName = "Group-" + (DEFAULT_PAGE_SIZE + 1);
    db.permissionTemplates().addGroupToTemplate(template, db.users().selectGroup(lastGroupName).get(), UserRole.USER);
    loginAsAdmin();

    WsGroupsResponse response = newRequest()
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .executeProtobuf(WsGroupsResponse.class);

    assertThat(response.getGroupsList())
      .extracting("name")
      .hasSize(DEFAULT_PAGE_SIZE)
      .startsWith("Anyone", lastGroupName, "Group-1");
  }

  @Test
  public void fail_if_not_logged_in() {
    PermissionTemplateDto template1 = addTemplate();
    userSession.anonymous();

    assertThatThrownBy(() ->  {
      newRequest()
        .setParam(PARAM_PERMISSION, USER)
        .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
        .execute();
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_if_insufficient_privileges() {
    PermissionTemplateDto template1 = addTemplate();
    userSession.logIn();

    assertThatThrownBy(() ->  {
      newRequest()
        .setParam(PARAM_PERMISSION, USER)
        .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_template_uuid_and_name_provided() {
    PermissionTemplateDto template1 = addTemplate();
    loginAsAdmin();

    assertThatThrownBy(() ->  {
      newRequest()
        .setParam(PARAM_PERMISSION, USER)
        .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
        .setParam(PARAM_TEMPLATE_NAME, template1.getName())
        .execute();
    })
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_if_template_uuid_nor_name_provided() {
    loginAsAdmin();

    assertThatThrownBy(() ->  {
      newRequest()
        .setParam(PARAM_PERMISSION, USER)
        .execute();
    })
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_if_template_is_not_found() {
    loginAsAdmin();

    assertThatThrownBy(() ->  {
      newRequest()
        .setParam(PARAM_PERMISSION, USER)
        .setParam(PARAM_TEMPLATE_ID, "unknown-uuid")
        .execute();
    })
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_if_not_a_project_permission() {
    loginAsAdmin();
    PermissionTemplateDto template1 = addTemplate();

    assertThatThrownBy(() ->  {
      newRequest()
        .setParam(PARAM_PERMISSION, GlobalPermissions.QUALITY_GATE_ADMIN)
        .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  private GroupDto insertGroup(String name, String description) {
    return db.users().insertGroup(newGroupDto().setName(name).setDescription(description));
  }

  private void addGroupToTemplate(PermissionTemplateGroupDto permissionTemplateGroup, String templateName) {
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), permissionTemplateGroup, templateName);
    db.commit();
  }

  private static PermissionTemplateGroupDto newPermissionTemplateGroup(String permission, String templateUuid, @Nullable String groupUuid) {
    return newPermissionTemplateGroupDto()
      .setPermission(permission)
      .setTemplateUuid(templateUuid)
      .setGroupUuid(groupUuid);
  }

}
