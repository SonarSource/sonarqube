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
package org.sonarqube.tests.qualityModel;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Measures;
import util.ItUtils;

import static java.lang.Double.parseDouble;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasuresByMetricKey;
import static util.ItUtils.projectDir;

public class SecurityMeasureTest {

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a";
  private static final String SUB_MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1";
  private static final String DIRECTORY = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1";
  private static final String FILE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo";

  private static final String VULNERABILITIES_METRIC = "vulnerabilities";
  private static final String SECURITY_REMEDIATION_EFFORT_METRIC = "security_remediation_effort";
  private static final String SECURITY_RATING_METRIC = "security_rating";

  private static final String[] METRICS = new String[] {VULNERABILITIES_METRIC, SECURITY_REMEDIATION_EFFORT_METRIC, SECURITY_RATING_METRIC};

  @ClassRule
  public static Orchestrator orchestrator = QualityModelSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void init() {
    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
  }

  @Test
  public void verify_security_measures_when_vulnerability_rules_activated() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityModel/with-many-rules.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "with-many-rules");
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertMeasures(PROJECT, 4, 340, 5);
    assertMeasures(MODULE, 2, 170, 5);
    assertMeasures(SUB_MODULE, 1, 85, 5);
    assertMeasures(DIRECTORY, 0, 0, 1);
    assertMeasures(FILE, 0, 0, 1);
  }

  @Test
  public void verify_security_measures_when_no_vulnerability_rule() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityModel/without-type-vulnerability.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "without-type-vulnerability");
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertMeasures(PROJECT, 0, 0, 1);
  }

  private void assertMeasures(String componentKey, int expectedVulnerabilities, int expectedReliabilityRemediationEffort, int expectedReliabilityRating) {
    Map<String, Measures.Measure> measures = getMeasuresByMetricKey(orchestrator, componentKey, METRICS);
    assertThat(parseDouble(measures.get(VULNERABILITIES_METRIC).getValue())).isEqualTo(expectedVulnerabilities);
    assertThat(parseDouble(measures.get(SECURITY_REMEDIATION_EFFORT_METRIC).getValue())).isEqualTo(expectedReliabilityRemediationEffort);
    assertThat(parseDouble(measures.get(SECURITY_RATING_METRIC).getValue())).isEqualTo(expectedReliabilityRating);
  }
}
