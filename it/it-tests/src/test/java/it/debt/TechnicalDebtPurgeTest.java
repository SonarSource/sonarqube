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
package it.debt;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category2Suite;
import java.sql.SQLException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class TechnicalDebtPurgeTest {

  private static final String SQL_COUNT_MEASURES_ON_CHARACTERISTICS = "select count(*) from project_measures where characteristic_id is not null";
  private static final String SQL_COUNT_MEASURES_ON_DEBT_MEASURES_WITH_RULES = "select count(*) from project_measures where rule_id is not null and metric_id in (select id from metrics where name='sqale_index')";
  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  private static void scanProject(String date) {
    SonarRunner scan = SonarRunner.create(projectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.projectDate", date);
    orchestrator.executeBuild(scan);
  }

  @Before
  public void resetData() throws Exception {
    orchestrator.resetData();

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/debt/with-many-rules.xml"));
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "com.sonarsource.it.samples:multi-modules-sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "with-many-rules");
  }

  /**
   * SONAR-2756
   */
  @Test
  public void purge_measures_on_requirements() throws SQLException {
    scanProject("2012-01-01");
    int onCharacteristicsCount = orchestrator.getDatabase().countSql(SQL_COUNT_MEASURES_ON_CHARACTERISTICS);
    int onRequirementsCount = orchestrator.getDatabase().countSql(SQL_COUNT_MEASURES_ON_DEBT_MEASURES_WITH_RULES);
    assertThat(onCharacteristicsCount).isGreaterThan(0);
    assertThat(onRequirementsCount).isGreaterThan(0);

    scanProject("2012-02-02");
    // past measures on characteristics are not purged
    assertThat(orchestrator.getDatabase().countSql(SQL_COUNT_MEASURES_ON_CHARACTERISTICS)).isGreaterThan(onCharacteristicsCount);

    // past measures on debt with rules are purged
    assertThat(orchestrator.getDatabase().countSql(SQL_COUNT_MEASURES_ON_DEBT_MEASURES_WITH_RULES)).isEqualTo(onRequirementsCount);
  }
}
