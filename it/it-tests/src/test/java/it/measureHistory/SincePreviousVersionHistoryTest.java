/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package it.measureHistory;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import it.Category1Suite;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.DateUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ItUtils;
import util.QaOnly;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;

@Category(QaOnly.class)
public class SincePreviousVersionHistoryTest {

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";
  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @BeforeClass
  public static void initPeriods() throws Exception {
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period2", "30");
    setServerProperty(orchestrator, "sonar.timemachine.period3", "previous_version");
  }

  @AfterClass
  public static void resetPeriods() throws Exception {
    ItUtils.resetPeriods(orchestrator);
  }

  private static void analyzeProject(String version) {
    analyzeProject(version, null, null);
  }

  private static void analyzeProjectWithExclusions(String version, String exclusions) {
    analyzeProject(version, exclusions, null);
  }

  private static void analyzeProjectWithDate(String version, String date) {
    analyzeProject(version, null, date);
  }

  private static void analyzeProject(String version, @Nullable String exclusions, @Nullable String date) {
    SonarRunner build = SonarRunner.create(projectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.projectVersion", version);
    if (exclusions != null) {
      build.setProperties("sonar.exclusions", exclusions);
    }
    if (date != null) {
      build.setProperty("sonar.projectDate", date);
    }
    orchestrator.executeBuild(build);
  }

  public static String toStringDate(Date date) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    return sdf.format(date);
  }

  @Before
  public void resetData() throws Exception {
    orchestrator.resetData();
  }

  /**
   * SONAR-2496
   */
  @Test
  public void test_since_previous_version_period() {
    analyzeProjectWithExclusions("0.9", "**/*2.xoo");
    analyzeProject("1.0-SNAPSHOT");
    analyzeProject("1.0-SNAPSHOT");

    Resource project = getProject("files");
    Measure measure = project.getMeasure("files");

    // There are 4 files
    assertThat(measure.getValue()).isEqualTo(4);

    // nothing changed in the previous analysis
    assertThat(project.getPeriod1Mode()).isEqualTo("previous_analysis");
    assertThat(measure.getVariation1()).isEqualTo(0);

    // but 2 files were added since the first analysis which was version 0.9
    assertThat(project.getPeriod3Mode()).isEqualTo("previous_version");
    assertThat(project.getPeriod3Param()).isEqualTo("0.9");
    assertThat(measure.getVariation3()).isEqualTo(2);
  }

  /**
   * SONAR-6356
   */
  @Test
  public void since_previous_version_should_use_first_analysis_when_no_version_found() {
    Date now = new Date();

    // Analyze project by excluding some files
    analyzeProject("1.0-SNAPSHOT", "**/*2.xoo", toStringDate(DateUtils.addDays(now, -2)));
    // No difference measure after first analysis
    assertThat(getProject("files").getMeasure("files").getVariation3()).isNull();

    analyzeProjectWithDate("1.0-SNAPSHOT", toStringDate(DateUtils.addDays(now, -1)));
    // No new version, first analysis is used -> 2 new files
    assertThat(getProject("files").getMeasure("files").getVariation3()).isEqualTo(2);

    analyzeProjectWithDate("1.0-SNAPSHOT", toStringDate(now));
    // Still no new version, first analysis is used -> 2 new files
    assertThat(getProject("files").getMeasure("files").getVariation3()).isEqualTo(2);
  }

  private Resource getProject(String... metricKeys) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT, metricKeys).setIncludeTrends(true));
  }

}
