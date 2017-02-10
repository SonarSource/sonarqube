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
package it.sourceCode;

import com.sonar.orchestrator.Orchestrator;
import it.Category1Suite;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static util.ItUtils.runProjectAnalysis;
import static util.selenium.Selenese.runSelenese;

public class EncodingTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  @Test
  public void support_japanese_charset() {
    runProjectAnalysis(orchestrator, "sourceCode/japanese-charset", "sonar.sourceEncoding", "Shift_JIS");

    runSelenese(orchestrator, "/sourceCode/EncodingTest/japanese_sources.html");
  }
}
