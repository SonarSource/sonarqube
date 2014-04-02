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
package org.sonar.core.technicaldebt.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class RequirementDaoTest extends AbstractDaoTestCase {

  private static final String[] EXCLUDED_COLUMNS = new String[]{"id", "created_at", "updated_at"};

  RequirementDao dao;

  @Before
  public void createDao() {
    dao = new RequirementDao(getMyBatis());
  }

  @Test
  public void select_requirements() {
    setupData("shared");

    assertThat(dao.selectRequirements()).hasSize(2);
  }

  @Test
  public void select_requirement() {
    setupData("select_requirement");

    List<RequirementDto> dtos = dao.selectRequirements();
    assertThat(dtos).hasSize(1);

    RequirementDto dto = dtos.get(0);
    assertThat(dto.getId()).isEqualTo(3);
    assertThat(dto.getParentId()).isEqualTo(2);
    assertThat(dto.getRuleId()).isEqualTo(10);
    assertThat(dto.getFunction()).isEqualTo("linear_offset");
    assertThat(dto.getCoefficientValue()).isEqualTo(20d);
    assertThat(dto.getCoefficientUnit()).isEqualTo("mn");
    assertThat(dto.getOffsetValue()).isEqualTo(30d);
    assertThat(dto.getOffsetUnit()).isEqualTo("h");
    assertThat(dto.isEnabled()).isTrue();
    assertThat(dto.getCreatedAt()).isNotNull();
    assertThat(dto.getUpdatedAt()).isNull();
  }

}
