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
package it.qualityModel;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category2Suite;
import javax.annotation.CheckForNull;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.assertj.core.api.Assertions.assertThat;
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

  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  @Before
  public void init() {
    orchestrator.resetData();

    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
  }

  @Test
  public void verify_security_measures_when_vulnerability_rules_activated() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/qualityModel/with-many-rules.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "with-many-rules");
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertThat(getMeasure(PROJECT, VULNERABILITIES_METRIC).getIntValue()).isEqualTo(4);
    assertThat(getMeasure(PROJECT, SECURITY_REMEDIATION_EFFORT_METRIC).getIntValue()).isEqualTo(340);
    assertThat(getMeasure(PROJECT, SECURITY_RATING_METRIC).getData()).isEqualTo("E");

    assertThat(getMeasure(MODULE, VULNERABILITIES_METRIC).getIntValue()).isEqualTo(2);
    assertThat(getMeasure(MODULE, SECURITY_REMEDIATION_EFFORT_METRIC).getIntValue()).isEqualTo(170);
    assertThat(getMeasure(MODULE, SECURITY_RATING_METRIC).getData()).isEqualTo("E");

    assertThat(getMeasure(SUB_MODULE, VULNERABILITIES_METRIC).getIntValue()).isEqualTo(1);
    assertThat(getMeasure(SUB_MODULE, SECURITY_REMEDIATION_EFFORT_METRIC).getIntValue()).isEqualTo(85);
    assertThat(getMeasure(SUB_MODULE, SECURITY_RATING_METRIC).getData()).isEqualTo("E");

    assertThat(getMeasure(DIRECTORY, VULNERABILITIES_METRIC).getIntValue()).isEqualTo(0);
    assertThat(getMeasure(DIRECTORY, SECURITY_REMEDIATION_EFFORT_METRIC).getIntValue()).isEqualTo(0);
    assertThat(getMeasure(DIRECTORY, SECURITY_RATING_METRIC).getData()).isEqualTo("A");

    assertThat(getMeasure(FILE, VULNERABILITIES_METRIC)).isNull();
    assertThat(getMeasure(FILE, SECURITY_REMEDIATION_EFFORT_METRIC)).isNull();
    assertThat(getMeasure(FILE, SECURITY_RATING_METRIC).getData()).isEqualTo("A");
  }

  @Test
  public void verify_security_measures_when_no_vulnerability_rule() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/qualityModel/without-type-vulnerability.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "without-type-vulnerability");
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertThat(getMeasure(PROJECT, VULNERABILITIES_METRIC).getIntValue()).isEqualTo(0);
    assertThat(getMeasure(PROJECT, SECURITY_REMEDIATION_EFFORT_METRIC).getIntValue()).isEqualTo(0);
    assertThat(getMeasure(PROJECT, SECURITY_RATING_METRIC).getData()).isEqualTo("A");
  }

  @CheckForNull
  private Measure getMeasure(String resource, String metricKey) {
    Resource res = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resource, metricKey));
    if (res == null) {
      return null;
    }
    return res.getMeasure(metricKey);
  }
}
