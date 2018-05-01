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
package org.sonarqube.tests.ui;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.DocumentationPage;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;

public class DocumentationTest {

  @ClassRule
  public static Orchestrator orchestrator = UiSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void documentation_loads() {
    tester.openBrowser().openDocumentation().shouldHaveLinks();
  }

  @Test
  public void can_switch_between_content() {
    DocumentationPage page = tester.openBrowser().openDocumentation().selectLink(0);
    String content = page.getDocumentationContent();
    page.selectLink(1);
    assertThat(content).isNotEqualTo(page.getDocumentationContent());
  }

  @Test
  public void display_not_found() {
    tester.openBrowser().open("/documentation/foobarbaz");
    $("#nonav").shouldBe(visible);
  }

}
