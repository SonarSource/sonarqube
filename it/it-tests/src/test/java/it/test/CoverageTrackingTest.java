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
package it.test;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import it.Category2Suite;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.skyscreamer.jsonassert.JSONAssert;
import util.QaOnly;

import static util.ItUtils.projectDir;

@Category(QaOnly.class)
public class CoverageTrackingTest {

  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  @Before
  public void delete_data() {
    orchestrator.resetData();
  }

  @Test
  public void test_coverage_per_test() throws Exception {
    orchestrator.executeBuilds(SonarRunner.create(projectDir("testing/xoo-sample-with-coverage-per-test")));

    String tests = orchestrator.getServer().adminWsClient().get("api/tests/list", "testFileKey", "sample-with-tests:src/test/xoo/sample/SampleTest.xoo");
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/CoverageTrackingTest/tests-expected.json"), "UTF-8"), tests, false);

    String covered_files = orchestrator.getServer().adminWsClient()
      .get("api/tests/covered_files", "testId", extractSuccessfulTestId(tests));
    JSONAssert
      .assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/CoverageTrackingTest/covered_files-expected.json"), "UTF-8"), covered_files, false);
  }

  private String extractSuccessfulTestId(String json) {
    Matcher jsonObjectMatcher = Pattern.compile(".*\\{((.*?)success(.*?))\\}.*", Pattern.MULTILINE).matcher(json);
    jsonObjectMatcher.find();

    Matcher idMatcher = Pattern.compile(".*\"id\"\\s*?:\\s*?\"(\\S*?)\".*", Pattern.MULTILINE).matcher(jsonObjectMatcher.group(1));
    return idMatcher.find() ? idMatcher.group(1) : "";
  }
}
