/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.tests.sourceCode;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category1Suite;
import util.selenium.Selenese;

import static util.ItUtils.runProjectAnalysis;

public class HighlightingTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void highlight_source_code_and_symbols_usage() {
    runProjectAnalysis(orchestrator, "highlighting/xoo-sample-with-highlighting-v2");

    // SONAR-3893 & SONAR-4247
    Selenese.runSelenese(orchestrator, "/sourceCode/HighlightingTest/syntax-highlighting.html");

    // SONAR-4249 & SONAR-4250
    Selenese.runSelenese(orchestrator, "/sourceCode/HighlightingTest/symbol-usages-highlighting.html");
  }

  // Check that E/S index is updated when file content is unchanged but plugin generates different syntax/symbol highlighting
  @Test
  public void update_highlighting_even_when_code_unchanged() {
    runProjectAnalysis(orchestrator, "highlighting/xoo-sample-with-highlighting-v1");

    Selenese.runSelenese(orchestrator, "/sourceCode/HighlightingTest/syntax-highlighting-v1.html");

    runProjectAnalysis(orchestrator, "highlighting/xoo-sample-with-highlighting-v2");

    Selenese.runSelenese(orchestrator, "/sourceCode/HighlightingTest/syntax-highlighting-v2.html");
    Selenese.runSelenese(orchestrator, "/sourceCode/HighlightingTest/symbol-usages-highlighting.html");
  }
}
