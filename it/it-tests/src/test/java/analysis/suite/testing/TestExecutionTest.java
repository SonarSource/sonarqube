/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package analysis.suite.testing;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static analysis.suite.AnalysisTestSuite.ORCHESTRATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class TestExecutionTest {

  @ClassRule
  public static Orchestrator orchestrator = ORCHESTRATOR;

  @Before
  public void delete_data() {
    orchestrator.resetData();
  }

  @Test
  public void test_execution() throws Exception {
    orchestrator.executeBuilds(SonarRunner.create(projectDir("testing/xoo-sample-with-tests-execution")));

    Resource project = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics("sample-with-tests", "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time"));
    assertThat(project.getMeasureValue("test_success_density")).isEqualTo(50.0);
    assertThat(project.getMeasureIntValue("test_failures")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("test_errors")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(4);
    assertThat(project.getMeasureIntValue("skipped_tests")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("test_execution_time")).isEqualTo(8);

    String json = orchestrator.getServer().adminWsClient().get("api/tests/list", "testFileKey", "sample-with-tests:src/test/xoo/sample/SampleTest.xoo");
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/testing/suite/TestExecutionTest/expected.json"), "UTF-8"), json, false);
  }
}
