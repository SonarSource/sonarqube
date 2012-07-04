/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.filters;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.filter.CriterionDto;
import org.sonar.core.filter.FilterColumnDto;
import org.sonar.core.filter.FilterDao;
import org.sonar.core.filter.FilterDto;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.fest.assertions.Assertions.assertThat;

public class FilterDaoTest extends AbstractDaoTestCase {
  private FilterDao dao;

  @Before
  public void createDao() {
    dao = new FilterDao(getMyBatis());
  }

  @Test
  public void should_find_filter() {
    setupData("shouldFindFilter");

    FilterDto filter = dao.findFilter("Projects");

    assertThat(filter.getId()).isEqualTo(1L);
    assertThat(filter.getName()).isEqualTo("Projects");
    assertThat(filter.getKey()).isEqualTo("Projects");
    assertThat(dao.findFilter("<UNKNOWN>")).isNull();
  }

  @Test
  public void should_insert() {
    setupData("shouldInsert");

    FilterDto filterDto = new FilterDto();
    filterDto.setName("Projects");
    filterDto.setKey("Projects");
    filterDto.setShared(true);
    filterDto.setFavourites(false);
    filterDto.setDefaultView("list");
    filterDto.setPageSize(10L);

    CriterionDto criterionDto = new CriterionDto();
    criterionDto.setFamily("family");
    criterionDto.setKey("key");
    criterionDto.setOperator("=");
    criterionDto.setValue(1.5f);
    criterionDto.setTextValue("1.5");
    criterionDto.setVariation(true);
    filterDto.add(criterionDto);

    FilterColumnDto filterColumnDto = new FilterColumnDto();
    filterColumnDto.setFamily("family");
    filterColumnDto.setKey("key");
    filterColumnDto.setSortDirection("ASC");
    filterColumnDto.setOrderIndex(2L);
    filterColumnDto.setVariation(true);
    filterDto.add(filterColumnDto);

    dao.insert(filterDto);

    checkTables("shouldInsert", "filters", "criteria", "filter_columns");
  }
}