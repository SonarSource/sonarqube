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
package org.sonar.server.organization;

import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.DefaultTemplates;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserMembershipDto;
import org.sonar.db.user.UserMembershipQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.organization.OrganizationCreation.NewOrganization.newOrganizationBuilder;

public class OrganizationCreationImplTest {
  private static final long SOME_USER_ID = 1L;
  private static final String SOME_UUID = "org-uuid";
  private static final long SOME_DATE = 12893434L;
  private OrganizationCreation.NewOrganization FULL_POPULATED_NEW_ORGANIZATION = newOrganizationBuilder()
    .setName("a-name")
    .setKey("a-key")
    .setDescription("a-description")
    .setUrl("a-url")
    .setAvatarUrl("a-avatar")
    .build();

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbSession dbSession = dbTester.getSession();

  private IllegalArgumentException exceptionThrownByOrganizationValidation = new IllegalArgumentException("simulate IAE thrown by OrganizationValidation");
  private DbClient dbClient = dbTester.getDbClient();
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private OrganizationValidation organizationValidation = mock(OrganizationValidation.class);

  private OrganizationCreationImpl underTest = new OrganizationCreationImpl(dbClient, system2, uuidFactory, organizationValidation);

  @Test
  public void create_throws_NPE_if_NewOrganization_arg_is_null() throws OrganizationCreation.KeyConflictException {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("newOrganization can't be null");

    underTest.create(dbSession, SOME_USER_ID, null);
  }

  @Test
  public void create_throws_exception_thrown_by_checkValidKey() throws OrganizationCreation.KeyConflictException {
    when(organizationValidation.checkKey(FULL_POPULATED_NEW_ORGANIZATION.getKey()))
      .thenThrow(exceptionThrownByOrganizationValidation);

    createThrowsExceptionThrownByOrganizationValidation();
  }

  private void createThrowsExceptionThrownByOrganizationValidation() throws OrganizationCreation.KeyConflictException {
    try {
      underTest.create(dbSession, SOME_USER_ID, FULL_POPULATED_NEW_ORGANIZATION);
      fail(exceptionThrownByOrganizationValidation + " should have been thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e).isSameAs(exceptionThrownByOrganizationValidation);
    }
  }

  @Test
  public void create_throws_exception_thrown_by_checkValidDescription() throws OrganizationCreation.KeyConflictException {
    when(organizationValidation.checkDescription(FULL_POPULATED_NEW_ORGANIZATION.getDescription())).thenThrow(exceptionThrownByOrganizationValidation);

    createThrowsExceptionThrownByOrganizationValidation();
  }

  @Test
  public void create_throws_exception_thrown_by_checkValidUrl() throws OrganizationCreation.KeyConflictException {
    when(organizationValidation.checkUrl(FULL_POPULATED_NEW_ORGANIZATION.getUrl())).thenThrow(exceptionThrownByOrganizationValidation);

    createThrowsExceptionThrownByOrganizationValidation();
  }

  @Test
  public void create_throws_exception_thrown_by_checkValidAvatar() throws OrganizationCreation.KeyConflictException {
    when(organizationValidation.checkAvatar(FULL_POPULATED_NEW_ORGANIZATION.getAvatar())).thenThrow(exceptionThrownByOrganizationValidation);

    createThrowsExceptionThrownByOrganizationValidation();
  }

  @Test
  public void create_fails_with_KeyConflictException_if_org_with_key_in_NewOrganization_arg_already_exists_in_db() throws OrganizationCreation.KeyConflictException {
    dbTester.organizations().insertForKey(FULL_POPULATED_NEW_ORGANIZATION.getKey());

    expectedException.expect(OrganizationCreation.KeyConflictException.class);
    expectedException.expectMessage("Organization key '" + FULL_POPULATED_NEW_ORGANIZATION.getKey() + "' is already used");

    underTest.create(dbSession, SOME_USER_ID, FULL_POPULATED_NEW_ORGANIZATION);
  }

  @Test
  public void create_creates_unguarded_organization_with_properties_from_NewOrganization_arg() throws OrganizationCreation.KeyConflictException {
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    underTest.create(dbSession, SOME_USER_ID, FULL_POPULATED_NEW_ORGANIZATION);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, FULL_POPULATED_NEW_ORGANIZATION.getKey()).get();
    assertThat(organization.getUuid()).isEqualTo(SOME_UUID);
    assertThat(organization.getKey()).isEqualTo(FULL_POPULATED_NEW_ORGANIZATION.getKey());
    assertThat(organization.getName()).isEqualTo(FULL_POPULATED_NEW_ORGANIZATION.getName());
    assertThat(organization.getDescription()).isEqualTo(FULL_POPULATED_NEW_ORGANIZATION.getDescription());
    assertThat(organization.getUrl()).isEqualTo(FULL_POPULATED_NEW_ORGANIZATION.getUrl());
    assertThat(organization.getAvatarUrl()).isEqualTo(FULL_POPULATED_NEW_ORGANIZATION.getAvatar());
    assertThat(organization.getCreatedAt()).isEqualTo(SOME_DATE);
    assertThat(organization.getUpdatedAt()).isEqualTo(SOME_DATE);
  }

  @Test
  public void create_creates_owners_group_with_all_permissions_for_new_organization_and_add_current_user_to_it() throws OrganizationCreation.KeyConflictException {
    UserDto user = dbTester.users().insertUser();

    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    underTest.create(dbSession, user.getId(), FULL_POPULATED_NEW_ORGANIZATION);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, FULL_POPULATED_NEW_ORGANIZATION.getKey()).get();
    Optional<GroupDto> groupDtoOptional = dbClient.groupDao().selectByName(dbSession, organization.getUuid(), "Owners");
    assertThat(groupDtoOptional).isNotEmpty();
    GroupDto groupDto = groupDtoOptional.get();
    assertThat(groupDto.getDescription()).isEqualTo("Owners of organization " + FULL_POPULATED_NEW_ORGANIZATION.getName());
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, groupDto.getOrganizationUuid(), groupDto.getId()))
      .containsOnly(GlobalPermissions.ALL.toArray(new String[GlobalPermissions.ALL.size()]));
    List<UserMembershipDto> members = dbClient.groupMembershipDao().selectMembers(
      dbSession,
      UserMembershipQuery.builder().groupId(groupDto.getId()).membership(UserMembershipQuery.IN).build(), 0, Integer.MAX_VALUE);
    assertThat(members)
      .extracting(UserMembershipDto::getLogin)
      .containsOnly(user.getLogin());
  }

  @Test
  public void create_does_not_require_description_url_and_avatar_to_be_non_null() throws OrganizationCreation.KeyConflictException {
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    underTest.create(dbSession, SOME_USER_ID, newOrganizationBuilder()
      .setKey("key")
      .setName("name")
      .build());

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, "key").get();
    assertThat(organization.getKey()).isEqualTo("key");
    assertThat(organization.getName()).isEqualTo("name");
    assertThat(organization.getDescription()).isNull();
    assertThat(organization.getUrl()).isNull();
    assertThat(organization.getAvatarUrl()).isNull();
  }

  @Test
  public void create_creates_default_template_for_new_organization() throws OrganizationCreation.KeyConflictException {
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    underTest.create(dbSession, SOME_USER_ID, FULL_POPULATED_NEW_ORGANIZATION);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, FULL_POPULATED_NEW_ORGANIZATION.getKey()).get();
    GroupDto ownersGroup = dbClient.groupDao().selectByName(dbSession, organization.getUuid(), "Owners").get();
    PermissionTemplateDto defaultTemplate = dbClient.permissionTemplateDao().selectByName(dbSession, organization.getUuid(), "default template");
    assertThat(defaultTemplate.getName()).isEqualTo("Default template");
    assertThat(defaultTemplate.getDescription()).isEqualTo("Default permission template of organization " + FULL_POPULATED_NEW_ORGANIZATION.getName());
    DefaultTemplates defaultTemplates = dbClient.organizationDao().getDefaultTemplates(dbSession, organization.getUuid()).get();
    assertThat(defaultTemplates.getProjectUuid()).isEqualTo(defaultTemplate.getUuid());
    assertThat(defaultTemplates.getViewUuid()).isNull();
    assertThat(dbClient.permissionTemplateDao().selectGroupPermissionsByTemplateId(dbSession, defaultTemplate.getId()))
      .extracting(PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple(ownersGroup.getId(), UserRole.ADMIN), tuple(ownersGroup.getId(), UserRole.ISSUE_ADMIN),
        tuple(0L, UserRole.USER), tuple(0L, UserRole.CODEVIEWER));
  }

  private void mockForSuccessfulInsert(String orgUuid, long orgDate) {
    when(uuidFactory.create()).thenReturn(orgUuid);
    when(system2.now()).thenReturn(orgDate);
  }
}
