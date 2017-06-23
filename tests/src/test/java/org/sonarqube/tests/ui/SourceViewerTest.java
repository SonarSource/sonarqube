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
package org.sonarqube.tests.ui;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category4Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.pageobjects.Navigation;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static util.ItUtils.projectDir;

public class SourceViewerTest {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Category4Suite.ORCHESTRATOR;

  private Navigation nav = Navigation.create(ORCHESTRATOR);

  @BeforeClass
  public static void beforeClass() {
    ORCHESTRATOR.resetData();
    analyzeSampleProject();
  }

  @Test
  public void line_permalink() {
    nav.open("/component?id=sample%3Asrc%2Fmain%2Fxoo%2Fsample%2FSample.xoo&line=6");
    $(".source-line").should(exist);
    $(".source-line-highlighted[data-line-number=\"6\"]").should(exist);
  }

  private static void analyzeSampleProject() {
    ORCHESTRATOR.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));
  }
}
