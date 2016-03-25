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
package org.sonar.db.qualityprofile;

import java.util.List;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UtcDateUtils;
import org.sonar.db.DbTester;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class QualityProfileDaoTest {

  System2 system = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system);

  QualityProfileDao dao = dbTester.getDbClient().qualityProfileDao();

  @Before
  public void createDao() {
    when(system.now()).thenReturn(UtcDateUtils.parseDateTime("2014-01-20T12:00:00+0000").getTime());
  }

  @Test
  public void insert() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    QualityProfileDto dto = QualityProfileDto.createFor("abcde")
      .setName("ABCDE")
      .setLanguage("xoo");

    dao.insert(dto);

    dbTester.assertDbUnit(getClass(), "insert-result.xml", new String[]{"created_at", "updated_at", "rules_updated_at"}, "rules_profiles");
  }

  @Test
  public void update() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    QualityProfileDto dto = new QualityProfileDto()
      .setId(1)
      .setName("New Name")
      .setLanguage("js")
      .setParentKee("fghij")
      .setDefault(false);

    dao.update(dto);

    dbTester.assertDbUnit(getClass(), "update-result.xml", new String[]{"created_at", "updated_at", "rules_updated_at"}, "rules_profiles");
  }

  @Test
  public void delete() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    dao.delete(1);

    dbTester.assertDbUnit(getClass(), "delete-result.xml", "rules_profiles");
  }

  @Test
  public void find_all() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<QualityProfileDto> dtos = dao.selectAll(dbTester.getSession());

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

    List<QualityProfileDto> dtos = dao.selectAll();

    assertThat(dtos).hasSize(3);
    assertThat(dtos.get(0).getName()).isEqualTo("First");
    assertThat(dtos.get(1).getName()).isEqualTo("Second");
    assertThat(dtos.get(2).getName()).isEqualTo("Third");
  }

  @Test
  public void get_default_profile() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    QualityProfileDto java = dao.selectDefaultProfile("java");
    assertThat(java).isNotNull();
    assertThat(java.getKey()).isEqualTo("java_sonar_way");

    assertThat(dao.selectDefaultProfile("js")).isNull();
  }

  @Test
  public void get_default_profiles() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<QualityProfileDto> java = dao.selectDefaultProfiles(dbTester.getSession(), singletonList("java"));
    assertThat(java).extracting("key").containsOnly("java_sonar_way");

    assertThat(dao.selectDefaultProfiles(dbTester.getSession(), singletonList("js"))).isEmpty();
    assertThat(dao.selectDefaultProfiles(dbTester.getSession(), of("java", "js"))).extracting("key").containsOnly("java_sonar_way");
    assertThat(dao.selectDefaultProfiles(dbTester.getSession(), of("js", "java"))).extracting("key").containsOnly("java_sonar_way");
  }

  @Test
  public void get_by_name_and_language() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    QualityProfileDto dto = dao.selectByNameAndLanguage("Sonar Way", "java", dbTester.getSession());
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParentKee()).isNull();

    assertThat(dao.selectByNameAndLanguage("Sonar Way", "java", dbTester.getSession())).isNotNull();
    assertThat(dao.selectByNameAndLanguage("Sonar Way", "unknown", dbTester.getSession())).isNull();
  }

  @Test
  public void get_by_name_and_languages() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<QualityProfileDto> dtos = dao.selectByNameAndLanguages("Sonar Way", singletonList("java"), dbTester.getSession());
    assertThat(dtos).hasSize(1);
    QualityProfileDto dto = dtos.iterator().next();
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParentKee()).isNull();

    assertThat(dao.selectByNameAndLanguages("Sonar Way", singletonList("unknown"), dbTester.getSession())).isEmpty();
    assertThat(dao.selectByNameAndLanguages("Sonar Way", of("java", "unknown"), dbTester.getSession())).extracting("id").containsOnly(1);
  }

  @Test
  public void find_by_language() {
    dbTester.prepareDbUnit(getClass(), "select_by_language.xml");

    List<QualityProfileDto> result = dao.selectByLanguage("java");
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("Sonar Way 1");
    assertThat(result.get(1).getName()).isEqualTo("Sonar Way 2");
  }

  @Test
  public void get_by_id() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    QualityProfileDto dto = dao.selectById(1);
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParentKee()).isNull();

    assertThat(dao.selectById(555)).isNull();
  }

  @Test
  public void get_parent_by_id() {
    dbTester.prepareDbUnit(getClass(), "inheritance.xml");

    QualityProfileDto dto = dao.selectParentById(1);
    assertThat(dto.getId()).isEqualTo(3);
  }

  @Test
  public void find_children() {
    dbTester.prepareDbUnit(getClass(), "inheritance.xml");

    List<QualityProfileDto> dtos = dao.selectChildren(dbTester.getSession(), "java_parent");

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

    assertThat(dao.selectProjects("Sonar Way", "java")).hasSize(2);
  }

  @Test
  public void count_projects() {
    dbTester.prepareDbUnit(getClass(), "projects.xml");

    assertThat(dao.countProjects("Sonar Way", "java")).isEqualTo(2);
  }

  @Test
  public void count_projects_by_profile() {
    dbTester.prepareDbUnit(getClass(), "projects.xml");

    assertThat(dao.countProjectsByProfileKey()).containsOnly(
      MapEntry.entry("java_sonar_way", 2L),
      MapEntry.entry("js_sonar_way", 2L));
  }

  @Test
  public void select_by_project_id_and_language() {
    dbTester.prepareDbUnit(getClass(), "projects.xml");

    QualityProfileDto dto = dao.selectByProjectAndLanguage(1L, "java");
    assertThat(dto.getId()).isEqualTo(1);
  }

  @Test
  public void select_by_project_key_and_language() {
    dbTester.prepareDbUnit(getClass(), "projects.xml");

    QualityProfileDto dto = dao.selectByProjectAndLanguage(dbTester.getSession(), "org.codehaus.sonar:sonar", "java");
    assertThat(dto.getId()).isEqualTo(1);

    assertThat(dao.selectByProjectAndLanguage(dbTester.getSession(), "org.codehaus.sonar:sonar", "unkown")).isNull();
    assertThat(dao.selectByProjectAndLanguage(dbTester.getSession(), "unknown", "java")).isNull();
  }

  @Test
  public void select_by_project_key_and_languages() {
    dbTester.prepareDbUnit(getClass(), "projects.xml");

    List<QualityProfileDto> dto = dao.selectByProjectAndLanguages(dbTester.getSession(), "org.codehaus.sonar:sonar", singletonList("java"));
    assertThat(dto).extracting("id").containsOnly(1);

    assertThat(dao.selectByProjectAndLanguages(dbTester.getSession(), "org.codehaus.sonar:sonar", singletonList("unkown"))).isEmpty();
    assertThat(dao.selectByProjectAndLanguages(dbTester.getSession(), "org.codehaus.sonar:sonar", of("java", "unkown"))).extracting("id").containsOnly(1);
    assertThat(dao.selectByProjectAndLanguages(dbTester.getSession(), "unknown", singletonList("java"))).isEmpty();
  }
}
