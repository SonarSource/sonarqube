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
package it.highlighting;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category4Suite;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import util.ItUtils;

public class HighlightingTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  // Check that E/S index is updated when file content is unchanged but plugin generates different syntax/symbol highlighting
  @Test
  public void updateHighlightingEvenWhenCodeUnchanged() throws Exception {
    SonarScanner runner = SonarScanner.create(ItUtils.projectDir("highlighting/xoo-sample-with-highlighting-v1"));
    orchestrator.executeBuild(runner);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("syntax-highlighting-v1",
      "/highlighting/HighlightingTest/syntax-highlighting-v1.html").build();
    orchestrator.executeSelenese(selenese);

    runner = SonarScanner.create(ItUtils.projectDir("highlighting/xoo-sample-with-highlighting-v2"));
    orchestrator.executeBuild(runner);

    selenese = Selenese.builder().setHtmlTestsInClasspath("syntax-highlighting-v2",
      "/highlighting/HighlightingTest/syntax-highlighting-v2.html",
      "/highlighting/HighlightingTest/symbol-usages-highlighting.html").build();
    orchestrator.executeSelenese(selenese);
  }
}
