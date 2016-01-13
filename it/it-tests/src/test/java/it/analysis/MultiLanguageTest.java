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
package it.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category3Suite;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

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
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/analysis/MultiLanguageTest/one-issue-per-line.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/analysis/MultiLanguageTest/one-issue-per-line-xoo2.xml"));

    orchestrator.getServer().provisionProject("multi-language-sample", "multi-language-sample");

    orchestrator.getServer().associateProjectToQualityProfile("multi-language-sample", "xoo", "one-issue-per-line");
    orchestrator.getServer().associateProjectToQualityProfile("multi-language-sample","xoo2", "one-issue-per-line-xoo2");

    SonarRunner build = SonarRunner.create().setProjectDir(ItUtils.projectDir("analysis/xoo-multi-languages"));
    BuildResult result = orchestrator.executeBuild(build);

    assertThat(result.getLogs()).contains("2 files indexed");
    assertThat(result.getLogs()).contains("Quality profile for xoo: one-issue-per-line");
    assertThat(result.getLogs()).contains("Quality profile for xoo2: one-issue-per-line-xoo2");

    // modules
    Resource project = getResource("multi-language-sample", "files", "violations");
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(26);

    Resource xooFile = getResource("multi-language-sample:src/sample/Sample.xoo", "violations");
    assertThat(xooFile.getMeasureIntValue("violations")).isEqualTo(13);

    Resource xoo2File = getResource("multi-language-sample:src/sample/Sample.xoo2", "violations");
    assertThat(xoo2File.getMeasureIntValue("violations")).isEqualTo(13);
  }

  private Resource getResource(String resourceKey, String... metricKeys) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resourceKey, metricKeys));
  }
}
