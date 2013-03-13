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
package org.sonar.core.rule;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.List;

import static org.junit.Assert.assertThat;

public class RuleDaoTest extends AbstractDaoTestCase {

  private static RuleDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new RuleDao(getMyBatis());
  }

  @Test
  public void testSelectAll() throws Exception {
    setupData("selectAll");
    List<RuleDto> ruleDtos = dao.selectAll();

    assertThat(ruleDtos.size(), Is.is(1));
    RuleDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId(), Is.is(1L));
    assertThat(ruleDto.getName(), Is.is("Avoid Null"));
    assertThat(ruleDto.getDescription(), Is.is("Should avoid NULL"));
    assertThat(ruleDto.getStatus(), Is.is(RuleStatus.READY.name()));
    assertThat(ruleDto.getRepositoryKey(), Is.is("checkstyle"));
  }

  @Test
  public void testSelectById() throws Exception {
    setupData("selectById");
    RuleDto ruleDto = dao.selectById(2L);

    assertThat(ruleDto.getId(), Is.is(2L));
    assertThat(ruleDto.getName(), Is.is("Avoid Null"));
    assertThat(ruleDto.getDescription(), Is.is("Should avoid NULL"));
    assertThat(ruleDto.getStatus(), Is.is(RuleStatus.READY.name()));
    assertThat(ruleDto.getRepositoryKey(), Is.is("checkstyle"));
  }

}
