/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.tests.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category3Suite;
import java.util.Map;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasureAsDouble;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;

public class MultiLanguageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @After
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  /**
   * SONAR-926
   * SONAR-5069
   */
  @Test
  public void test_sonar_runner_inspection() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/analysis/MultiLanguageTest/one-issue-per-line.xml"));
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/analysis/MultiLanguageTest/one-issue-per-line-xoo2.xml"));

    orchestrator.getServer().provisionProject("multi-language-sample", "multi-language-sample");

    orchestrator.getServer().associateProjectToQualityProfile("multi-language-sample", "xoo", "one-issue-per-line");
    orchestrator.getServer().associateProjectToQualityProfile("multi-language-sample", "xoo2", "one-issue-per-line-xoo2");

    SonarScanner build = SonarScanner.create().setProjectDir(ItUtils.projectDir("analysis/xoo-multi-languages"));
    BuildResult result = orchestrator.executeBuild(build);

    // 4 files: 1 .xoo, 1.xoo2, 2 .measures
    assertThat(result.getLogs()).contains("4 files indexed");
    assertThat(result.getLogs()).contains("Quality profile for xoo: one-issue-per-line");
    assertThat(result.getLogs()).contains("Quality profile for xoo2: one-issue-per-line-xoo2");

    // modules
    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(orchestrator, "multi-language-sample", "files", "violations");
    assertThat(measures.get("files")).isEqualTo(2);
    assertThat(measures.get("violations")).isEqualTo(26);

    assertThat(getMeasureAsDouble(orchestrator, "multi-language-sample:src/sample/Sample.xoo", "violations")).isEqualTo(13);
    assertThat(getMeasureAsDouble(orchestrator, "multi-language-sample:src/sample/Sample.xoo2", "violations")).isEqualTo(13);
  }
}
