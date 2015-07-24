/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package analysis.suite.testing;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static analysis.suite.AnalysisTestSuite.ORCHESTRATOR;
import static util.ItUtils.projectDir;

public class CoverageTrackingTest {

  @ClassRule
  public static Orchestrator orchestrator = ORCHESTRATOR;

  @Before
  public void delete_data() {
    orchestrator.resetData();
  }

  @Test
  public void test_coverage_per_test() throws Exception {
    orchestrator.executeBuilds(SonarRunner.create(projectDir("testing/xoo-sample-with-coverage-per-test")));

    String tests = orchestrator.getServer().adminWsClient().get("api/tests/list", "testFileKey", "sample-with-tests:src/test/xoo/sample/SampleTest.xoo");
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/testing/suite/CoverageTrackingTest/tests-expected.json"), "UTF-8"), tests, false);

    String covered_files = orchestrator.getServer().adminWsClient()
      .get("api/tests/covered_files", "testUuid", extractSuccessfulTestUuid(tests));
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/testing/suite/CoverageTrackingTest/covered_files-expected.json"), "UTF-8"), covered_files, false);
  }

  private String extractSuccessfulTestUuid(String json) {
    Matcher jsonObjectMatcher = Pattern.compile(".*\\{((.*?)success(.*?))\\}.*", Pattern.MULTILINE).matcher(json);
    jsonObjectMatcher.find();

    Matcher uuidMatcher = Pattern.compile(".*\"testUuid\"\\s*?:\\s*?\"(\\S*?)\".*", Pattern.MULTILINE).matcher(jsonObjectMatcher.group(1));
    return uuidMatcher.find() ? uuidMatcher.group(1) : "";
  }
}
