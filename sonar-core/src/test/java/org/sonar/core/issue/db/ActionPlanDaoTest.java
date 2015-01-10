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

package org.sonar.core.issue.db;

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class ActionPlanDaoTest extends AbstractDaoTestCase {

  ActionPlanDao dao;

  @Before
  public void createDao() {
    dao = new ActionPlanDao(getMyBatis());
  }

  @Test
  public void should_insert_new_action_plan() {
    ActionPlanDto actionPlanDto = new ActionPlanDto().setKey("ABC").setName("Long term").setDescription("Long term action plan").setStatus("OPEN")
                                    .setProjectId(1l).setUserLogin("arthur");

    dao.save(actionPlanDto);

    checkTables("should_insert_new_action_plan", new String[]{"id", "created_at", "updated_at"}, "action_plans");
  }

  @Test
  public void should_update_action_plan() {
    setupData("should_update_action_plan");

    ActionPlanDto actionPlanDto = new ActionPlanDto().setKey("ABC").setName("Long term").setDescription("Long term action plan").setStatus("OPEN")
                                    .setProjectId(1l).setUserLogin("arthur");
    dao.update(actionPlanDto);

    checkTables("should_update_action_plan", new String[]{"id", "created_at", "updated_at"}, "action_plans");
  }

  @Test
  public void should_delete_action_plan() {
    setupData("should_delete_action_plan");

    dao.delete("BCD");

    checkTables("should_delete_action_plan", new String[]{"id", "created_at", "updated_at"}, "action_plans");
  }

  @Test
  public void should_find_by_key() {
    setupData("shared", "should_find_by_key");

    ActionPlanDto result = dao.findByKey("ABC");
    assertThat(result).isNotNull();
    assertThat(result.getKey()).isEqualTo("ABC");
    assertThat(result.getProjectKey()).isEqualTo("org.sonar.Sample");
  }

  @Test
  public void should_find_by_keys() {
    setupData("shared", "should_find_by_keys");

    Collection<ActionPlanDto> result = dao.findByKeys(newArrayList("ABC", "ABD", "ABE"));
    assertThat(result).hasSize(3);
  }

  @Test
  public void should_find_by_keys_on_huge_number_of_keys() {
    setupData("shared");

    SqlSession session = getMyBatis().openSession();
    List<String> hugeNbOKeys = newArrayList();
    for (int i=0; i<4500; i++) {
      hugeNbOKeys.add("ABCD" + i);
    }
    List<ActionPlanDto> result = dao.findByKeys(hugeNbOKeys);
    MyBatis.closeQuietly(session);

    // The goal of this test is only to check that the query do no fail, not to check the number of results
    assertThat(result).isEmpty();
  }

  @Test
  public void should_find_open_by_project_id() {
    setupData("shared", "should_find_open_by_project_id");

    Collection<ActionPlanDto> result = dao.findOpenByProjectId(1l);
    assertThat(result).hasSize(2);
  }

  @Test
  public void should_find_by_name_and_project_id() {
    setupData("shared", "should_find_by_name_and_project_id");

    Collection<ActionPlanDto> result = dao.findByNameAndProjectId("SHORT_TERM", 1l);
    assertThat(result).hasSize(2);
  }
}
