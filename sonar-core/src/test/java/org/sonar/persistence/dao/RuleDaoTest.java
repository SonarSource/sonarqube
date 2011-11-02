/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.persistence.dao;

import static org.junit.Assert.assertThat;

import java.util.List;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

public class RuleDaoTest extends AbstractDbUnitTestCase {

  private static RuleDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new RuleDao(getMyBatis());
  }

  @Test
  public void testSelectAll() throws Exception {
    setupData("selectAll");
    List<org.sonar.persistence.model.Rule> rules = dao.selectAll();

    assertThat(rules.size(), Is.is(1));
    org.sonar.persistence.model.Rule rule = rules.get(0);
    assertThat(rule.getId(), Is.is(1L));
    assertThat(rule.getName(), Is.is("Avoid Null"));
    assertThat(rule.getDescription(), Is.is("Should avoid NULL"));
    assertThat(rule.isEnabled(), Is.is(true));
    assertThat(rule.getRepositoryKey(), Is.is("checkstyle"));
  }

  @Test
  public void testSelectById() throws Exception {
    setupData("selectById");
    org.sonar.persistence.model.Rule rule = dao.selectById(2L);

    assertThat(rule.getId(), Is.is(2L));
    assertThat(rule.getName(), Is.is("Avoid Null"));
    assertThat(rule.getDescription(), Is.is("Should avoid NULL"));
    assertThat(rule.isEnabled(), Is.is(true));
    assertThat(rule.getRepositoryKey(), Is.is("checkstyle"));
  }

}
