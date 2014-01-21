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
package org.sonar.core.rule;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.fest.assertions.Assertions.assertThat;

public class RuleTagDaoTest extends AbstractDaoTestCase {

  RuleTagDao dao;

  @Before
  public void createDao() {
    dao = new RuleTagDao(getMyBatis());
  }

  @Test
  public void should_select_all_tags() {
    setupData("shared");

    assertThat(dao.selectAll()).hasSize(3);
  }

  @Test
  public void should_select_id() {
    setupData("shared");

    assertThat(dao.selectId("tag1")).isEqualTo(1L);
    assertThat(dao.selectId("unknown")).isNull();
  }

  @Test
  public void should_insert_tag() {
    setupData("shared");

    dao.insert(new RuleTagDto().setTag("tag4"));
    checkTable("insert", "rule_tags");
  }

  @Test
  public void should_delete_tag() {
    setupData("shared");

    dao.delete(1L);
    checkTable("delete", "rule_tags");
  }
}
