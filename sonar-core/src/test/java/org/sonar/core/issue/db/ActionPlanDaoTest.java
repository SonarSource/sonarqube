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

package org.sonar.core.issue.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class ActionPlanDaoTest extends AbstractDaoTestCase {

  private ActionPlanDao dao;

  @Before
  public void createDao() {
    dao = new ActionPlanDao(getMyBatis());
  }

  @Test
  public void should_find_by_key() {
    setupData("should_find_by_key");

    ActionPlanDto result = dao.findByKey("ABC");
    assertThat(result).isNotNull();
    assertThat(result.getKey()).isEqualTo("ABC");
  }

  @Test
  public void should_find_by_keys() {
    setupData("should_find_by_keys");

    Collection<ActionPlanDto> result = dao.findByKeys(newArrayList("ABC", "ABD", "ABE"));
    assertThat(result).hasSize(3);
  }

  @Test
  public void should_find_open_by_project_id() {
    setupData("should_find_open_by_project_id");

    Collection<ActionPlanDto> result = dao.findOpenByProjectId(1l);
    assertThat(result).hasSize(2);
  }
}
