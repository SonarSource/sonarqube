/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonarqube.tests.measure;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category1Suite;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.DateUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import util.ItUtils;

import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.WsMeasures.Measure;
import static org.sonarqube.ws.WsMeasures.PeriodValue;
import static util.ItUtils.getLeakPeriodValue;
import static util.ItUtils.getMeasureWithVariation;
import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;

public class SincePreviousVersionHistoryTest {

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";
  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @BeforeClass
  public static void initPeriod() throws Exception {
    setServerProperty(orchestrator, "sonar.leak.period", "previous_version");
  }

  @AfterClass
  public static void resetPeriod() throws Exception {
    ItUtils.resetPeriod(orchestrator);
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
    SonarScanner build = SonarScanner.create(projectDir("shared/xoo-multi-modules-sample"))
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

    Measure measure = getMeasureWithVariation(orchestrator, PROJECT, "files");

    // There are 4 files
    assertThat(parseInt(measure.getValue())).isEqualTo(4);
    // 2 files were added since the first analysis which was version 0.9
    assertThat(measure.getPeriods().getPeriodsValueList()).extracting(PeriodValue::getValue).contains("2");
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
    assertThat(getLeakPeriodValue(orchestrator, PROJECT, "files")).isNull();

    analyzeProjectWithDate("1.0-SNAPSHOT", toStringDate(DateUtils.addDays(now, -1)));
    // No new version, first analysis is used -> 2 new files
    assertThat(getLeakPeriodValue(orchestrator, PROJECT, "files")).isEqualTo(2);

    analyzeProjectWithDate("1.0-SNAPSHOT", toStringDate(now));
    // Still no new version, first analysis is used -> 2 new files
    assertThat(getLeakPeriodValue(orchestrator, PROJECT, "files")).isEqualTo(2);
  }

}
