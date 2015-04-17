/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.qualityprofile.db;

import org.assertj.core.data.MapEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.UtcDateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualityProfileDaoTest extends AbstractDaoTestCase {

  QualityProfileDao dao;
  DbSession session;
  System2 system = mock(System2.class);

  @Before
  public void createDao() {
    this.session = getMyBatis().openSession(false);
    dao = new QualityProfileDao(getMyBatis(), system);
    when(system.now()).thenReturn(UtcDateUtils.parseDateTime("2014-01-20T12:00:00+0000").getTime());
  }

  @After
  public void after() {
    this.session.close();
  }

  @Test
  public void insert() {
    setupData("shared");

    QualityProfileDto dto = QualityProfileDto.createFor("abcde")
      .setName("ABCDE")
      .setLanguage("xoo");

    dao.insert(dto);

    checkTables("insert", new String[]{"created_at", "updated_at", "rules_updated_at"}, "rules_profiles");
  }

  @Test
  public void update() {
    setupData("shared");

    QualityProfileDto dto = new QualityProfileDto()
      .setId(1)
      .setName("New Name")
      .setLanguage("js")
      .setParentKee("fghij")
      .setDefault(false);

    dao.update(dto);

    checkTables("update", new String[]{"created_at", "updated_at", "rules_updated_at"}, "rules_profiles");
  }

  @Test
  public void delete() {
    setupData("shared");

    dao.delete(1);

    checkTables("delete", "rules_profiles");
  }

  @Test
  public void find_all() {
    setupData("shared");

    DbSession session = getMyBatis().openSession(false);
    try {
      List<QualityProfileDto> dtos = dao.findAll(session);

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
    } finally {
      session.close();
    }
  }

  @Test
  public void find_all_is_sorted_by_profile_name() {
    setupData("select_all_is_sorted_by_profile_name");

    List<QualityProfileDto> dtos = dao.findAll();

    assertThat(dtos).hasSize(3);
    assertThat(dtos.get(0).getName()).isEqualTo("First");
    assertThat(dtos.get(1).getName()).isEqualTo("Second");
    assertThat(dtos.get(2).getName()).isEqualTo("Third");
  }

  @Test
  public void get_default_profile() {
    setupData("shared");

    QualityProfileDto java = dao.getDefaultProfile("java");
    assertThat(java).isNotNull();
    assertThat(java.getKey()).isEqualTo("java_sonar_way");

    assertThat(dao.getDefaultProfile("js")).isNull();
  }

  @Test
  public void get_by_name_and_language() {
    setupData("shared");

    QualityProfileDto dto = dao.getByNameAndLanguage("Sonar Way", "java");
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParentKee()).isNull();

    assertThat(dao.getByNameAndLanguage("Sonar Way", "java")).isNotNull();
    assertThat(dao.getByNameAndLanguage("Sonar Way", "unknown")).isNull();
  }

  @Test
  public void find_by_language() {
    setupData("select_by_language");

    List<QualityProfileDto> result = dao.findByLanguage("java");
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("Sonar Way 1");
    assertThat(result.get(1).getName()).isEqualTo("Sonar Way 2");
  }

  @Test
  public void get_by_id() {
    setupData("shared");

    QualityProfileDto dto = dao.getById(1);
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParentKee()).isNull();

    assertThat(dao.getById(555)).isNull();
  }

  @Test
  public void get_parent_by_id() {
    setupData("inheritance");

    QualityProfileDto dto = dao.getParentById(1);
    assertThat(dto.getId()).isEqualTo(3);
  }

  @Test
  public void find_children() {
    setupData("inheritance");

    DbSession session = getMyBatis().openSession(false);
    try {
      List<QualityProfileDto> dtos = dao.findChildren(session, "java_parent");

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

    } finally {
      session.close();
    }
  }

  @Test
  public void select_projects() {
    setupData("projects");

    assertThat(dao.selectProjects("Sonar Way", "java")).hasSize(2);
  }

  @Test
  public void count_projects() {
    setupData("projects");

    assertThat(dao.countProjects("Sonar Way", "java")).isEqualTo(2);
  }

  @Test
  public void count_projects_by_profile() {
    setupData("projects");

    assertThat(dao.countProjectsByProfileKey()).containsOnly(
      MapEntry.entry("java_sonar_way", 2L),
      MapEntry.entry("js_sonar_way", 2L));
  }

  @Test
  public void select_by_project_id_and_language() {
    setupData("projects");

    QualityProfileDto dto = dao.getByProjectAndLanguage(1L, "java");
    assertThat(dto.getId()).isEqualTo(1);
  }

  @Test
  public void select_by_project_key_and_language() {
    setupData("projects");

    QualityProfileDto dto = dao.getByProjectAndLanguage("org.codehaus.sonar:sonar", "java", session);
    assertThat(dto.getId()).isEqualTo(1);

    assertThat(dao.getByProjectAndLanguage("org.codehaus.sonar:sonar", "unkown", session)).isNull();
    assertThat(dao.getByProjectAndLanguage("unknown", "java", session)).isNull();
  }
}
