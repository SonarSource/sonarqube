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

import java.util.List;
import org.assertj.core.data.MapEntry;
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
  public void delete() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.delete(dbSession, 1);
    dbSession.commit();

    dbTester.assertDbUnit(getClass(), "delete-result.xml", "rules_profiles");
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

    QualityProfileDto java = underTest.selectDefaultProfile(dbTester.getSession(), "java");
    assertThat(java).isNotNull();
    assertThat(java.getKey()).isEqualTo("java_sonar_way");

    assertThat(underTest.selectDefaultProfile(dbTester.getSession(), "js")).isNull();
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
  public void find_by_language() {
    dbTester.prepareDbUnit(getClass(), "select_by_language.xml");

    List<QualityProfileDto> result = underTest.selectByLanguage(dbTester.getSession(), "java");
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("Sonar Way 1");
    assertThat(result.get(1).getName()).isEqualTo("Sonar Way 2");
  }

  @Test
  public void get_by_id() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    QualityProfileDto dto = underTest.selectById(dbTester.getSession(), 1);
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParentKee()).isNull();

    assertThat(underTest.selectById(dbTester.getSession(),555)).isNull();
  }

  @Test
  public void get_parent_by_id() {
    dbTester.prepareDbUnit(getClass(), "inheritance.xml");

    QualityProfileDto dto = underTest.selectParentById(dbSession, 1);
    assertThat(dto.getId()).isEqualTo(3);
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
  public void select_projects() {
    dbTester.prepareDbUnit(getClass(), "projects.xml");

    assertThat(underTest.selectProjects("Sonar Way", "java", dbTester.getSession())).hasSize(2);
  }

  @Test
  public void count_projects_by_profile() {
    dbTester.prepareDbUnit(getClass(), "projects.xml");

    assertThat(underTest.countProjectsByProfileKey(dbTester.getSession())).containsOnly(
      MapEntry.entry("java_sonar_way", 2L),
      MapEntry.entry("js_sonar_way", 2L));
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
  public void select_by_project_key_and_languages() {
    dbTester.prepareDbUnit(getClass(), "projects.xml");

    OrganizationDto organization = OrganizationTesting.newOrganizationDto().setUuid("org1");
    List<QualityProfileDto> dto = underTest.selectByProjectAndLanguages(dbTester.getSession(), organization, "org.codehaus.sonar:sonar", singletonList("java"));
    assertThat(dto).extracting("id").containsOnly(1);

    assertThat(underTest.selectByProjectAndLanguages(dbTester.getSession(), organization, "org.codehaus.sonar:sonar", singletonList("unkown"))).isEmpty();
    assertThat(underTest.selectByProjectAndLanguages(dbTester.getSession(), organization, "org.codehaus.sonar:sonar", of("java", "unkown"))).extracting("id").containsOnly(1);
    assertThat(underTest.selectByProjectAndLanguages(dbTester.getSession(), organization, "unknown", singletonList("java"))).isEmpty();
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
    ComponentDto project1 = dbTester.components().insertProject((t) -> t.setName("Project1 name"));
    ComponentDto project2 = dbTester.components().insertProject((t) -> t.setName("Project2 name"));
    ComponentDto project3 = dbTester.components().insertProject((t) -> t.setName("Project3 name"));

    QualityProfileDto profile1 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile1);
    qualityProfileDb.associateProjectWithQualityProfile(project1, profile1);
    qualityProfileDb.associateProjectWithQualityProfile(project2, profile1);

    QualityProfileDto profile2 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile2);
    qualityProfileDb.associateProjectWithQualityProfile(project3, profile2);

    assertThat(underTest.selectSelectedProjects(profile1.getKey(), null, dbSession))
      .extracting("projectId", "projectUuid", "projectKey", "projectName", "profileKey")
      .containsOnly(
        tuple(project1.getId(), project1.uuid(), project1.key(), project1.name(), profile1.getKey()),
        tuple(project2.getId(), project2.uuid(), project2.key(), project2.name(), profile1.getKey()));

    assertThat(underTest.selectSelectedProjects(profile1.getKey(), "ect1", dbSession)).hasSize(1);
    assertThat(underTest.selectSelectedProjects("unknown", null, dbSession)).isEmpty();
  }

  @Test
  public void select_deselected_projects() throws Exception {
    ComponentDto project1 = dbTester.components().insertProject((t) -> t.setName("Project1 name"));
    ComponentDto project2 = dbTester.components().insertProject((t) -> t.setName("Project2 name"));
    ComponentDto project3 = dbTester.components().insertProject((t) -> t.setName("Project3 name"));

    QualityProfileDto profile1 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile1);
    qualityProfileDb.associateProjectWithQualityProfile(project1, profile1);

    QualityProfileDto profile2 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile2);
    qualityProfileDb.associateProjectWithQualityProfile(project2, profile2);

    assertThat(underTest.selectDeselectedProjects(profile1.getKey(), null, dbSession))
      .extracting("projectId", "projectUuid", "projectKey", "projectName", "profileKey")
      .containsOnly(
        tuple(project2.getId(), project2.uuid(), project2.key(), project2.name(), null),
        tuple(project3.getId(), project3.uuid(), project3.key(), project3.name(), null));

    assertThat(underTest.selectDeselectedProjects(profile1.getKey(), "ect2", dbSession)).hasSize(1);
    assertThat(underTest.selectDeselectedProjects("unknown", null, dbSession)).hasSize(3);
  }

  @Test
  public void select_project_associations() throws Exception {
    ComponentDto project1 = dbTester.components().insertProject((t) -> t.setName("Project1 name"));
    ComponentDto project2 = dbTester.components().insertProject((t) -> t.setName("Project2 name"));
    ComponentDto project3 = dbTester.components().insertProject((t) -> t.setName("Project3 name"));

    QualityProfileDto profile1 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile1);
    qualityProfileDb.associateProjectWithQualityProfile(project1, profile1);

    QualityProfileDto profile2 = newQualityProfileDto();
    qualityProfileDb.insertQualityProfiles(profile2);
    qualityProfileDb.associateProjectWithQualityProfile(project2, profile2);

    assertThat(underTest.selectProjectAssociations(profile1.getKey(), null, dbSession))
      .extracting("projectId", "projectUuid", "projectKey", "projectName", "profileKey")
      .containsOnly(
        tuple(project1.getId(), project1.uuid(), project1.key(), project1.name(), profile1.getKey()),
        tuple(project2.getId(), project2.uuid(), project2.key(), project2.name(), null),
        tuple(project3.getId(), project3.uuid(), project3.key(), project3.name(), null));

    assertThat(underTest.selectProjectAssociations(profile1.getKey(), "ect2", dbSession)).hasSize(1);
    assertThat(underTest.selectProjectAssociations("unknown", null, dbSession)).hasSize(3);
  }

  @Test
  public void update_project_profile_association() {
    ComponentDto project = dbTester.components().insertProject();
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
