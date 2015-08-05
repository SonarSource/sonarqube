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

package org.sonar.db.issue;

import java.util.Collection;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class ActionPlanDaoTest {

  private static final String[] EXCLUDED_COLUMNS = new String[] {"id", "created_at", "updated_at"};

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  ActionPlanDao dao = dbTester.getDbClient().actionPlanDao();

  @Test
  public void should_insert_new_action_plan() {
    dbTester.truncateTables();

    ActionPlanDto actionPlanDto = new ActionPlanDto().setKey("ABC").setName("Long term").setDescription("Long term action plan").setStatus("OPEN")
      .setProjectId(1l).setUserLogin("arthur");

    dao.save(actionPlanDto);

    dbTester.assertDbUnit(getClass(), "should_insert_new_action_plan-result.xml", EXCLUDED_COLUMNS, "action_plans");
  }

  @Test
  public void should_update_action_plan() {
    dbTester.prepareDbUnit(getClass(), "should_update_action_plan.xml");

    ActionPlanDto actionPlanDto = new ActionPlanDto().setKey("ABC").setName("Long term").setDescription("Long term action plan").setStatus("OPEN")
      .setProjectId(1l).setUserLogin("arthur");
    dao.update(actionPlanDto);

    dbTester.assertDbUnit(getClass(), "should_update_action_plan-result.xml", EXCLUDED_COLUMNS, "action_plans");
  }

  @Test
  public void should_delete_action_plan() {
    dbTester.prepareDbUnit(getClass(), "should_delete_action_plan.xml");

    dao.delete("BCD");

    dbTester.assertDbUnit(getClass(), "should_delete_action_plan-result.xml", EXCLUDED_COLUMNS, "action_plans");
  }

  @Test
  public void should_find_by_key() {
    dbTester.prepareDbUnit(getClass(), "shared.xml", "should_find_by_key.xml");

    ActionPlanDto result = dao.selectByKey("ABC");
    assertThat(result).isNotNull();
    assertThat(result.getKey()).isEqualTo("ABC");
    assertThat(result.getProjectKey()).isEqualTo("PROJECT_KEY");
    assertThat(result.getProjectUuid()).isEqualTo("PROJECT_UUID");
  }

  @Test
  public void should_find_by_keys() {
    dbTester.prepareDbUnit(getClass(), "shared.xml", "should_find_by_keys.xml");

    Collection<ActionPlanDto> result = dao.selectByKeys(newArrayList("ABC", "ABD", "ABE"));
    assertThat(result).hasSize(3);
  }

  @Test
  public void should_find_by_keys_on_huge_number_of_keys() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<String> hugeNbOKeys = newArrayList();
    for (int i = 0; i < 4500; i++) {
      hugeNbOKeys.add("ABCD" + i);
    }
    List<ActionPlanDto> result = dao.selectByKeys(hugeNbOKeys);

    // The goal of this test is only to check that the query do no fail, not to check the number of results
    assertThat(result).isEmpty();
  }

  @Test
  public void should_find_open_by_project_id() {
    dbTester.prepareDbUnit(getClass(), "shared.xml", "should_find_open_by_project_id.xml");

    Collection<ActionPlanDto> result = dao.selectOpenByProjectId(1l);
    assertThat(result).hasSize(2);
  }

  @Test
  public void should_find_by_name_and_project_id() {
    dbTester.prepareDbUnit(getClass(), "shared.xml", "should_find_by_name_and_project_id.xml");

    Collection<ActionPlanDto> result = dao.selectByNameAndProjectId("SHORT_TERM", 1l);
    assertThat(result).hasSize(2);
  }
}
