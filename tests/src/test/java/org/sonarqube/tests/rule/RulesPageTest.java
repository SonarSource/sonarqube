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
package org.sonarqube.tests.rule;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.pageobjects.RulesPage;
import org.sonarqube.tests.Category2Suite;
import org.sonarqube.tests.Tester;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class RulesPageTest {
  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category2Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(ORCHESTRATOR).disableOrganizations();

  @Test
  public void should_display_rule_profiles() {
    RulesPage page = tester.openBrowser().openRules();
    $(".search-navigator-facet-box[data-property=\"qprofile\"] .js-facet-toggle").click();
    $$(".search-navigator-facet-box[data-property=\"qprofile\"] .js-facet").findBy(text("Basic")).click();
    page.shouldHaveTotalRules(1);
    $$(".js-rule").first().click();
    $("#coding-rules-detail-quality-profiles").shouldHave(text("Basic"));
  }
}
