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

  @Test
  public void technical_debt_measures_on_characteristics_on_project() {
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "PORTABILITY").getValue()).isEqualTo(0);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "MAINTAINABILITY").getValue()).isEqualTo(4);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "SECURITY").getValue()).isEqualTo(340);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "EFFICIENCY").getValue()).isEqualTo(61);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "CHANGEABILITY").getValue()).isEqualTo(40);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "RELIABILITY").getValue()).isEqualTo(0);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "READABILITY").getValue()).isEqualTo(4);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "TESTABILITY").getValue()).isEqualTo(0);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "REUSABILITY").getValue()).isEqualTo(0);

    // sub characteristics
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "API_ABUSE").getValue()).isEqualTo(340);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "ARCHITECTURE_CHANGEABILITY").getValue()).isEqualTo(40);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "MEMORY_EFFICIENCY").getValue()).isEqualTo(61);
  }

  @Test
  public void technical_debt_measures_on_characteristics_on_modules() {
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "MAINTAINABILITY").getValue()).isEqualTo(4);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "PORTABILITY").getValue()).isEqualTo(0);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "SECURITY").getValue()).isEqualTo(170);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "EFFICIENCY").getValue()).isEqualTo(37);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "CHANGEABILITY").getValue()).isEqualTo(20);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "RELIABILITY").getValue()).isEqualTo(0);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "READABILITY").getValue()).isEqualTo(4);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "TESTABILITY").getValue()).isEqualTo(0);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "REUSABILITY").getValue()).isEqualTo(0);

    // sub characteristics
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "API_ABUSE").getValue()).isEqualTo(170);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "ARCHITECTURE_CHANGEABILITY").getValue()).isEqualTo(20);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "MEMORY_EFFICIENCY").getValue()).isEqualTo(37);
  }

  @Test
  public void technical_debt_measures_on_characteristics_on_directory() {
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "PORTABILITY")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "MAINTAINABILITY").getValue()).isEqualTo(2);
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "SECURITY")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "EFFICIENCY").getValue()).isEqualTo(16);
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "CHANGEABILITY").getValue()).isEqualTo(10);
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "RELIABILITY")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "READABILITY").getValue()).isEqualTo(2);
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "TESTABILITY")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "REUSABILITY")).isNull();

    // sub characteristics
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "API_ABUSE")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "ARCHITECTURE_CHANGEABILITY").getValue()).isEqualTo(10);
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "MEMORY_EFFICIENCY").getValue()).isEqualTo(16);
  }

  @Test
  public void technical_debt_measures_on_characteristics_on_file() {
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "PORTABILITY")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "MAINTAINABILITY").getValue()).isEqualTo(2);
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "SECURITY")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "EFFICIENCY").getValue()).isEqualTo(16);
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "CHANGEABILITY").getValue()).isEqualTo(10);
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "RELIABILITY")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "READABILITY").getValue()).isEqualTo(2);
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "TESTABILITY")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "REUSABILITY")).isNull();

    // sub characteristics
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "API_ABUSE")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "ARCHITECTURE_CHANGEABILITY").getValue()).isEqualTo(10);
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "MEMORY_EFFICIENCY").getValue()).isEqualTo(16);
  }

  @Test
  public void not_save_zero_value_on_non_top_characteristics() throws Exception {
    String sqlRequest = "SELECT count(*) FROM project_measures WHERE characteristic_id IN (select id from characteristics where parent_id IS NOT NULL) AND value = 0";
    assertThat(orchestrator.getDatabase().countSql(sqlRequest)).isEqualTo(0);
  }

  private Measure getMeasure(String resource, String metricKey) {
    Resource res = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resource, metricKey));
    if (res == null) {
      return null;
    }
    return res.getMeasure(metricKey);
  }

  private Measure getCharacteristicMeasure(String resource, String metricKey, String characteristicKey) {
    Resource res = orchestrator.getServer().getWsClient().find(
      ResourceQuery.createForMetrics(resource, metricKey).setCharacteristics(characteristicKey));
    if (res == null) {
      return null;
    }
    return res.getMeasure(metricKey);
  }

}
