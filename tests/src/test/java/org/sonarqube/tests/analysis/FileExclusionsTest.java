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
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category4Suite;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.client.components.TreeRequest;
import util.ItUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;
import static util.ItUtils.newWsClient;

public class FileExclusionsTest {
  static final String PROJECT = "exclusions";

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Before
  public void resetData() {
    orchestrator.resetData();
  }

  @Test
  public void exclude_source_files() {
    scan(
      "sonar.global.exclusions", "**/*Ignore*.xoo",
      "sonar.exclusions", "**/*Exclude*.xoo,src/main/xoo/org/sonar/tests/packageToExclude/**",
      "sonar.test.exclusions", "**/ClassTwoTest.xoo");

    Map<String, Double> measures = getMeasuresAsDouble("ncloc", "files", "directories");
    assertThat(measures.get("files").intValue()).isEqualTo(4);
    assertThat(measures.get("ncloc").intValue()).isEqualTo(60);
    assertThat(measures.get("directories").intValue()).isEqualTo(2);
  }

  /**
   * SONAR-2444 / SONAR-3758
   */
  @Test
  public void exclude_test_files() {
    scan(
      "sonar.global.exclusions", "**/*Ignore*.xoo",
      "sonar.exclusions", "**/*Exclude*.xoo,org/sonar/tests/packageToExclude/**",
      "sonar.test.exclusions", "**/ClassTwoTest.xoo");

    List<Component> testFiles = getComponents("UTS");
    assertThat(testFiles).hasSize(2);
    assertThat(testFiles).extracting(Component::getName).doesNotContain("ClassTwoTest.xoo");
  }

  /**
   * SONAR-1896
   */
  @Test
  public void include_source_files() {
    scan("sonar.inclusions", "**/*One.xoo,**/*Two.xoo");

    assertThat(getMeasuresAsDouble("files").get("files")).isEqualTo(2);

    List<Component> sourceFiles = getComponents("FIL");
    assertThat(sourceFiles).hasSize(2);
    assertThat(sourceFiles).extracting(Component::getName).containsOnly("ClassOne.xoo", "ClassTwo.xoo");
  }

  /**
   * SONAR-1896
   */
  @Test
  public void include_test_files() {
    scan("sonar.test.inclusions", "src/test/xoo/**/*One*.xoo,src/test/xoo/**/*Two*.xoo");

    assertThat(getMeasuresAsDouble("tests").get("tests")).isEqualTo(2);

    List<Component> testFiles = getComponents("UTS");
    assertThat(testFiles).hasSize(2);
    assertThat(testFiles).extracting(Component::getName).containsOnly("ClassOneTest.xoo", "ClassTwoTest.xoo");
  }

  /**
   * SONAR-2760
   */
  @Test
  public void include_and_exclude_files_by_absolute_path() {
    scan(
      // includes everything except ClassOnDefaultPackage
      "sonar.inclusions", "file:**/src/main/xoo/org/**/*.xoo",

      // exclude ClassThree and ClassToExclude
      "sonar.exclusions", "file:**/src/main/xoo/org/**/packageToExclude/*.xoo,file:**/src/main/xoo/org/**/*Exclude.xoo");

    List<Component> sourceFiles = getComponents("FIL");
    assertThat(sourceFiles).hasSize(4);
    assertThat(sourceFiles).extracting(Component::getName).containsOnly("ClassOne.xoo", "ClassToIgnoreGlobally.xoo", "ClassTwo.xoo", "NoSonarComment.xoo");
  }

  static Map<String, Double> getMeasuresAsDouble(String... metricKeys) {
    return getMeasuresAsDoubleByMetricKey(orchestrator, PROJECT, metricKeys);
  }

  private void scan(String... properties) {
    SonarScanner build = SonarScanner
      .create(ItUtils.projectDir("exclusions/exclusions"))
      .setProperties(properties);
    orchestrator.executeBuild(build);
  }

  public static List<Component> getComponents(String qualifier) {
    return newWsClient(orchestrator).components().tree(new TreeRequest().setComponent(PROJECT).setQualifiers(singletonList(qualifier))).getComponentsList();
  }
}
