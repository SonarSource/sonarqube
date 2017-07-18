/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.permission;

import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.WsPermissions;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_DESCRIPTION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY_PATTERN;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class PermissionsServiceTest {
  private static final String ORGANIZATION_VALUE = "organization value";
  private static final String PERMISSION_VALUE = "permission value";
  private static final String PROJECT_ID_VALUE = "project id value";
  private static final String PROJECT_KEY_VALUE = "project key value";
  private static final String QUERY_VALUE = "query value";
  private static final int PAGE_VALUE = 66;
  private static final int PAGE_SIZE_VALUE = 99;
  private static final String GROUP_ID_VALUE = "group id value";
  private static final String GROUP_NAME_VALUE = "group name value";
  private static final String TEMPLATE_ID_VALUE = "template id value";
  private static final String TEMPLATE_NAME_VALUE = "template name value";
  private static final String LOGIN_VALUE = "login value";
  private static final String NAME_VALUE = "name value";
  private static final String DESCRIPTION_VALUE = "description value";
  private static final String PROJECT_KEY_PATTERN_VALUE = "project key pattern value";
  private static final String QUALIFIER_VALUE = "qualifier value";
  private static final String PARAM_Q = "q";
  private static final String PARAM_PS = "ps";
  private static final String PARAM_P = "p";

  @Rule
  public ServiceTester<PermissionsService> serviceTester = new ServiceTester<>(new PermissionsService(mock(WsConnector.class)));

  private PermissionsService underTest = serviceTester.getInstanceUnderTest();

  @Test(expected = NullPointerException.class)
  public void groups_throws_NPE_if_GroupWsRequest_argument_is_null() {
    underTest.groups(null);
  }

  @Test
  public void groups_does_POST_on_WS_groups() {
    GroupsWsRequest request = new GroupsWsRequest();
    underTest.groups(request
      .setPermission(PERMISSION_VALUE)
      .setProjectId(PROJECT_ID_VALUE)
      .setProjectKey(PROJECT_KEY_VALUE)
      .setPage(PAGE_VALUE)
      .setPageSize(PAGE_SIZE_VALUE)
      .setQuery(QUERY_VALUE));

    assertThat(serviceTester.getGetParser()).isSameAs(WsPermissions.WsGroupsResponse.parser());
    GetRequest getRequest = serviceTester.getGetRequest();
    serviceTester.assertThat(getRequest)
      .hasPath("groups")
      .hasParam(PARAM_PERMISSION, PERMISSION_VALUE)
      .hasParam(PARAM_PROJECT_ID, PROJECT_ID_VALUE)
      .hasParam(PARAM_PROJECT_KEY, PROJECT_KEY_VALUE)
      .hasParam(PARAM_P, PAGE_VALUE)
      .hasParam(PARAM_PS, PAGE_SIZE_VALUE)
      .hasParam(PARAM_Q, QUERY_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void addGroup_does_POST_on_Ws_add_group() {
    underTest.addGroup(new AddGroupWsRequest()
      .setOrganization(ORGANIZATION_VALUE)
      .setPermission(PERMISSION_VALUE)
      .setProjectId(PROJECT_ID_VALUE)
      .setProjectKey(PROJECT_KEY_VALUE)
      .setGroupId(GROUP_ID_VALUE)
      .setGroupName(GROUP_NAME_VALUE));

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("add_group")
      .hasParam(PARAM_ORGANIZATION, ORGANIZATION_VALUE)
      .hasParam(PARAM_PERMISSION, PERMISSION_VALUE)
      .hasParam(PARAM_PROJECT_ID, PROJECT_ID_VALUE)
      .hasParam(PARAM_PROJECT_KEY, PROJECT_KEY_VALUE)
      .hasParam(PARAM_GROUP_ID, GROUP_ID_VALUE)
      .hasParam(PARAM_GROUP_NAME, GROUP_NAME_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void addGroupToTemplate_does_POST_on_Ws_add_group_to_template() {
    underTest.addGroupToTemplate(
      new AddGroupToTemplateWsRequest()
        .setGroupId(GROUP_ID_VALUE)
        .setGroupName(GROUP_NAME_VALUE)
        .setPermission(PERMISSION_VALUE)
        .setTemplateId(TEMPLATE_ID_VALUE)
        .setTemplateName(TEMPLATE_NAME_VALUE)
    );

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("add_group_to_template")
      .hasParam(PARAM_GROUP_ID, GROUP_ID_VALUE)
      .hasParam(PARAM_GROUP_NAME, GROUP_NAME_VALUE)
      .hasParam(PARAM_PERMISSION, PERMISSION_VALUE)
      .hasParam(PARAM_TEMPLATE_ID, TEMPLATE_ID_VALUE)
      .hasParam(PARAM_TEMPLATE_NAME, TEMPLATE_NAME_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void addUser_does_POST_on_Ws_add_user() {
    underTest.addUser(new AddUserWsRequest()
      .setLogin(LOGIN_VALUE)
      .setOrganization(ORGANIZATION_VALUE)
      .setPermission(PERMISSION_VALUE)
      .setProjectId(PROJECT_ID_VALUE)
      .setProjectKey(PROJECT_KEY_VALUE)
    );

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("add_user")
      .hasParam(PARAM_USER_LOGIN, LOGIN_VALUE)
      .hasParam(PARAM_ORGANIZATION, ORGANIZATION_VALUE)
      .hasParam(PARAM_PERMISSION, PERMISSION_VALUE)
      .hasParam(PARAM_PROJECT_ID, PROJECT_ID_VALUE)
      .hasParam(PARAM_PROJECT_KEY, PROJECT_KEY_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void addUserToTemplate_does_POST_on_Ws_add_user_to_template() {
    underTest.addUserToTemplate(new AddUserToTemplateWsRequest()
      .setOrganization(ORGANIZATION_VALUE)
      .setPermission(PERMISSION_VALUE)
      .setLogin(LOGIN_VALUE)
      .setTemplateId(TEMPLATE_ID_VALUE)
      .setTemplateName(TEMPLATE_NAME_VALUE)
    );

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("add_user_to_template")
      .hasParam(PARAM_ORGANIZATION, ORGANIZATION_VALUE)
      .hasParam(PARAM_PERMISSION, PERMISSION_VALUE)
      .hasParam(PARAM_USER_LOGIN, LOGIN_VALUE)
      .hasParam(PARAM_TEMPLATE_ID, TEMPLATE_ID_VALUE)
      .hasParam(PARAM_TEMPLATE_NAME, TEMPLATE_NAME_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void applyTemplate_does_POST_on_Ws_apply_template() {
    underTest.applyTemplate(new ApplyTemplateWsRequest()
      .setOrganization(ORGANIZATION_VALUE)
      .setProjectId(PROJECT_ID_VALUE)
      .setProjectKey(PROJECT_KEY_VALUE)
      .setTemplateId(TEMPLATE_ID_VALUE)
      .setTemplateName(TEMPLATE_NAME_VALUE)
    );

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("apply_template")
      .hasParam(PARAM_ORGANIZATION, ORGANIZATION_VALUE)
      .hasParam(PARAM_PROJECT_ID, PROJECT_ID_VALUE)
      .hasParam(PARAM_PROJECT_KEY, PROJECT_KEY_VALUE)
      .hasParam(PARAM_TEMPLATE_ID, TEMPLATE_ID_VALUE)
      .hasParam(PARAM_TEMPLATE_NAME, TEMPLATE_NAME_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void bulk_apply_template() {
    underTest.bulkApplyTemplate(new BulkApplyTemplateWsRequest()
      .setOrganization(ORGANIZATION_VALUE)
      .setTemplateId(TEMPLATE_ID_VALUE)
      .setTemplateName(TEMPLATE_NAME_VALUE)
      .setQualifier(QUALIFIER_VALUE)
      .setQuery(QUERY_VALUE));

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("bulk_apply_template")
      .hasParam(PARAM_ORGANIZATION, ORGANIZATION_VALUE)
      .hasParam(PARAM_TEMPLATE_ID, TEMPLATE_ID_VALUE)
      .hasParam(PARAM_TEMPLATE_NAME, TEMPLATE_NAME_VALUE)
      .hasParam("q", QUERY_VALUE)
      .hasParam(PARAM_QUALIFIER, QUALIFIER_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void createTemplate_does_POST_on_Ws_create_template() {
    underTest.createTemplate(new CreateTemplateWsRequest()
      .setOrganization(ORGANIZATION_VALUE)
      .setName(NAME_VALUE)
      .setDescription(DESCRIPTION_VALUE)
      .setProjectKeyPattern(PROJECT_KEY_PATTERN_VALUE)
    );

    assertThat(serviceTester.getPostParser()).isSameAs(WsPermissions.CreateTemplateWsResponse.parser());
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("create_template")
      .hasParam(PARAM_ORGANIZATION, ORGANIZATION_VALUE)
      .hasParam(PARAM_NAME, NAME_VALUE)
      .hasParam(PARAM_DESCRIPTION, DESCRIPTION_VALUE)
      .hasParam(PARAM_PROJECT_KEY_PATTERN, PROJECT_KEY_PATTERN_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void deleteTemplate_does_POST_on_Ws_delete_template() {
    underTest.deleteTemplate(new DeleteTemplateWsRequest()
      .setTemplateId(TEMPLATE_ID_VALUE)
      .setTemplateName(TEMPLATE_NAME_VALUE)
      .setOrganization(ORGANIZATION_VALUE)
    );

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("delete_template")
      .hasParam(PARAM_TEMPLATE_ID, TEMPLATE_ID_VALUE)
      .hasParam(PARAM_TEMPLATE_NAME, TEMPLATE_NAME_VALUE)
      .hasParam(PARAM_ORGANIZATION, ORGANIZATION_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void removeGroup_does_POST_on_Ws_remove_group() {
    underTest.removeGroup(new RemoveGroupWsRequest()
      .setPermission(PERMISSION_VALUE)
      .setGroupId(GROUP_ID_VALUE)
      .setGroupName(GROUP_NAME_VALUE)
      .setProjectId(PROJECT_ID_VALUE)
      .setProjectKey(PROJECT_KEY_VALUE)
      .setOrganization(ORGANIZATION_VALUE)
    );

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("remove_group")
      .hasParam(PARAM_PERMISSION, PERMISSION_VALUE)
      .hasParam(PARAM_GROUP_ID, GROUP_ID_VALUE)
      .hasParam(PARAM_GROUP_NAME, GROUP_NAME_VALUE)
      .hasParam(PARAM_PROJECT_ID, PROJECT_ID_VALUE)
      .hasParam(PARAM_PROJECT_KEY, PROJECT_KEY_VALUE)
      .hasParam(PARAM_ORGANIZATION, ORGANIZATION_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void removeGroupFromTemplate_does_POST_on_Ws_remove_group_from_template() {
    underTest.removeGroupFromTemplate(new RemoveGroupFromTemplateWsRequest()
      .setPermission(PERMISSION_VALUE)
      .setGroupId(GROUP_ID_VALUE)
      .setGroupName(GROUP_NAME_VALUE)
      .setTemplateId(TEMPLATE_ID_VALUE)
      .setTemplateName(TEMPLATE_NAME_VALUE)
      .setOrganization(ORGANIZATION_VALUE)
    );

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("remove_group_from_template")
      .hasParam(PARAM_PERMISSION, PERMISSION_VALUE)
      .hasParam(PARAM_GROUP_ID, GROUP_ID_VALUE)
      .hasParam(PARAM_GROUP_NAME, GROUP_NAME_VALUE)
      .hasParam(PARAM_TEMPLATE_ID, TEMPLATE_ID_VALUE)
      .hasParam(PARAM_TEMPLATE_NAME, TEMPLATE_NAME_VALUE)
      .hasParam(PARAM_ORGANIZATION, ORGANIZATION_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void removeUser_does_POST_on_Ws_remove_user() {
    underTest.removeUser(new RemoveUserWsRequest()
      .setPermission(PERMISSION_VALUE)
      .setLogin(LOGIN_VALUE)
      .setProjectId(PROJECT_ID_VALUE)
      .setProjectKey(PROJECT_KEY_VALUE)
    );

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("remove_user")
      .hasParam(PARAM_PERMISSION, PERMISSION_VALUE)
      .hasParam(PARAM_USER_LOGIN, LOGIN_VALUE)
      .hasParam(PARAM_PROJECT_ID, PROJECT_ID_VALUE)
      .hasParam(PARAM_PROJECT_KEY, PROJECT_KEY_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void removeUserFromTemplate_does_POST_on_Ws_remove_user_from_template() {
    underTest.removeUserFromTemplate(new RemoveUserFromTemplateWsRequest()
      .setPermission(PERMISSION_VALUE)
      .setLogin(LOGIN_VALUE)
      .setTemplateId(TEMPLATE_ID_VALUE)
      .setTemplateName(TEMPLATE_NAME_VALUE)
      .setOrganization(ORGANIZATION_VALUE)
    );

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("remove_user_from_template")
      .hasParam(PARAM_PERMISSION, PERMISSION_VALUE)
      .hasParam(PARAM_USER_LOGIN, LOGIN_VALUE)
      .hasParam(PARAM_TEMPLATE_ID, TEMPLATE_ID_VALUE)
      .hasParam(PARAM_TEMPLATE_NAME, TEMPLATE_NAME_VALUE)
      .hasParam(PARAM_ORGANIZATION, ORGANIZATION_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void searchGlobalPermissions_does_GET_on_Ws_search_global_permissions() {
    underTest.searchGlobalPermissions();

    assertThat(serviceTester.getGetParser()).isSameAs(WsPermissions.WsSearchGlobalPermissionsResponse.parser());
    GetRequest getRequest = serviceTester.getGetRequest();
    serviceTester.assertThat(getRequest)
      .hasPath("search_global_permissions")
      .andNoOtherParam();
  }

  @Test
  public void searchProjectPermissions_does_GET_on_Ws_search_project_permissions() {
    underTest.searchProjectPermissions(new SearchProjectPermissionsWsRequest()
      .setProjectId(PROJECT_ID_VALUE)
      .setProjectKey(PROJECT_KEY_VALUE)
      .setQualifier(QUALIFIER_VALUE)
      .setPage(PAGE_VALUE)
      .setPageSize(PAGE_SIZE_VALUE)
      .setQuery(QUERY_VALUE)
    );

    assertThat(serviceTester.getGetParser()).isSameAs(WsPermissions.SearchProjectPermissionsWsResponse.parser());
    GetRequest getRequest = serviceTester.getGetRequest();
    serviceTester.assertThat(getRequest)
      .hasPath("search_project_permissions")
      .hasParam(PARAM_PROJECT_ID, PROJECT_ID_VALUE)
      .hasParam(PARAM_PROJECT_KEY, PROJECT_KEY_VALUE)
      .hasParam(PARAM_QUALIFIER, QUALIFIER_VALUE)
      .hasParam(PARAM_P, PAGE_VALUE)
      .hasParam(PARAM_PS, PAGE_SIZE_VALUE)
      .hasParam(PARAM_Q, QUERY_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void searchTemplates_does_GET_on_Ws_search_templates() {
    underTest.searchTemplates(new SearchTemplatesWsRequest()
      .setQuery(QUERY_VALUE)
    );

    assertThat(serviceTester.getGetParser()).isSameAs(WsPermissions.SearchTemplatesWsResponse.parser());
    GetRequest getRequest = serviceTester.getGetRequest();
    serviceTester.assertThat(getRequest)
      .hasPath("search_templates")
      .hasParam(PARAM_Q, QUERY_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void setDefaultTemplate_does_POST_on_Ws_set_default_template() {
    underTest.setDefaultTemplate(new SetDefaultTemplateWsRequest()
      .setQualifier(QUALIFIER_VALUE)
      .setTemplateId(TEMPLATE_ID_VALUE)
      .setTemplateName(TEMPLATE_NAME_VALUE)
    );

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("set_default_template")
      .hasParam(PARAM_QUALIFIER, QUALIFIER_VALUE)
      .hasParam(PARAM_TEMPLATE_ID, TEMPLATE_ID_VALUE)
      .hasParam(PARAM_TEMPLATE_NAME, TEMPLATE_NAME_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void updateTemplate_does_POST_on_Ws_update_template() {
    underTest.updateTemplate(new UpdateTemplateWsRequest()
      .setDescription(DESCRIPTION_VALUE)
      .setId(TEMPLATE_ID_VALUE)
      .setName(TEMPLATE_NAME_VALUE)
      .setProjectKeyPattern(PROJECT_KEY_PATTERN_VALUE)
    );

    assertThat(serviceTester.getPostParser()).isSameAs(WsPermissions.UpdateTemplateWsResponse.parser());
    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("update_template")
      .hasParam(PARAM_DESCRIPTION, DESCRIPTION_VALUE)
      .hasParam(PARAM_ID, TEMPLATE_ID_VALUE)
      .hasParam(PARAM_NAME, TEMPLATE_NAME_VALUE)
      .hasParam(PARAM_PROJECT_KEY_PATTERN, PROJECT_KEY_PATTERN_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void add_project_creator_to_template() {
    underTest.addProjectCreatorToTemplate(AddProjectCreatorToTemplateWsRequest.builder()
      .setPermission(PERMISSION_VALUE)
      .setTemplateId(TEMPLATE_ID_VALUE)
      .setTemplateName(TEMPLATE_NAME_VALUE)
      .setOrganization(ORGANIZATION_VALUE)
      .build());

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest getRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(getRequest)
      .hasPath("add_project_creator_to_template")
      .hasParam(PARAM_PERMISSION, PERMISSION_VALUE)
      .hasParam(PARAM_TEMPLATE_ID, TEMPLATE_ID_VALUE)
      .hasParam(PARAM_TEMPLATE_NAME, TEMPLATE_NAME_VALUE)
      .hasParam(PARAM_ORGANIZATION, ORGANIZATION_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void remove_project_creator_from_template() {
    underTest.removeProjectCreatorFromTemplate(RemoveProjectCreatorFromTemplateWsRequest.builder()
      .setPermission(PERMISSION_VALUE)
      .setTemplateId(TEMPLATE_ID_VALUE)
      .setTemplateName(TEMPLATE_NAME_VALUE)
      .setOrganization(ORGANIZATION_VALUE)
      .build());

    assertThat(serviceTester.getPostParser()).isNull();
    PostRequest getRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(getRequest)
      .hasPath("remove_project_creator_from_template")
      .hasParam(PARAM_PERMISSION, PERMISSION_VALUE)
      .hasParam(PARAM_TEMPLATE_ID, TEMPLATE_ID_VALUE)
      .hasParam(PARAM_TEMPLATE_NAME, TEMPLATE_NAME_VALUE)
      .hasParam(PARAM_ORGANIZATION, ORGANIZATION_VALUE)
      .andNoOtherParam();
  }
}
