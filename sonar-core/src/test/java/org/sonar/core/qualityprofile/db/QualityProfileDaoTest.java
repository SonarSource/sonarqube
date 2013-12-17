/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class QualityProfileDaoTest extends AbstractDaoTestCase {

  QualityProfileDao dao;

  @Before
  public void createDao() {
    dao = new QualityProfileDao(getMyBatis());
  }

  @Test
  public void select_all() {
    setupData("shared");

    List<QualityProfileDto> dtos = dao.selectAll();

    assertThat(dtos).hasSize(2);

    QualityProfileDto dto1 = dtos.get(0);
    assertThat(dto1.getId()).isEqualTo(1);
    assertThat(dto1.getName()).isEqualTo("Sonar Way");
    assertThat(dto1.getLanguage()).isEqualTo("java");
    assertThat(dto1.getParent()).isNull();
    assertThat(dto1.getVersion()).isEqualTo(1);
    assertThat(dto1.isUsed()).isFalse();

    QualityProfileDto dto2 = dtos.get(1);
    assertThat(dto2.getId()).isEqualTo(2);
    assertThat(dto2.getName()).isEqualTo("Sonar Way");
    assertThat(dto2.getLanguage()).isEqualTo("js");
    assertThat(dto2.getParent()).isNull();
    assertThat(dto2.getVersion()).isEqualTo(1);
    assertThat(dto2.isUsed()).isFalse();
  }

  @Test
  public void select_default_profile() {
    setupData("shared");

    assertThat(dao.selectDefaultProfile("java", "sonar.profile.java")).isNotNull();
    assertThat(dao.selectDefaultProfile("js", "sonar.profile.js")).isNull();
  }

  @Test
  public void select_by_name_and_language() {
    setupData("shared");

    QualityProfileDto dto = dao.selectByNameAndLanguage("Sonar Way", "java");
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParent()).isNull();
    assertThat(dto.getVersion()).isEqualTo(1);
    assertThat(dto.isUsed()).isFalse();

    assertThat(dao.selectByNameAndLanguage("Sonar Way", "java")).isNotNull();
    assertThat(dao.selectByNameAndLanguage("Sonar Way", "unknown")).isNull();
  }

  @Test
  public void select_by_id() {
    setupData("shared");

    QualityProfileDto dto = dao.selectById(1);
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParent()).isNull();
    assertThat(dto.getVersion()).isEqualTo(1);
    assertThat(dto.isUsed()).isFalse();

    assertThat(dao.selectById(555)).isNull();
  }

  @Test
  public void select_projects() {
    setupData("projects");

    assertThat(dao.selectProjects("Sonar Way", "sonar.profile.java")).hasSize(2);
  }

  @Test
  public void select_by_project() {
    setupData("projects");

    assertThat(dao.selectByProject(1L, "sonar.profile.%")).hasSize(2);
  }

  @Test
  public void insert() {
    setupData("shared");

    QualityProfileDto dto = new QualityProfileDto()
      .setName("Sonar Way with Findbugs")
      .setLanguage("xoo")
      .setParent("Sonar Way")
      .setVersion(2)
      .setUsed(true);

    dao.insert(dto);

    checkTables("insert", "rules_profiles");
  }

  @Test
  public void update() {
    setupData("shared");

    QualityProfileDto dto = new QualityProfileDto()
      .setId(1)
      .setName("New Sonar Way with Findbugs")
      .setLanguage("js")
      .setParent("New Sonar Way")
      .setVersion(3)
      .setUsed(false);

    dao.update(dto);

    checkTables("update", "rules_profiles");
  }

  @Test
  public void delete() {
    setupData("shared");

    dao.delete(1);

    checkTables("delete", "rules_profiles");
  }

}
