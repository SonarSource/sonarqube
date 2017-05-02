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

    QualityProfileDto dto = QualityProfileDto.createFor("abcde")
      .setOrganizationUuid(organization.getUuid())
      .setName("ABCDE")
      .setLanguage("xoo");

    underTest.insert(dbTester.getSession(), dto);
    dbTester.commit();

    dbTester.assertDbUnit(getClass(), "insert-result.xml", new String[] {"created_at", "updated_at", "rules_updated_at"}, "rules_profiles");
  }

  @Test
  public void update() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    QualityProfileDto dto = QualityProfileDto.createFor("key")
      .setId(1)
      .setOrganizationUuid(organization.getUuid())
      .setName("New Name")
      .setLanguage("js")
      .setParentKee("fghij")
      .setDefault(false);

    underTest.update(dbSession, dto);
    dbSession.commit();

    dbTester.assertDbUnit(getClass(), "update-result.xml", new String[] {"created_at", "updated_at", "rules_updated_at"}, "rules_profiles");
  }

  @Test
  public void test_deleteByKeys() {
    QualityProfileDto p1 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());
    QualityProfileDto p2 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());
    QualityProfileDto p3 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());

    underTest.deleteByKeys(dbSession, asList(p1.getKey(), p3.getKey(), "does_not_exist"));

    List<Map<String, Object>> keysInDb = dbTester.select(dbSession, "select kee as \"key\" from rules_profiles");
    assertThat(keysInDb).hasSize(1);
    assertThat(keysInDb.get(0).get("key")).isEqualTo(p2.getKey());
  }

  @Test
  public void deleteByKeys_does_nothing_if_empty_keys() {
    QualityProfileDto p1 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());

    underTest.deleteByKeys(dbSession, Collections.emptyList());

    assertThat(dbTester.countRowsOfTable(dbSession, "rules_profiles")).isEqualTo(1);
  }

  @Test
  public void deleteProjectAssociationsByProfileKeys_does_nothing_if_empty_keys() {
    QualityProfileDto profile1 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());
    ComponentDto project1 = dbTester.components().insertPrivateProject();
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project1, profile1);

    underTest.deleteProjectAssociationsByProfileKeys(dbSession, Collections.emptyList());

    assertThat(dbTester.countRowsOfTable(dbSession, "project_qprofiles")).isEqualTo(1);
  }

  @Test
  public void deleteProjectAssociationsByProfileKeys_deletes_rows_from_table_project_profiles() {
    QualityProfileDto profile1 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());
    QualityProfileDto profile2 = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());
    ComponentDto project1 = dbTester.components().insertPrivateProject();
    ComponentDto project2 = dbTester.components().insertPrivateProject();
    ComponentDto project3 = dbTester.components().insertPrivateProject();
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project1, profile1);
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project2, profile1);
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project3, profile2);

    underTest.deleteProjectAssociationsByProfileKeys(dbSession, asList(profile1.getKey(), "does_not_exist"));

    List<Map<String, Object>> rows = dbTester.select(dbSession, "select project_uuid as \"projectUuid\", profile_key as \"profileKey\" from project_qprofiles");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("projectUuid")).isEqualTo(project3.uuid());
    assertThat(rows.get(0).get("profileKey")).isEqualTo(profile2.getKey());
  }

  @Test
  public void find_all() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<QualityProfileDto> dtos = underTest.selectAll(dbTester.getSession(), organization);

    assertThat(dtos).hasSize(2);

    QualityProfileDto dto1 = dtos.get(0);
    assertThat(dto1.getId()).isEqualTo(1);
    assertThat(dto1.getName()).isEqualTo("Sonar Way");
    assertThat(dto1.getLanguage()).isEqualTo("java");
    assertThat(dto1.getParentKee()).isNull();

    QualityProfileDto dto2 = dtos.get(1);
    assertThat(dto2.getId()).isEqualTo(2);
    assertThat(dto2.getName()).isEqualTo("Sonar Way");
    assertThat(dto2.getLanguage()).isEqualTo("js");
    assertThat(dto2.getParentKee()).isNull();
  }

  @Test
  public void find_all_is_sorted_by_profile_name() {
    dbTester.prepareDbUnit(getClass(), "select_all_is_sorted_by_profile_name.xml");

    List<QualityProfileDto> dtos = underTest.selectAll(dbTester.getSession(), organization);

    assertThat(dtos).hasSize(3);
    assertThat(dtos.get(0).getName()).isEqualTo("First");
    assertThat(dtos.get(1).getName()).isEqualTo("Second");
    assertThat(dtos.get(2).getName()).isEqualTo("Third");
  }

  @Test
  public void get_default_profile() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    QualityProfileDto java = underTest.selectDefaultProfile(dbTester.getSession(), organization, "java");
    assertThat(java).isNotNull();
    assertThat(java.getKey()).isEqualTo("java_sonar_way");

    assertThat(underTest.selectDefaultProfile(dbTester.getSession(), dbTester.organizations().insert(), "java")).isNull();
    assertThat(underTest.selectDefaultProfile(dbTester.getSession(), organization, "js")).isNull();
  }

  @Test
  public void get_default_profiles() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<QualityProfileDto> java = underTest.selectDefaultProfiles(dbTester.getSession(), organization, singletonList("java"));
    assertThat(java).extracting("key").containsOnly("java_sonar_way");

    assertThat(underTest.selectDefaultProfiles(dbTester.getSession(), organization, singletonList("js"))).isEmpty();
    assertThat(underTest.selectDefaultProfiles(dbTester.getSession(), organization, of("java", "js"))).extracting("key").containsOnly("java_sonar_way");
    assertThat(underTest.selectDefaultProfiles(dbTester.getSession(), organization, of("js", "java"))).extracting("key").containsOnly("java_sonar_way");
  }

  @Test
  public void get_by_name_and_language() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    QualityProfileDto dto = underTest.selectByNameAndLanguage(organization, "Sonar Way", "java", dbTester.getSession());
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

    List<QualityProfileDto> dtos = underTest.selectByNameAndLanguages(organization, "Sonar Way", singletonList("java"), dbTester.getSession());
    assertThat(dtos).hasSize(1);
    QualityProfileDto dto = dtos.iterator().next();
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParentKee()).isNull();

    assertThat(underTest.selectByNameAndLanguages(organization, "Sonar Way", singletonList("unknown"), dbTester.getSession())).isEmpty();
    assertThat(underTest.selectByNameAndLanguages(organization, "Sonar Way", of("java", "unknown"), dbTester.getSession())).extracting("id").containsOnly(1);
  }

  @Test
  public void should_find_by_language() {
    QualityProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid());
    underTest.insert(dbSession, profile);

    List<QualityProfileDto> results = underTest.selectByLanguage(dbSession, organization, profile.getLanguage());
    assertThat(results).hasSize(1);
    QualityProfileDto result = results.get(0);

    assertThat(result.getId()).isEqualTo(profile.getId());
    assertThat(result.getName()).isEqualTo(profile.getName());
    assertThat(result.getKey()).isEqualTo(profile.getKey());
    assertThat(result.getLanguage()).isEqualTo(profile.getLanguage());
    assertThat(result.getOrganizationUuid()).isEqualTo(profile.getOrganizationUuid());
  }

  @Test
  public void should_not_find_by_language_in_wrong_organization() {
    QualityProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid());
    underTest.insert(dbSession, profile);

    List<QualityProfileDto> results = underTest.selectByLanguage(dbSession, OrganizationTesting.newOrganizationDto(), profile.getLanguage());
    assertThat(results).isEmpty();
  }

  @Test
  public void should_not_find_by_language_with_wrong_language() {
    QualityProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid());
    underTest.insert(dbSession, profile);

    List<QualityProfileDto> results = underTest.selectByLanguage(dbSession, organization, "another language");
    assertThat(results).isEmpty();
  }

  @Test
  public void find_children() {
    dbTester.prepareDbUnit(getClass(), "inheritance.xml");

    List<QualityProfileDto> dtos = underTest.selectChildren(dbTester.getSession(), "java_parent");

    assertThat(dtos).hasSize(2);

    QualityProfileDto dto1 = dtos.get(0);
    assertThat(dto1.getId()).isEqualTo(1);
    assertThat(dto1.getName()).isEqualTo("Child1");
    assertThat(dto1.getLanguage()).isEqualTo("java");
    assertThat(dto1.getParentKee()).isEqualTo("java_parent");

    QualityProfileDto dto2 = dtos.get(1);
    assertThat(dto2.getId()).isEqualTo(2);
    assertThat(dto2.getName()).isEqualTo("Child2");
    assertThat(dto2.getLanguage()).isEqualTo("java");
    assertThat(dto2.getParentKee()).isEqualTo("java_parent");
  }

  @Test
  public void countProjectsByProfileKey() {
    QualityProfileDto profileWithoutProjects = dbTester.qualityProfiles().insert(organization);
    QualityProfileDto profileWithProjects = dbTester.qualityProfiles().insert(organization);
    ComponentDto project1 = dbTester.components().insertPrivateProject(organization);
    ComponentDto project2 = dbTester.components().insertPrivateProject(organization);
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project1, profileWithProjects);
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project2, profileWithProjects);

    OrganizationDto otherOrg = dbTester.organizations().insert();
    QualityProfileDto profileInOtherOrg = dbTester.qualityProfiles().insert(otherOrg);
    ComponentDto projectInOtherOrg = dbTester.components().insertPrivateProject(otherOrg);
    dbTester.qualityProfiles().associateProjectWithQualityProfile(projectInOtherOrg, profileInOtherOrg);

    assertThat(underTest.countProjectsByProfileKey(dbTester.getSession(), organization)).containsOnly(
      MapEntry.entry(profileWithProjects.getKey(), 2L));
  }

  @Test
  public void select_by_project_key_and_language() {
    dbTester.prepareDbUnit(getClass(), "projects.xml");

    QualityProfileDto dto = underTest.selectByProjectAndLanguage(dbTester.getSession(), "org.codehaus.sonar:sonar", "java");
    assertThat(dto.getId()).isEqualTo(1);

    assertThat(underTest.selectByProjectAndLanguage(dbTester.getSession(), "org.codehaus.sonar:sonar", "unkown")).isNull();
    assertThat(underTest.selectByProjectAndLanguage(dbTester.getSession(), "unknown", "java")).isNull();
  }

  @Test
  public void select_by_project_and_languages() {
    dbTester.prepareDbUnit(getClass(), "projects.xml");

    OrganizationDto organization = dbTester.organizations().insert(OrganizationTesting.newOrganizationDto().setUuid("org1"));
    ComponentDto project = dbTester.getDbClient().componentDao().selectOrFailByKey(dbTester.getSession(), "org.codehaus.sonar:sonar");
    ComponentDto unknownProject = dbTester.components().insertPrivateProject(organization, p -> p.setKey("unknown"));
    List<QualityProfileDto> dto = underTest.selectByProjectAndLanguages(dbTester.getSession(), organization, project, singletonList("java"));
    assertThat(dto).extracting("id").containsOnly(1);

    assertThat(underTest.selectByProjectAndLanguages(dbTester.getSession(), organization, project, singletonList("unkown"))).isEmpty();
    assertThat(underTest.selectByProjectAndLanguages(dbTester.getSession(), organization, project, of("java", "unkown"))).extracting("id").containsOnly(1);
    assertThat(underTest.selectByProjectAndLanguages(dbTester.getSession(), organization, unknownProject, singletonList("java"))).isEmpty();
  }

  @Test
  public void selectByKeys() {
    qualityProfileDb.insertQualityProfiles(newQualityProfileDto().setKey("qp-key-1"), newQualityProfileDto().setKee("qp-key-2"), newQualityProfileDto().setKee("qp-key-3"));

    assertThat(underTest.selectOrFailByKey(dbSession, "qp-key-1")).isNotNull();
    assertThat(underTest.selectByKey(dbSession, "qp-key-1")).isNotNull();
    assertThat(underTest.selectByKey(dbSession, "qp-key-42")).isNull();
    assertThat(underTest.selectByKeys(dbSession, newArrayList("qp-key-1", "qp-key-3", "qp-key-42")))
      .hasSize(2)
      .extracting(QualityProfileDto::getKey).containsOnlyOnce("qp-key-1", "qp-key-3");
    assertThat(underTest.selectByKeys(dbSession, emptyList())).isEmpty();
  }

  @Test
  public void select_selected_projects() throws Exception {
    ComponentDto project1 = dbTester.components().insertPrivateProject(t -> t.setName("Project1 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project2 = dbTester.components().insertPrivateProject(t -> t.setName("Project2 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project3 = dbTester.components().insertPrivateProject(t -> t.setName("Project3 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    OrganizationDto organization2 = dbTester.organizations().insert();
    ComponentDto project4 = dbTester.components().insertPrivateProject(t -> t.setName("Project4 name"), t -> t.setOrganizationUuid(organization2.getUuid()));

    QualityProfileDto profile1 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile1);
    qualityProfileDb.associateProjectWithQualityProfile(project1, profile1);
    qualityProfileDb.associateProjectWithQualityProfile(project2, profile1);

    QualityProfileDto profile2 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile2);
    qualityProfileDb.associateProjectWithQualityProfile(project3, profile2);

    assertThat(underTest.selectSelectedProjects(organization, profile1.getKey(), null, dbSession))
      .extracting("projectId", "projectUuid", "projectKey", "projectName", "profileKey")
      .containsOnly(
        tuple(project1.getId(), project1.uuid(), project1.key(), project1.name(), profile1.getKey()),
        tuple(project2.getId(), project2.uuid(), project2.key(), project2.name(), profile1.getKey()));

    assertThat(underTest.selectSelectedProjects(organization, profile1.getKey(), "ect1", dbSession)).hasSize(1);
    assertThat(underTest.selectSelectedProjects(organization, "unknown", null, dbSession)).isEmpty();
  }

  @Test
  public void select_deselected_projects() throws Exception {
    ComponentDto project1 = dbTester.components().insertPrivateProject(t -> t.setName("Project1 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project2 = dbTester.components().insertPrivateProject(t -> t.setName("Project2 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project3 = dbTester.components().insertPrivateProject(t -> t.setName("Project3 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    OrganizationDto organization2 = dbTester.organizations().insert();
    ComponentDto project4 = dbTester.components().insertPrivateProject(t -> t.setName("Project4 name"), t -> t.setOrganizationUuid(organization2.getUuid()));

    QualityProfileDto profile1 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile1);
    qualityProfileDb.associateProjectWithQualityProfile(project1, profile1);

    QualityProfileDto profile2 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile2);
    qualityProfileDb.associateProjectWithQualityProfile(project2, profile2);

    assertThat(underTest.selectDeselectedProjects(organization, profile1.getKey(), null, dbSession))
      .extracting("projectId", "projectUuid", "projectKey", "projectName", "profileKey")
      .containsExactly(
        tuple(project2.getId(), project2.uuid(), project2.key(), project2.name(), null),
        tuple(project3.getId(), project3.uuid(), project3.key(), project3.name(), null));

    assertThat(underTest.selectDeselectedProjects(organization, profile1.getKey(), "ect2", dbSession)).hasSize(1);
    assertThat(underTest.selectDeselectedProjects(organization, "unknown", null, dbSession)).hasSize(3);
  }

  @Test
  public void select_project_associations() throws Exception {
    ComponentDto project1 = dbTester.components().insertPrivateProject(t -> t.setName("Project1 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project2 = dbTester.components().insertPrivateProject(t -> t.setName("Project2 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project3 = dbTester.components().insertPrivateProject(t -> t.setName("Project3 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    OrganizationDto organization2 = dbTester.organizations().insert();
    ComponentDto project4 = dbTester.components().insertPrivateProject(t -> t.setName("Project4 name"), t -> t.setOrganizationUuid(organization2.getUuid()));

    QualityProfileDto profile1 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile1);
    qualityProfileDb.associateProjectWithQualityProfile(project1, profile1);

    QualityProfileDto profile2 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile2);
    qualityProfileDb.associateProjectWithQualityProfile(project2, profile2);

    assertThat(underTest.selectProjectAssociations(organization, profile1.getKey(), null, dbSession))
      .extracting("projectId", "projectUuid", "projectKey", "projectName", "profileKey")
      .containsOnly(
        tuple(project1.getId(), project1.uuid(), project1.key(), project1.name(), profile1.getKey()),
        tuple(project2.getId(), project2.uuid(), project2.key(), project2.name(), null),
        tuple(project3.getId(), project3.uuid(), project3.key(), project3.name(), null));

    assertThat(underTest.selectProjectAssociations(organization, profile1.getKey(), "ect2", dbSession)).hasSize(1);
    assertThat(underTest.selectProjectAssociations(organization, "unknown", null, dbSession)).hasSize(3);
  }

  @Test
  public void update_project_profile_association() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    QualityProfileDto profile1Language1 = insertQualityProfileDto("profile1", "Profile 1", "xoo");
    QualityProfileDto profile2Language2 = insertQualityProfileDto("profile2", "Profile 2", "xoo2");
    QualityProfileDto profile3Language1 = insertQualityProfileDto("profile3", "Profile 3", "xoo");
    qualityProfileDb.associateProjectWithQualityProfile(project, profile1Language1, profile2Language2);

    underTest.updateProjectProfileAssociation(project.uuid(), profile3Language1.getKey(), profile1Language1.getKey(), dbSession);

    assertThat(underTest.selectByProjectAndLanguage(dbSession, project.getKey(), "xoo").getKey()).isEqualTo(profile3Language1.getKey());
    assertThat(underTest.selectByProjectAndLanguage(dbSession, project.getKey(), "xoo2").getKey()).isEqualTo(profile2Language2.getKey());
  }

  private QualityProfileDto insertQualityProfileDto(String key, String name, String language) {
    QualityProfileDto dto = QualityProfileDto.createFor(key)
      .setOrganizationUuid(organization.getUuid())
      .setName(name)
      .setLanguage(language);
    underTest.insert(dbSession, dto);
    return dto;
  }

}
