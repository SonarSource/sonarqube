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
package org.sonarqube.qa.util.pageobjects;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

public class QualityProfilePage {
  public QualityProfilePage() {
    Selenide.$("#quality-profile").shouldBe(Condition.visible);
  }

  public QualityProfilePage shouldHaveMissingSonarWayRules(Integer nbRules) {
    Selenide.$(".quality-profile-rules-sonarway-missing")
      .shouldBe(Condition.visible)
      .$("a").shouldHave(Condition.text(nbRules.toString()));
    return this;
  }

  public RulesPage showMissingSonarWayRules() {
    Selenide.$(".quality-profile-rules-sonarway-missing")
      .shouldBe(Condition.visible).$("a").click();
    return Selenide.page(RulesPage.class);
  }

  public QualityProfilePage shouldHaveAssociatedProject(String projectName) {
    Selenide.$(".js-profile-project").shouldHave(Condition.text(projectName));
    return this;
  }

  public QualityProfilePage shouldAllowToChangeProjects() {
    Selenide.$(".js-change-projects").shouldBe(Condition.visible).click();
    Selenide.$("#profile-projects .select-list-list-container").shouldBe(Condition.visible);
    return this;
  }

  public QualityProfilePage shouldNotAllowToChangeProjects() {
    Selenide.$(".js-change-projects").shouldNot(Condition.exist);
    return this;
  }

  public QualityProfilePage shouldNotAllowToEdit() {
    Selenide.$("button.dropdown-toggle").should(Condition.exist).click();
    Selenide.$("#quality-profile-rename").shouldNot(Condition.exist);
    Selenide.$("#quality-profile-activate-more-rules").shouldNot(Condition.exist);
    return this;
  }
}
