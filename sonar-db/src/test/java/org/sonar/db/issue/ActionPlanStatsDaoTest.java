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
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class ActionPlanStatsDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  ActionPlanStatsDao dao = dbTester.getDbClient().getActionPlanStatsDao();

  @Test
  public void should_find_by_project() {
    dbTester.prepareDbUnit(getClass(), "shared.xml", "should_find_by_project.xml");

    Collection<ActionPlanStatsDto> result = dao.selectByProjectId(1l);
    assertThat(result).isNotEmpty();

    ActionPlanStatsDto actionPlanStatsDto = result.iterator().next();
    assertThat(actionPlanStatsDto.getProjectKey()).isEqualTo("PROJECT_KEY");
    assertThat(actionPlanStatsDto.getProjectUuid()).isEqualTo("PROJECT_UUID");
    assertThat(actionPlanStatsDto.getTotalIssues()).isEqualTo(3);
    assertThat(actionPlanStatsDto.getUnresolvedIssues()).isEqualTo(1);
  }

}
