/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package it.test;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import it.Category2Suite;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class CoverageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  private static final String[] ALL_COVERAGE_METRICS = new String[] {
    "line_coverage", "lines_to_cover", "uncovered_lines", "branch_coverage", "conditions_to_cover", "uncovered_conditions", "coverage",
    "it_line_coverage", "it_lines_to_cover", "it_uncovered_lines", "it_branch_coverage", "it_conditions_to_cover", "it_uncovered_conditions", "it_coverage",
    "overall_line_coverage", "overall_lines_to_cover", "overall_uncovered_lines", "overall_branch_coverage", "overall_conditions_to_cover", "overall_uncovered_conditions",
    "overall_coverage"
  };

  @Before
  public void delete_data() {
    orchestrator.resetData();
  }

  @Test
  public void unit_test_coverage() throws Exception {
    orchestrator.executeBuilds(SonarRunner.create(projectDir("testing/xoo-sample-ut-coverage")));

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample-ut-coverage", ALL_COVERAGE_METRICS));
    assertThat(project.getMeasureValue("line_coverage")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("lines_to_cover")).isEqualTo(4);
    assertThat(project.getMeasureValue("uncovered_lines")).isEqualTo(2);
    assertThat(project.getMeasureValue("branch_coverage")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("conditions_to_cover")).isEqualTo(2);
    assertThat(project.getMeasureValue("uncovered_conditions")).isEqualTo(1);
    assertThat(project.getMeasureValue("coverage")).isEqualTo(50.0);

    assertThat(project.getMeasureValue("it_coverage")).isNull();

    assertThat(project.getMeasureValue("overall_coverage")).isNull();

    String coverage = orchestrator.getServer().adminWsClient().get("api/sources/lines", "key", "sample-ut-coverage:src/main/xoo/sample/Sample.xoo");
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/CoverageTest/unit_test_coverage-expected.json"), "UTF-8"), coverage, false);

    verifyComputeEngineTempDirIsEmpty();
  }

  @Test
  public void unit_test_coverage_no_condition() throws Exception {
    orchestrator.executeBuilds(SonarRunner.create(projectDir("testing/xoo-sample-ut-coverage-no-condition")));

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample-ut-coverage", ALL_COVERAGE_METRICS));
    assertThat(project.getMeasureValue("line_coverage")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("lines_to_cover")).isEqualTo(4);
    assertThat(project.getMeasureValue("uncovered_lines")).isEqualTo(2);
    assertThat(project.getMeasureValue("branch_coverage")).isNull();
    assertThat(project.getMeasureValue("conditions_to_cover")).isNull();
    assertThat(project.getMeasureValue("uncovered_conditions")).isNull();
    assertThat(project.getMeasureValue("coverage")).isEqualTo(50.0);

    assertThat(project.getMeasureValue("it_coverage")).isNull();

    assertThat(project.getMeasureValue("overall_coverage")).isNull();

    String coverage = orchestrator.getServer().adminWsClient().get("api/sources/lines", "key", "sample-ut-coverage:src/main/xoo/sample/Sample.xoo");
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/CoverageTest/unit_test_coverage_no_condition-expected.json"), "UTF-8"), coverage,
      false);

    verifyComputeEngineTempDirIsEmpty();
  }

  @Test
  public void it_coverage() throws Exception {
    orchestrator.executeBuilds(SonarRunner.create(projectDir("testing/xoo-sample-it-coverage")));

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample-it-coverage", ALL_COVERAGE_METRICS));
    assertThat(project.getMeasureValue("coverage")).isNull();

    assertThat(project.getMeasureValue("it_line_coverage")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("it_lines_to_cover")).isEqualTo(4);
    assertThat(project.getMeasureValue("it_uncovered_lines")).isEqualTo(2);
    assertThat(project.getMeasureValue("it_branch_coverage")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("it_conditions_to_cover")).isEqualTo(2);
    assertThat(project.getMeasureValue("it_uncovered_conditions")).isEqualTo(1);
    assertThat(project.getMeasureValue("it_coverage")).isEqualTo(50.0);

    assertThat(project.getMeasureValue("overall_coverage")).isNull();

    String coverage = orchestrator.getServer().adminWsClient().get("api/sources/lines", "key", "sample-it-coverage:src/main/xoo/sample/Sample.xoo");
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/CoverageTest/it_coverage-expected.json"), "UTF-8"), coverage, false);

    verifyComputeEngineTempDirIsEmpty();
  }

  @Test
  public void ut_and_it_coverage() throws Exception {
    orchestrator.executeBuilds(SonarRunner.create(projectDir("testing/xoo-sample-overall-coverage")));

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample-overall-coverage", ALL_COVERAGE_METRICS));
    assertThat(project.getMeasureValue("line_coverage")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("lines_to_cover")).isEqualTo(4);
    assertThat(project.getMeasureValue("uncovered_lines")).isEqualTo(2);
    assertThat(project.getMeasureValue("branch_coverage")).isEqualTo(25.0);
    assertThat(project.getMeasureValue("conditions_to_cover")).isEqualTo(4);
    assertThat(project.getMeasureValue("uncovered_conditions")).isEqualTo(3);
    assertThat(project.getMeasureValue("coverage")).isEqualTo(37.5);

    assertThat(project.getMeasureValue("it_line_coverage")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("it_lines_to_cover")).isEqualTo(4);
    assertThat(project.getMeasureValue("it_uncovered_lines")).isEqualTo(2);
    assertThat(project.getMeasureValue("it_branch_coverage")).isEqualTo(25.0);
    assertThat(project.getMeasureValue("it_conditions_to_cover")).isEqualTo(4);
    assertThat(project.getMeasureValue("it_uncovered_conditions")).isEqualTo(3);
    assertThat(project.getMeasureValue("it_coverage")).isEqualTo(37.5);

    assertThat(project.getMeasureValue("overall_line_coverage")).isEqualTo(75.0);
    assertThat(project.getMeasureValue("overall_lines_to_cover")).isEqualTo(4);
    assertThat(project.getMeasureValue("overall_uncovered_lines")).isEqualTo(1);
    assertThat(project.getMeasureValue("overall_branch_coverage")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("overall_conditions_to_cover")).isEqualTo(4);
    assertThat(project.getMeasureValue("overall_uncovered_conditions")).isEqualTo(2);
    assertThat(project.getMeasureValue("overall_coverage")).isEqualTo(62.5);

    String coverage = orchestrator.getServer().adminWsClient().get("api/sources/lines", "key", "sample-overall-coverage:src/main/xoo/sample/Sample.xoo");
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/CoverageTest/ut_and_it_coverage-expected.json"), "UTF-8"), coverage, false);

    verifyComputeEngineTempDirIsEmpty();
  }

  /**
   * SONAR-766
   */
  @Test
  public void should_compute_coverage_on_project() {
    orchestrator.executeBuilds(SonarRunner.create(projectDir("testing/xoo-half-covered")));

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("xoo-half-covered", ALL_COVERAGE_METRICS));
    assertThat(project.getMeasureValue("coverage")).isEqualTo(50.0);

    verifyComputeEngineTempDirIsEmpty();
  }

  /**
   * SONAR-766
   */
  @Test
  public void should_ignore_coverage_on_full_path() {
    orchestrator.executeBuilds(SonarRunner.create(projectDir("testing/xoo-half-covered"))
      .setProperty("sonar.coverage.exclusions", "src/main/xoo/org/sonar/tests/halfcovered/UnCovered.xoo"));

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("xoo-half-covered", ALL_COVERAGE_METRICS));
    assertThat(project.getMeasureValue("coverage")).isEqualTo(100.0);

    verifyComputeEngineTempDirIsEmpty();
  }

  /**
   * SONAR-766
   */
  @Test
  public void should_ignore_coverage_on_pattern() {
    orchestrator.executeBuilds(SonarRunner.create(projectDir("testing/xoo-half-covered"))
      .setProperty("sonar.coverage.exclusions", "**/UnCovered*"));

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("xoo-half-covered", ALL_COVERAGE_METRICS));
    assertThat(project.getMeasureValue("coverage")).isEqualTo(100.0);

    verifyComputeEngineTempDirIsEmpty();
  }

  /**
   * SONAR-766
   */
  @Test
  public void should_not_have_coverage_at_all() {
    orchestrator.executeBuilds(SonarRunner.create(projectDir("testing/xoo-half-covered"))
      .setProperty("sonar.coverage.exclusions", "**/*"));

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("xoo-half-covered", ALL_COVERAGE_METRICS));
    assertThat(project.getMeasureValue("coverage")).isNull();

    verifyComputeEngineTempDirIsEmpty();
  }

  private void verifyComputeEngineTempDirIsEmpty() {
    File ceTempDirectory = new File(new File(orchestrator.getServer().getHome(), "temp"), "ce");
    assertThat(FileUtils.listFiles(ceTempDirectory, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)).isEmpty();
  }

}
