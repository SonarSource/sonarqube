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
package it.debt;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category2Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

/**
 * SONAR-4715
 */
public class TechnicalDebtMeasureTest {

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a";
  private static final String SUB_MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1";
  private static final String DIRECTORY = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1";
  private static final String FILE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo";
  private static final String TECHNICAL_DEBT_MEASURE = "sqale_index";
  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  @BeforeClass
  public static void init() {
    orchestrator.resetData();

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/debt/with-many-rules.xml"));
    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "with-many-rules");
    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-multi-modules-sample")));
  }

  @Test
  public void technical_debt_measures() {
    assertThat(getMeasure(PROJECT, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(445);
    assertThat(getMeasure(MODULE, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(231);
    assertThat(getMeasure(SUB_MODULE, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(113);
    assertThat(getMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(28);
    assertThat(getMeasure(FILE, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(28);
  }

  private Measure getMeasure(String resource, String metricKey) {
    Resource res = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resource, metricKey));
    if (res == null) {
      return null;
    }
    return res.getMeasure(metricKey);
  }
}
