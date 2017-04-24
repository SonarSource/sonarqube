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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.DefaultTemplates;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserMembershipDto;
import org.sonar.db.user.UserMembershipQuery;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.DefinedQProfile;
import org.sonar.server.qualityprofile.DefinedQProfileCreationRule;
import org.sonar.server.qualityprofile.DefinedQProfileRepositoryRule;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.user.index.UserQuery;
import org.sonar.server.usergroups.DefaultGroupCreator;
import org.sonar.server.usergroups.DefaultGroupCreatorImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.organization.OrganizationCreation.NewOrganization.newOrganizationBuilder;

public class OrganizationCreationImplTest {
  private static final String SOME_UUID = "org-uuid";
  private static final long SOME_DATE = 12893434L;
  private static final String A_LOGIN = "a-login";
  private static final String SLUG_OF_A_LOGIN = "slug-of-a-login";
  private static final String STRING_64_CHARS = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  private static final String A_NAME = "a name";
  private static final int ANYONE_GROUP_ID = 0;

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
  public EsTester es = new EsTester(new UserIndexDefinition(new MapSettings()));
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DefinedQProfileRepositoryRule definedQProfileRepositoryRule = new DefinedQProfileRepositoryRule();
  @Rule
  public DefinedQProfileCreationRule definedQProfileCreationRule = new DefinedQProfileCreationRule();

  private DbSession dbSession = dbTester.getSession();

  private IllegalArgumentException exceptionThrownByOrganizationValidation = new IllegalArgumentException("simulate IAE thrown by OrganizationValidation");
  private DbClient dbClient = dbTester.getDbClient();
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private OrganizationValidation organizationValidation = mock(OrganizationValidation.class);
  private MapSettings settings = new MapSettings();
  private UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private UserIndex userIndex = new UserIndex(es.client());
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private DefaultGroupCreator defaultGroupCreator = new DefaultGroupCreatorImpl(dbClient);

  private OrganizationCreationImpl underTest = new OrganizationCreationImpl(dbClient, system2, uuidFactory, organizationValidation, settings, userIndexer,
    definedQProfileRepositoryRule, definedQProfileCreationRule, defaultGroupCreator, activeRuleIndexer);

  private UserDto someUser;

  @Before
  public void setUp() {
    someUser = dbTester.users().insertUser();
    userIndexer.index(someUser.getLogin());
  }

  @Test
  public void create_throws_NPE_if_NewOrganization_arg_is_null() throws OrganizationCreation.KeyConflictException {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("newOrganization can't be null");

    underTest.create(dbSession, someUser, null);
  }

  @Test
  public void create_throws_exception_thrown_by_checkValidKey() throws OrganizationCreation.KeyConflictException {
    when(organizationValidation.checkKey(FULL_POPULATED_NEW_ORGANIZATION.getKey()))
      .thenThrow(exceptionThrownByOrganizationValidation);

    createThrowsExceptionThrownByOrganizationValidation();
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

  private void createThrowsExceptionThrownByOrganizationValidation() throws OrganizationCreation.KeyConflictException {
    try {
      underTest.create(dbSession, someUser, FULL_POPULATED_NEW_ORGANIZATION);
      fail(exceptionThrownByOrganizationValidation + " should have been thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e).isSameAs(exceptionThrownByOrganizationValidation);
    }
  }

  @Test
  public void create_fails_with_ISE_if_DefinedQProfileRepository_has_not_been_initialized() throws OrganizationCreation.KeyConflictException {
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("initialize must be called first");

    underTest.create(dbSession, someUser, FULL_POPULATED_NEW_ORGANIZATION);
  }

  @Test
  public void create_fails_with_KeyConflictException_if_org_with_key_in_NewOrganization_arg_already_exists_in_db() throws OrganizationCreation.KeyConflictException {
    dbTester.organizations().insertForKey(FULL_POPULATED_NEW_ORGANIZATION.getKey());

    expectedException.expect(OrganizationCreation.KeyConflictException.class);
    expectedException.expectMessage("Organization key '" + FULL_POPULATED_NEW_ORGANIZATION.getKey() + "' is already used");

    underTest.create(dbSession, someUser, FULL_POPULATED_NEW_ORGANIZATION);
  }

  @Test
  public void create_creates_unguarded_organization_with_properties_from_NewOrganization_arg() throws OrganizationCreation.KeyConflictException {
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    definedQProfileRepositoryRule.initialize();

    underTest.create(dbSession, someUser, FULL_POPULATED_NEW_ORGANIZATION);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, FULL_POPULATED_NEW_ORGANIZATION.getKey()).get();
    assertThat(organization.getUuid()).isEqualTo(SOME_UUID);
    assertThat(organization.getKey()).isEqualTo(FULL_POPULATED_NEW_ORGANIZATION.getKey());
    assertThat(organization.getName()).isEqualTo(FULL_POPULATED_NEW_ORGANIZATION.getName());
    assertThat(organization.getDescription()).isEqualTo(FULL_POPULATED_NEW_ORGANIZATION.getDescription());
    assertThat(organization.getUrl()).isEqualTo(FULL_POPULATED_NEW_ORGANIZATION.getUrl());
    assertThat(organization.getAvatarUrl()).isEqualTo(FULL_POPULATED_NEW_ORGANIZATION.getAvatar());
    assertThat(organization.isGuarded()).isFalse();
    assertThat(organization.getUserId()).isNull();
    assertThat(organization.getCreatedAt()).isEqualTo(SOME_DATE);
    assertThat(organization.getUpdatedAt()).isEqualTo(SOME_DATE);
  }

  @Test
  public void create_creates_owners_group_with_all_permissions_for_new_organization_and_add_current_user_to_it() throws OrganizationCreation.KeyConflictException {
    UserDto user = dbTester.users().insertUser();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    definedQProfileRepositoryRule.initialize();

    underTest.create(dbSession, user, FULL_POPULATED_NEW_ORGANIZATION);

    verifyGroupOwners(user, FULL_POPULATED_NEW_ORGANIZATION.getKey(), FULL_POPULATED_NEW_ORGANIZATION.getName());
  }

  @Test
  public void create_creates_members_group_and_add_current_user_to_it() throws OrganizationCreation.KeyConflictException {
    UserDto user = dbTester.users().insertUser();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    definedQProfileRepositoryRule.initialize();

    underTest.create(dbSession, user, FULL_POPULATED_NEW_ORGANIZATION);

    verifyMembersGroup(user, FULL_POPULATED_NEW_ORGANIZATION.getKey());
  }

  @Test
  public void create_does_not_require_description_url_and_avatar_to_be_non_null() throws OrganizationCreation.KeyConflictException {
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    definedQProfileRepositoryRule.initialize();

    underTest.create(dbSession, someUser, newOrganizationBuilder()
      .setKey("key")
      .setName("name")
      .build());

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, "key").get();
    assertThat(organization.getKey()).isEqualTo("key");
    assertThat(organization.getName()).isEqualTo("name");
    assertThat(organization.getDescription()).isNull();
    assertThat(organization.getUrl()).isNull();
    assertThat(organization.getAvatarUrl()).isNull();
    assertThat(organization.isGuarded()).isFalse();
    assertThat(organization.getUserId()).isNull();
  }

  @Test
  public void create_creates_default_template_for_new_organization() throws OrganizationCreation.KeyConflictException {
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    definedQProfileRepositoryRule.initialize();

    underTest.create(dbSession, someUser, FULL_POPULATED_NEW_ORGANIZATION);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, FULL_POPULATED_NEW_ORGANIZATION.getKey()).get();
    GroupDto ownersGroup = dbClient.groupDao().selectByName(dbSession, organization.getUuid(), "Owners").get();
    int defaultGroupId = dbClient.organizationDao().getDefaultGroupId(dbSession, organization.getUuid()).get();
    PermissionTemplateDto defaultTemplate = dbClient.permissionTemplateDao().selectByName(dbSession, organization.getUuid(), "default template");
    assertThat(defaultTemplate.getName()).isEqualTo("Default template");
    assertThat(defaultTemplate.getDescription()).isEqualTo("Default permission template of organization " + FULL_POPULATED_NEW_ORGANIZATION.getName());
    DefaultTemplates defaultTemplates = dbClient.organizationDao().getDefaultTemplates(dbSession, organization.getUuid()).get();
    assertThat(defaultTemplates.getProjectUuid()).isEqualTo(defaultTemplate.getUuid());
    assertThat(defaultTemplates.getViewUuid()).isNull();
    assertThat(dbClient.permissionTemplateDao().selectGroupPermissionsByTemplateId(dbSession, defaultTemplate.getId()))
      .extracting(PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple(ownersGroup.getId(), UserRole.ADMIN), tuple(ownersGroup.getId(), UserRole.ISSUE_ADMIN), tuple(ownersGroup.getId(), GlobalPermissions.SCAN_EXECUTION),
        tuple(defaultGroupId, UserRole.USER), tuple(defaultGroupId, UserRole.CODEVIEWER));
  }

  @Test
  public void create_add_current_user_as_member_of_organization() throws OrganizationCreation.KeyConflictException {
    UserDto user = dbTester.users().insertUser();
    userIndexer.index(user.getLogin());

    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    definedQProfileRepositoryRule.initialize();
    userIndexer.index(someUser.getLogin());

    underTest.create(dbSession, someUser, FULL_POPULATED_NEW_ORGANIZATION);

    assertThat(dbClient.organizationMemberDao().select(dbSession, SOME_UUID, someUser.getId())).isPresent();
    assertThat(userIndex.search(UserQuery.builder().setOrganizationUuid(SOME_UUID).setTextQuery(someUser.getLogin()).build(), new SearchOptions()).getTotal()).isEqualTo(1L);
  }

  @Test
  public void create_creates_QualityProfile_for_each_DefinedQProfile_in_repository_and_index_ActiveRule_changes_in_order() throws OrganizationCreation.KeyConflictException {
    DefinedQProfile definedQProfile1 = definedQProfileRepositoryRule.add(LanguageTesting.newLanguage("foo"), "qp1");
    DefinedQProfile definedQProfile2 = definedQProfileRepositoryRule.add(LanguageTesting.newLanguage("foo"), "qp2");
    DefinedQProfile definedQProfile3 = definedQProfileRepositoryRule.add(LanguageTesting.newLanguage("foo"), "qp3");
    DefinedQProfile definedQProfile4 = definedQProfileRepositoryRule.add(LanguageTesting.newLanguage("foo"), "qp4");
    definedQProfileRepositoryRule.initialize();
    ActiveRuleChange[] changes = {newActiveRuleChange("0"), newActiveRuleChange("1"), newActiveRuleChange("2"), newActiveRuleChange("3"), newActiveRuleChange("4")};
    definedQProfileCreationRule.addChanges();
    definedQProfileCreationRule.addChanges(changes[2], changes[1], changes[4]);
    definedQProfileCreationRule.addChanges(changes[3]);
    definedQProfileCreationRule.addChanges(changes[0]);
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    underTest.create(dbSession, someUser, FULL_POPULATED_NEW_ORGANIZATION);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, FULL_POPULATED_NEW_ORGANIZATION.getKey()).get();
    assertThat(definedQProfileCreationRule.getCallLogs())
      .hasSize(4)
      .extracting(DefinedQProfileCreationRule.CallLog::getOrganizationDto)
      .extracting(OrganizationDto::getUuid)
      .containsOnly(organization.getUuid());
    assertThat(definedQProfileCreationRule.getCallLogs())
      .extracting(DefinedQProfileCreationRule.CallLog::getDefinedQProfile)
      .extracting(DefinedQProfile::getName)
      .containsExactly(definedQProfile1.getName(), definedQProfile2.getName(), definedQProfile3.getName(), definedQProfile4.getName());
    verify(activeRuleIndexer).index(Arrays.asList(changes[2], changes[1], changes[4], changes[3], changes[0]));
    verifyNoMoreInteractions(activeRuleIndexer);
  }

  @Test
  public void createForUser_has_no_effect_if_setting_for_feature_is_not_set() {
    checkSizeOfTables();

    underTest.createForUser(null /* argument is not even read */, null /* argument is not even read */);

    checkSizeOfTables();
  }

  @Test
  public void createForUser_has_no_effect_if_setting_for_feature_is_disabled() {
    enableCreatePersonalOrg(false);

    checkSizeOfTables();

    underTest.createForUser(null /* argument is not even read */, null /* argument is not even read */);

    checkSizeOfTables();
  }

  private void checkSizeOfTables() {
    assertThat(dbTester.countRowsOfTable("organizations")).isEqualTo(1);
    assertThat(dbTester.countRowsOfTable("groups")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("groups_users")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("permission_templates")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("perm_templates_users")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("perm_templates_groups")).isEqualTo(0);
  }

  @Test
  public void createForUser_creates_guarded_organization_with_key_name_and_description_generated_from_user_login_and_name_and_associated_to_user() {
    UserDto user = dbTester.users().insertUser(A_LOGIN);
    when(organizationValidation.generateKeyFrom(A_LOGIN)).thenReturn(SLUG_OF_A_LOGIN);
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    enableCreatePersonalOrg(true);
    definedQProfileRepositoryRule.initialize();

    underTest.createForUser(dbSession, user);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, SLUG_OF_A_LOGIN).get();
    assertThat(organization.getUuid()).isEqualTo(SOME_UUID);
    assertThat(organization.getKey()).isEqualTo(SLUG_OF_A_LOGIN);
    assertThat(organization.getName()).isEqualTo(user.getName());
    assertThat(organization.getDescription()).isEqualTo(user.getName() + "'s personal organization");
    assertThat(organization.getUrl()).isNull();
    assertThat(organization.getAvatarUrl()).isNull();
    assertThat(organization.isGuarded()).isTrue();
    assertThat(organization.getUserId()).isEqualTo(user.getId());
    assertThat(organization.getCreatedAt()).isEqualTo(SOME_DATE);
    assertThat(organization.getUpdatedAt()).isEqualTo(SOME_DATE);
  }

  @Test
  public void createForUser_fails_with_ISE_if_organization_with_slug_of_login_already_exists() {
    UserDto user = dbTester.users().insertUser(A_LOGIN);
    when(organizationValidation.generateKeyFrom(A_LOGIN)).thenReturn(SLUG_OF_A_LOGIN);
    dbTester.organizations().insertForKey(SLUG_OF_A_LOGIN);
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    enableCreatePersonalOrg(true);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't create organization with key '" + SLUG_OF_A_LOGIN + "' for new user '" + A_LOGIN
      + "' because an organization with this key already exists");

    underTest.createForUser(dbSession, user);
  }

  @Test
  public void createForUser_gives_all_permissions_for_new_organization_to_current_user() throws OrganizationCreation.KeyConflictException {
    UserDto user = dbTester.users().insertUser(dto -> dto.setLogin(A_LOGIN).setName(A_NAME));
    when(organizationValidation.generateKeyFrom(A_LOGIN)).thenReturn(SLUG_OF_A_LOGIN);
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    enableCreatePersonalOrg(true);
    definedQProfileRepositoryRule.initialize();

    underTest.createForUser(dbSession, user);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, SLUG_OF_A_LOGIN).get();
    assertThat(dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getId(), organization.getUuid()))
      .containsOnly(GlobalPermissions.ALL.toArray(new String[GlobalPermissions.ALL.size()]));
  }

  @Test
  public void createForUser_creates_members_group_and_add_current_user_to_it() throws OrganizationCreation.KeyConflictException {
    UserDto user = dbTester.users().insertUser(dto -> dto.setLogin(A_LOGIN).setName(A_NAME));
    when(organizationValidation.generateKeyFrom(A_LOGIN)).thenReturn(SLUG_OF_A_LOGIN);
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    enableCreatePersonalOrg(true);
    definedQProfileRepositoryRule.initialize();

    underTest.createForUser(dbSession, user);

    verifyMembersGroup(user, SLUG_OF_A_LOGIN);
  }

  @Test
  public void createForUser_creates_default_template_for_new_organization() throws OrganizationCreation.KeyConflictException {
    UserDto user = dbTester.users().insertUser(dto -> dto.setLogin(A_LOGIN).setName(A_NAME));
    when(organizationValidation.generateKeyFrom(A_LOGIN)).thenReturn(SLUG_OF_A_LOGIN);
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    enableCreatePersonalOrg(true);
    definedQProfileRepositoryRule.initialize();

    underTest.createForUser(dbSession, user);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, SLUG_OF_A_LOGIN).get();
    int defaultGroupId = dbClient.organizationDao().getDefaultGroupId(dbSession, organization.getUuid()).get();
    PermissionTemplateDto defaultTemplate = dbClient.permissionTemplateDao().selectByName(dbSession, organization.getUuid(), "default template");
    assertThat(defaultTemplate.getName()).isEqualTo("Default template");
    assertThat(defaultTemplate.getDescription()).isEqualTo("Default permission template of organization " + A_NAME);
    DefaultTemplates defaultTemplates = dbClient.organizationDao().getDefaultTemplates(dbSession, organization.getUuid()).get();
    assertThat(defaultTemplates.getProjectUuid()).isEqualTo(defaultTemplate.getUuid());
    assertThat(defaultTemplates.getViewUuid()).isNull();
    assertThat(dbClient.permissionTemplateDao().selectGroupPermissionsByTemplateId(dbSession, defaultTemplate.getId()))
      .extracting(PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple(defaultGroupId, UserRole.USER), tuple(defaultGroupId, UserRole.CODEVIEWER));
    assertThat(dbClient.permissionTemplateCharacteristicDao().selectByTemplateIds(dbSession, Collections.singletonList(defaultTemplate.getId())))
      .extracting(PermissionTemplateCharacteristicDto::getWithProjectCreator, PermissionTemplateCharacteristicDto::getPermission)
      .containsOnly(
        tuple(true, UserRole.ADMIN), tuple(true, UserRole.ISSUE_ADMIN), tuple(true, GlobalPermissions.SCAN_EXECUTION));
  }

  @Test
  public void createForUser_add_current_user_as_member_of_organization() throws OrganizationCreation.KeyConflictException {
    UserDto user = dbTester.users().insertUser(dto -> dto.setLogin(A_LOGIN).setName(A_NAME));
    when(organizationValidation.generateKeyFrom(A_LOGIN)).thenReturn(SLUG_OF_A_LOGIN);
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    enableCreatePersonalOrg(true);
    definedQProfileRepositoryRule.initialize();

    underTest.createForUser(dbSession, user);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, SLUG_OF_A_LOGIN).get();
    assertThat(dbClient.organizationMemberDao().select(dbSession, organization.getUuid(), user.getId())).isPresent();
  }

  @Test
  public void createForUser_does_not_fail_if_name_is_too_long_for_an_organization_name() {
    String nameTooLong = STRING_64_CHARS + "b";
    UserDto user = dbTester.users().insertUser(dto -> dto.setName(nameTooLong).setLogin(A_LOGIN));
    when(organizationValidation.generateKeyFrom(A_LOGIN)).thenReturn(SLUG_OF_A_LOGIN);
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    enableCreatePersonalOrg(true);
    definedQProfileRepositoryRule.initialize();

    underTest.createForUser(dbSession, user);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, SLUG_OF_A_LOGIN).get();
    assertThat(organization.getName()).isEqualTo(STRING_64_CHARS);
    assertThat(organization.getDescription()).isEqualTo(nameTooLong + "'s personal organization");
  }

  @Test
  public void createForUser_does_not_fail_if_name_is_empty_and_login_is_too_long_for_an_organization_name() {
    String login = STRING_64_CHARS + "b";
    UserDto user = dbTester.users().insertUser(dto -> dto.setName("").setLogin(login));
    when(organizationValidation.generateKeyFrom(login)).thenReturn(SLUG_OF_A_LOGIN);
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    enableCreatePersonalOrg(true);
    definedQProfileRepositoryRule.initialize();

    underTest.createForUser(dbSession, user);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, SLUG_OF_A_LOGIN).get();
    assertThat(organization.getName()).isEqualTo(STRING_64_CHARS);
    assertThat(organization.getDescription()).isEqualTo(login + "'s personal organization");
  }

  @Test
  public void createForUser_does_not_fail_if_name_is_null_and_login_is_too_long_for_an_organization_name() {
    String login = STRING_64_CHARS + "b";
    UserDto user = dbTester.users().insertUser(dto -> dto.setName(null).setLogin(login));
    when(organizationValidation.generateKeyFrom(login)).thenReturn(SLUG_OF_A_LOGIN);
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    enableCreatePersonalOrg(true);
    definedQProfileRepositoryRule.initialize();

    underTest.createForUser(dbSession, user);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, SLUG_OF_A_LOGIN).get();
    assertThat(organization.getName()).isEqualTo(STRING_64_CHARS);
    assertThat(organization.getDescription()).isEqualTo(login + "'s personal organization");
  }

  @Test
  public void createForUser_creates_QualityProfile_for_each_DefinedQProfile_in_repository_and_index_ActiveRule_changes_in_order() throws OrganizationCreation.KeyConflictException {
    UserDto user = dbTester.users().insertUser(A_LOGIN);
    when(organizationValidation.generateKeyFrom(A_LOGIN)).thenReturn(SLUG_OF_A_LOGIN);
    DefinedQProfile definedQProfile1 = definedQProfileRepositoryRule.add(LanguageTesting.newLanguage("foo"), "qp1");
    DefinedQProfile definedQProfile2 = definedQProfileRepositoryRule.add(LanguageTesting.newLanguage("foo"), "qp2");
    DefinedQProfile definedQProfile3 = definedQProfileRepositoryRule.add(LanguageTesting.newLanguage("foo"), "qp3");
    DefinedQProfile definedQProfile4 = definedQProfileRepositoryRule.add(LanguageTesting.newLanguage("foo"), "qp4");
    definedQProfileRepositoryRule.initialize();
    ActiveRuleChange[] changes = {newActiveRuleChange("0"), newActiveRuleChange("1"), newActiveRuleChange("2"), newActiveRuleChange("3"), newActiveRuleChange("4")};
    definedQProfileCreationRule.addChanges();
    definedQProfileCreationRule.addChanges(changes[2], changes[1], changes[4]);
    definedQProfileCreationRule.addChanges(changes[3]);
    definedQProfileCreationRule.addChanges(changes[0]);
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    enableCreatePersonalOrg(true);

    underTest.createForUser(dbSession, user);

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, SLUG_OF_A_LOGIN).get();
    assertThat(definedQProfileCreationRule.getCallLogs())
      .hasSize(4)
      .extracting(DefinedQProfileCreationRule.CallLog::getOrganizationDto)
      .extracting(OrganizationDto::getUuid)
      .containsOnly(organization.getUuid());
    assertThat(definedQProfileCreationRule.getCallLogs())
      .extracting(DefinedQProfileCreationRule.CallLog::getDefinedQProfile)
      .extracting(DefinedQProfile::getName)
      .containsExactly(definedQProfile1.getName(), definedQProfile2.getName(), definedQProfile3.getName(), definedQProfile4.getName());
    verify(activeRuleIndexer).index(Arrays.asList(changes[2], changes[1], changes[4], changes[3], changes[0]));
    verifyNoMoreInteractions(activeRuleIndexer);
  }

  private static ActiveRuleChange newActiveRuleChange(String id) {
    return ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(id, RuleKey.of(id + "1", id + "2")));
  }

  private void enableCreatePersonalOrg(boolean flag) {
    settings.setProperty(CorePropertyDefinitions.ORGANIZATIONS_CREATE_PERSONAL_ORG, flag);
  }

  private void mockForSuccessfulInsert(String orgUuid, long orgDate) {
    when(uuidFactory.create()).thenReturn(orgUuid);
    when(system2.now()).thenReturn(orgDate);
  }

  private void verifyGroupOwners(UserDto user, String organizationKey, String organizationName) {
    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, organizationKey).get();
    Optional<GroupDto> groupOpt = dbClient.groupDao().selectByName(dbSession, organization.getUuid(), "Owners");
    assertThat(groupOpt).isPresent();
    GroupDto groupDto = groupOpt.get();
    assertThat(groupDto.getDescription()).isEqualTo("Owners of organization " + organizationName);

    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, groupDto.getOrganizationUuid(), groupDto.getId()))
      .containsOnly(GlobalPermissions.ALL.toArray(new String[GlobalPermissions.ALL.size()]));
    List<UserMembershipDto> members = dbClient.groupMembershipDao().selectMembers(
      dbSession,
      UserMembershipQuery.builder()
        .organizationUuid(organization.getUuid())
        .groupId(groupDto.getId())
        .membership(UserMembershipQuery.IN).build(),
      0, Integer.MAX_VALUE);
    assertThat(members)
      .extracting(UserMembershipDto::getLogin)
      .containsOnly(user.getLogin());
  }

  private void verifyMembersGroup(UserDto user, String organizationKey) {
    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, organizationKey).get();
    Optional<GroupDto> groupOpt = dbClient.groupDao().selectByName(dbSession, organization.getUuid(), "Members");
    assertThat(groupOpt).isPresent();
    GroupDto groupDto = groupOpt.get();
    assertThat(groupDto.getDescription()).isEqualTo("All members of the organization");

    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, groupDto.getOrganizationUuid(), groupDto.getId())).isEmpty();
    List<UserMembershipDto> members = dbClient.groupMembershipDao().selectMembers(
      dbSession,
      UserMembershipQuery.builder()
        .organizationUuid(organization.getUuid())
        .groupId(groupDto.getId())
        .membership(UserMembershipQuery.IN).build(),
      0, Integer.MAX_VALUE);
    assertThat(members)
      .extracting(UserMembershipDto::getLogin)
      .containsOnly(user.getLogin());
  }

}
