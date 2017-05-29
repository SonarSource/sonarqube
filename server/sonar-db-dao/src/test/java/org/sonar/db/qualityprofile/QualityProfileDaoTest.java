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
package org.sonar.db.qualityprofile;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UtcDateUtils;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newQualityProfileDto;

public class QualityProfileDaoTest {

  private System2 system = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system);

  private DbSession dbSession = dbTester.getSession();
  private QualityProfileDbTester qualityProfileDb = new QualityProfileDbTester(dbTester);
  private QualityProfileDao underTest = dbTester.getDbClient().qualityProfileDao();
  private OrganizationDto organization;

  @Before
  public void before() {
    when(system.now()).thenReturn(UtcDateUtils.parseDateTime("2014-01-20T12:00:00+0000").getTime());
    organization = dbTester.organizations().insertForUuid("QualityProfileDaoTest-ORG");
  }

  @After
  public void tearDown() {
    // minor optimization, no need to commit pending operations
    dbSession.rollback();
  }

  @Test
  public void insert() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    RulesProfileDto dto = RulesProfileDto.createFor("abcde")
      .setOrganizationUuid(organization.getUuid())
      .setName("ABCDE")
      .setLanguage("xoo")
      .setIsBuiltIn(true);

    underTest.insert(dbTester.getSession(), dto);
    dbTester.commit();

    dbTester.assertDbUnit(getClass(), "insert-result.xml", new String[] {"created_at", "updated_at", "rules_updated_at"}, "rules_profiles");
  }

  @Test
  public void update() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    RulesProfileDto dto = RulesProfileDto.createFor("key")
      .setId(1)
      .setOrganizationUuid(organization.getUuid())
      .setName("New Name")
      .setLanguage("js")
      .setParentKee("fghij")
      .setIsBuiltIn(false);

    underTest.update(dbSession, dto);
    dbSession.commit();

    dbTester.assertDbUnit(getClass(), "update-result.xml", new String[] {"created_at", "updated_at", "rules_updated_at"}, "rules_profiles");
  }

  @Test
  public void test_deleteByKeys() {
    RulesProfileDto p1 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());
    RulesProfileDto p2 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());
    RulesProfileDto p3 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());

    underTest.deleteByKeys(dbSession, asList(p1.getKee(), p3.getKee(), "does_not_exist"));

    List<Map<String, Object>> keysInDb = dbTester.select(dbSession, "select kee as \"key\" from rules_profiles");
    assertThat(keysInDb).hasSize(1);
    assertThat(keysInDb.get(0).get("key")).isEqualTo(p2.getKee());
  }

  @Test
  public void deleteByKeys_does_nothing_if_empty_keys() {
    RulesProfileDto p1 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());

    underTest.deleteByKeys(dbSession, Collections.emptyList());

    assertThat(dbTester.countRowsOfTable(dbSession, "rules_profiles")).isEqualTo(1);
  }

  @Test
  public void deleteProjectAssociationsByProfileKeys_does_nothing_if_empty_keys() {
    RulesProfileDto profile1 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());
    ComponentDto project1 = dbTester.components().insertPrivateProject();
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project1, profile1);

    underTest.deleteProjectAssociationsByProfileKeys(dbSession, Collections.emptyList());

    assertThat(dbTester.countRowsOfTable(dbSession, "project_qprofiles")).isEqualTo(1);
  }

  @Test
  public void deleteProjectAssociationsByProfileKeys_deletes_rows_from_table_project_profiles() {
    RulesProfileDto profile1 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());
    RulesProfileDto profile2 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());
    ComponentDto project1 = dbTester.components().insertPrivateProject();
    ComponentDto project2 = dbTester.components().insertPrivateProject();
    ComponentDto project3 = dbTester.components().insertPrivateProject();
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project1, profile1);
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project2, profile1);
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project3, profile2);

    underTest.deleteProjectAssociationsByProfileKeys(dbSession, asList(profile1.getKee(), "does_not_exist"));

    List<Map<String, Object>> rows = dbTester.select(dbSession, "select project_uuid as \"projectUuid\", profile_key as \"profileKey\" from project_qprofiles");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("projectUuid")).isEqualTo(project3.uuid());
    assertThat(rows.get(0).get("profileKey")).isEqualTo(profile2.getKee());
  }

  @Test
  public void find_all() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<RulesProfileDto> dtos = underTest.selectAll(dbTester.getSession(), organization);

    assertThat(dtos).hasSize(2);

    RulesProfileDto dto1 = dtos.get(0);
    assertThat(dto1.getId()).isEqualTo(1);
    assertThat(dto1.getName()).isEqualTo("Sonar Way");
    assertThat(dto1.getLanguage()).isEqualTo("java");
    assertThat(dto1.getParentKee()).isNull();
    assertThat(dto1.isBuiltIn()).isTrue();

    RulesProfileDto dto2 = dtos.get(1);
    assertThat(dto2.getId()).isEqualTo(2);
    assertThat(dto2.getName()).isEqualTo("Sonar Way");
    assertThat(dto2.getLanguage()).isEqualTo("js");
    assertThat(dto2.getParentKee()).isNull();
    assertThat(dto2.isBuiltIn()).isFalse();
  }

  @Test
  public void find_all_is_sorted_by_profile_name() {
    dbTester.prepareDbUnit(getClass(), "select_all_is_sorted_by_profile_name.xml");

    List<RulesProfileDto> dtos = underTest.selectAll(dbTester.getSession(), organization);

    assertThat(dtos).hasSize(3);
    assertThat(dtos.get(0).getName()).isEqualTo("First");
    assertThat(dtos.get(1).getName()).isEqualTo("Second");
    assertThat(dtos.get(2).getName()).isEqualTo("Third");
  }

  @Test
  public void get_default_profile() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    RulesProfileDto java = underTest.selectDefaultProfile(dbTester.getSession(), organization, "java");
    assertThat(java).isNotNull();
    assertThat(java.getKee()).isEqualTo("java_sonar_way");

    assertThat(underTest.selectDefaultProfile(dbTester.getSession(), dbTester.organizations().insert(), "java")).isNull();
    assertThat(underTest.selectDefaultProfile(dbTester.getSession(), organization, "js")).isNull();
  }

  @Test
  public void get_default_profiles() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<RulesProfileDto> java = underTest.selectDefaultProfiles(dbTester.getSession(), organization, singletonList("java"));
    assertThat(java).extracting(RulesProfileDto::getKee).containsOnly("java_sonar_way");

    assertThat(underTest.selectDefaultProfiles(dbTester.getSession(), organization, singletonList("js"))).isEmpty();
    assertThat(underTest.selectDefaultProfiles(dbTester.getSession(), organization, of("java", "js"))).extracting(RulesProfileDto::getKee).containsOnly("java_sonar_way");
    assertThat(underTest.selectDefaultProfiles(dbTester.getSession(), organization, of("js", "java"))).extracting(RulesProfileDto::getKee).containsOnly("java_sonar_way");
  }

  @Test
  public void get_by_name_and_language() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    RulesProfileDto dto = underTest.selectByNameAndLanguage(organization, "Sonar Way", "java", dbTester.getSession());
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParentKee()).isNull();

    assertThat(underTest.selectByNameAndLanguage(organization, "Sonar Way", "java", dbTester.getSession())).isNotNull();
    assertThat(underTest.selectByNameAndLanguage(organization, "Sonar Way", "unknown", dbTester.getSession())).isNull();
  }

  @Test
  public void get_by_name_and_languages() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<RulesProfileDto> dtos = underTest.selectByNameAndLanguages(organization, "Sonar Way", singletonList("java"), dbTester.getSession());
    assertThat(dtos).hasSize(1);
    RulesProfileDto dto = dtos.iterator().next();
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParentKee()).isNull();

    assertThat(underTest.selectByNameAndLanguages(organization, "Sonar Way", singletonList("unknown"), dbTester.getSession())).isEmpty();
    assertThat(underTest.selectByNameAndLanguages(organization, "Sonar Way", of("java", "unknown"), dbTester.getSession())).extracting("id").containsOnly(1);
  }

  @Test
  public void should_find_by_language() {
    RulesProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid());
    underTest.insert(dbSession, profile);

    List<RulesProfileDto> results = underTest.selectByLanguage(dbSession, organization, profile.getLanguage());
    assertThat(results).hasSize(1);
    RulesProfileDto result = results.get(0);

    assertThat(result.getId()).isEqualTo(profile.getId());
    assertThat(result.getName()).isEqualTo(profile.getName());
    assertThat(result.getKee()).isEqualTo(profile.getKee());
    assertThat(result.getLanguage()).isEqualTo(profile.getLanguage());
    assertThat(result.getOrganizationUuid()).isEqualTo(profile.getOrganizationUuid());
  }

  @Test
  public void should_not_find_by_language_in_wrong_organization() {
    RulesProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid());
    underTest.insert(dbSession, profile);

    List<RulesProfileDto> results = underTest.selectByLanguage(dbSession, OrganizationTesting.newOrganizationDto(), profile.getLanguage());
    assertThat(results).isEmpty();
  }

  @Test
  public void should_not_find_by_language_with_wrong_language() {
    RulesProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid());
    underTest.insert(dbSession, profile);

    List<RulesProfileDto> results = underTest.selectByLanguage(dbSession, organization, "another language");
    assertThat(results).isEmpty();
  }

  @Test
  public void find_children() {
    dbTester.prepareDbUnit(getClass(), "inheritance.xml");

    List<RulesProfileDto> dtos = underTest.selectChildren(dbTester.getSession(), "java_parent");

    assertThat(dtos).hasSize(2);

    RulesProfileDto dto1 = dtos.get(0);
    assertThat(dto1.getId()).isEqualTo(1);
    assertThat(dto1.getName()).isEqualTo("Child1");
    assertThat(dto1.getLanguage()).isEqualTo("java");
    assertThat(dto1.getParentKee()).isEqualTo("java_parent");

    RulesProfileDto dto2 = dtos.get(1);
    assertThat(dto2.getId()).isEqualTo(2);
    assertThat(dto2.getName()).isEqualTo("Child2");
    assertThat(dto2.getLanguage()).isEqualTo("java");
    assertThat(dto2.getParentKee()).isEqualTo("java_parent");
  }

  @Test
  public void countProjectsByProfileKey() {
    RulesProfileDto profileWithoutProjects = dbTester.qualityProfiles().insert(organization);
    RulesProfileDto profileWithProjects = dbTester.qualityProfiles().insert(organization);
    ComponentDto project1 = dbTester.components().insertPrivateProject(organization);
    ComponentDto project2 = dbTester.components().insertPrivateProject(organization);
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project1, profileWithProjects);
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project2, profileWithProjects);

    OrganizationDto otherOrg = dbTester.organizations().insert();
    RulesProfileDto profileInOtherOrg = dbTester.qualityProfiles().insert(otherOrg);
    ComponentDto projectInOtherOrg = dbTester.components().insertPrivateProject(otherOrg);
    dbTester.qualityProfiles().associateProjectWithQualityProfile(projectInOtherOrg, profileInOtherOrg);

    assertThat(underTest.countProjectsByProfileKey(dbTester.getSession(), organization)).containsOnly(
      MapEntry.entry(profileWithProjects.getKee(), 2L));
  }

  @Test
  public void test_selectAssociatedToProjectAndLanguage() {
    OrganizationDto org = dbTester.organizations().insert();
    ComponentDto project1 = dbTester.components().insertPublicProject(org);
    ComponentDto project2 = dbTester.components().insertPublicProject(org);
    RulesProfileDto javaProfile = dbTester.qualityProfiles().insert(org, p -> p.setLanguage("java"));
    RulesProfileDto jsProfile = dbTester.qualityProfiles().insert(org, p -> p.setLanguage("js"));
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project1, javaProfile, jsProfile);

    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbTester.getSession(), project1, "java").getKee())
      .isEqualTo(javaProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbTester.getSession(), project1, "js").getKee())
      .isEqualTo(jsProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbTester.getSession(), project1, "cobol"))
      .isNull();
    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbTester.getSession(), project2, "java"))
      .isNull();
  }

  @Test
  public void test_selectAssociatedToProjectUuidAndLanguages() {
    OrganizationDto org = dbTester.organizations().insert();
    ComponentDto project1 = dbTester.components().insertPublicProject(org);
    ComponentDto project2 = dbTester.components().insertPublicProject(org);
    RulesProfileDto javaProfile = dbTester.qualityProfiles().insert(org, p -> p.setLanguage("java"));
    RulesProfileDto jsProfile = dbTester.qualityProfiles().insert(org, p -> p.setLanguage("js"));
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project1, javaProfile, jsProfile);

    assertThat(underTest.selectAssociatedToProjectUuidAndLanguages(dbTester.getSession(), project1, singletonList("java")))
      .extracting(RulesProfileDto::getKee).containsOnly(javaProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectUuidAndLanguages(dbTester.getSession(), project1, singletonList("unknown")))
      .isEmpty();
    assertThat(underTest.selectAssociatedToProjectUuidAndLanguages(dbTester.getSession(), project1, of("java", "unknown")))
      .extracting(RulesProfileDto::getKee).containsExactly(javaProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectUuidAndLanguages(dbTester.getSession(), project1, of("java", "js")))
      .extracting(RulesProfileDto::getKee).containsExactlyInAnyOrder(javaProfile.getKee(), jsProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectUuidAndLanguages(dbTester.getSession(), project2, singletonList("java")))
      .isEmpty();
  }

  @Test
  public void test_updateProjectProfileAssociation() {
    OrganizationDto org = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(org);
    RulesProfileDto javaProfile1 = dbTester.qualityProfiles().insert(org, p -> p.setLanguage("java"));
    RulesProfileDto jsProfile = dbTester.qualityProfiles().insert(org, p -> p.setLanguage("js"));
    RulesProfileDto javaProfile2 = dbTester.qualityProfiles().insert(org, p -> p.setLanguage("java"));
    qualityProfileDb.associateProjectWithQualityProfile(project, javaProfile1, jsProfile);

    underTest.updateProjectProfileAssociation(dbSession, project, javaProfile2.getKee(), javaProfile1.getKee());

    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project, "java").getKee()).isEqualTo(javaProfile2.getKee());
    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project, "js").getKee()).isEqualTo(jsProfile.getKee());
  }

  @Test
  public void selectByKeys() {
    qualityProfileDb.insertQualityProfiles(newQualityProfileDto().setKey("qp-key-1"), newQualityProfileDto().setKee("qp-key-2"), newQualityProfileDto().setKee("qp-key-3"));

    assertThat(underTest.selectOrFailByKey(dbSession, "qp-key-1")).isNotNull();
    assertThat(underTest.selectByKey(dbSession, "qp-key-1")).isNotNull();
    assertThat(underTest.selectByKey(dbSession, "qp-key-42")).isNull();
    assertThat(underTest.selectByKeys(dbSession, newArrayList("qp-key-1", "qp-key-3", "qp-key-42")))
      .hasSize(2)
      .extracting(RulesProfileDto::getKee).containsOnlyOnce("qp-key-1", "qp-key-3");
    assertThat(underTest.selectByKeys(dbSession, emptyList())).isEmpty();
  }

  @Test
  public void select_selected_projects() throws Exception {
    ComponentDto project1 = dbTester.components().insertPrivateProject(t -> t.setName("Project1 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project2 = dbTester.components().insertPrivateProject(t -> t.setName("Project2 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project3 = dbTester.components().insertPrivateProject(t -> t.setName("Project3 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    OrganizationDto organization2 = dbTester.organizations().insert();
    ComponentDto project4 = dbTester.components().insertPrivateProject(t -> t.setName("Project4 name"), t -> t.setOrganizationUuid(organization2.getUuid()));

    RulesProfileDto profile1 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile1);
    qualityProfileDb.associateProjectWithQualityProfile(project1, profile1);
    qualityProfileDb.associateProjectWithQualityProfile(project2, profile1);

    RulesProfileDto profile2 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile2);
    qualityProfileDb.associateProjectWithQualityProfile(project3, profile2);

    assertThat(underTest.selectSelectedProjects(organization, profile1.getKee(), null, dbSession))
      .extracting("projectId", "projectUuid", "projectKey", "projectName", "profileKey")
      .containsOnly(
        tuple(project1.getId(), project1.uuid(), project1.key(), project1.name(), profile1.getKee()),
        tuple(project2.getId(), project2.uuid(), project2.key(), project2.name(), profile1.getKee()));

    assertThat(underTest.selectSelectedProjects(organization, profile1.getKee(), "ect1", dbSession)).hasSize(1);
    assertThat(underTest.selectSelectedProjects(organization, "unknown", null, dbSession)).isEmpty();
  }

  @Test
  public void select_deselected_projects() throws Exception {
    ComponentDto project1 = dbTester.components().insertPrivateProject(t -> t.setName("Project1 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project2 = dbTester.components().insertPrivateProject(t -> t.setName("Project2 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project3 = dbTester.components().insertPrivateProject(t -> t.setName("Project3 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    OrganizationDto organization2 = dbTester.organizations().insert();
    ComponentDto project4 = dbTester.components().insertPrivateProject(t -> t.setName("Project4 name"), t -> t.setOrganizationUuid(organization2.getUuid()));

    RulesProfileDto profile1 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile1);
    qualityProfileDb.associateProjectWithQualityProfile(project1, profile1);

    RulesProfileDto profile2 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile2);
    qualityProfileDb.associateProjectWithQualityProfile(project2, profile2);

    assertThat(underTest.selectDeselectedProjects(organization, profile1.getKee(), null, dbSession))
      .extracting("projectId", "projectUuid", "projectKey", "projectName", "profileKey")
      .containsExactly(
        tuple(project2.getId(), project2.uuid(), project2.key(), project2.name(), null),
        tuple(project3.getId(), project3.uuid(), project3.key(), project3.name(), null));

    assertThat(underTest.selectDeselectedProjects(organization, profile1.getKee(), "ect2", dbSession)).hasSize(1);
    assertThat(underTest.selectDeselectedProjects(organization, "unknown", null, dbSession)).hasSize(3);
  }

  @Test
  public void select_project_associations() throws Exception {
    ComponentDto project1 = dbTester.components().insertPrivateProject(t -> t.setName("Project1 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project2 = dbTester.components().insertPrivateProject(t -> t.setName("Project2 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project3 = dbTester.components().insertPrivateProject(t -> t.setName("Project3 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    OrganizationDto organization2 = dbTester.organizations().insert();
    ComponentDto project4 = dbTester.components().insertPrivateProject(t -> t.setName("Project4 name"), t -> t.setOrganizationUuid(organization2.getUuid()));

    RulesProfileDto profile1 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile1);
    qualityProfileDb.associateProjectWithQualityProfile(project1, profile1);

    RulesProfileDto profile2 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile2);
    qualityProfileDb.associateProjectWithQualityProfile(project2, profile2);

    assertThat(underTest.selectProjectAssociations(organization, profile1.getKee(), null, dbSession))
      .extracting("projectId", "projectUuid", "projectKey", "projectName", "profileKey")
      .containsOnly(
        tuple(project1.getId(), project1.uuid(), project1.key(), project1.name(), profile1.getKee()),
        tuple(project2.getId(), project2.uuid(), project2.key(), project2.name(), null),
        tuple(project3.getId(), project3.uuid(), project3.key(), project3.name(), null));

    assertThat(underTest.selectProjectAssociations(organization, profile1.getKee(), "ect2", dbSession)).hasSize(1);
    assertThat(underTest.selectProjectAssociations(organization, "unknown", null, dbSession)).hasSize(3);
  }

  @Test
  public void selectOutdatedProfiles_returns_the_custom_profiles_with_specified_name() {
    OrganizationDto org1 = dbTester.organizations().insert();
    OrganizationDto org2 = dbTester.organizations().insert();
    OrganizationDto org3 = dbTester.organizations().insert();
    RulesProfileDto outdatedProfile1 = dbTester.qualityProfiles().insert(org1, p -> p.setIsBuiltIn(false).setLanguage("java").setName("foo"));
    RulesProfileDto outdatedProfile2 = dbTester.qualityProfiles().insert(org2, p -> p.setIsBuiltIn(false).setLanguage("java").setName("foo"));
    RulesProfileDto builtInProfile = dbTester.qualityProfiles().insert(org3, p -> p.setIsBuiltIn(true).setLanguage("java").setName("foo"));
    RulesProfileDto differentLanguage = dbTester.qualityProfiles().insert(org1, p -> p.setIsBuiltIn(false).setLanguage("cobol").setName("foo"));
    RulesProfileDto differentName = dbTester.qualityProfiles().insert(org1, p -> p.setIsBuiltIn(false).setLanguage("java").setName("bar"));

    Collection<String> keys = underTest.selectOutdatedProfiles(dbSession, "java", "foo");

    assertThat(keys).containsExactlyInAnyOrder(outdatedProfile1.getKee(), outdatedProfile2.getKee());
  }

  @Test
  public void selectOutdatedProfiles_returns_empty_list_if_no_match() {
    assertThat(underTest.selectOutdatedProfiles(dbSession, "java", "foo")).isEmpty();
  }

  @Test
  public void renameAndCommit_updates_name_of_specified_profiles() {
    OrganizationDto org1 = dbTester.organizations().insert();
    OrganizationDto org2 = dbTester.organizations().insert();
    RulesProfileDto fooInOrg1 = dbTester.qualityProfiles().insert(org1, p -> p.setName("foo"));
    RulesProfileDto fooInOrg2 = dbTester.qualityProfiles().insert(org2, p -> p.setName("foo"));
    RulesProfileDto bar = dbTester.qualityProfiles().insert(org1, p -> p.setName("bar"));

    underTest.renameAndCommit(dbSession, asList(fooInOrg1.getKee(), fooInOrg2.getKee()), "foo (copy)");

    assertThat(underTest.selectOrFailByKey(dbSession, fooInOrg1.getKee()).getName()).isEqualTo("foo (copy)");
    assertThat(underTest.selectOrFailByKey(dbSession, fooInOrg2.getKee()).getName()).isEqualTo("foo (copy)");
    assertThat(underTest.selectOrFailByKey(dbSession, bar.getKee()).getName()).isEqualTo("bar");
  }

  @Test
  public void renameAndCommit_does_nothing_if_empty_keys() {
    OrganizationDto org = dbTester.organizations().insert();
    RulesProfileDto profile = dbTester.qualityProfiles().insert(org, p -> p.setName("foo"));

    underTest.renameAndCommit(dbSession, Collections.emptyList(), "foo (copy)");

    assertThat(underTest.selectOrFailByKey(dbSession, profile.getKee()).getName()).isEqualTo("foo");
  }

  private RulesProfileDto insertQualityProfileDto(String key, String name, String language) {
    RulesProfileDto dto = RulesProfileDto.createFor(key)
      .setOrganizationUuid(organization.getUuid())
      .setName(name)
      .setLanguage(language);
    underTest.insert(dbSession, dto);
    return dto;
  }

}
