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
import com.codeborne.selenide.SelenideElement;

public class ProjectAnalysisItem {

  private final SelenideElement elt;

  public ProjectAnalysisItem(SelenideElement elt) {
    this.elt = elt;
  }

  public ProjectAnalysisItem shouldHaveEventWithText(String text) {
    elt.find(".project-activity-events").shouldHave(Condition.text(text));
    return this;
  }

  public ProjectAnalysisItem shouldHaveDeleteButton() {
    elt.find(".js-analysis-actions").click();
    elt.find(".js-delete-analysis").shouldBe(Condition.visible);
    return this;
  }

  public ProjectAnalysisItem shouldNotHaveDeleteButton() {
    elt.find(".js-analysis-actions").click();
    elt.find(".js-delete-analysis").shouldNotBe(Condition.visible);
    return this;
  }

  public void delete() {
    elt.find(".js-analysis-actions").click();
    elt.find(".js-delete-analysis").click();

    SelenideElement modal = Selenide.$(".modal");
    modal.shouldBe(Condition.visible);
    modal.find("button[type=\"submit\"]").click();

    elt.shouldNotBe(Condition.visible);
  }

  public ProjectAnalysisItem addCustomEvent(String name) {
    elt.find(".js-analysis-actions").click();
    elt.find(".js-add-event").click();

    SelenideElement modal = Selenide.$(".modal");
    modal.shouldBe(Condition.visible);
    modal.find("input").setValue(name);
    modal.find("button[type=\"submit\"]").click();

    elt.find(".project-activity-event:first-child").shouldHave(Condition.text(name));
    return this;
  }

  public ProjectAnalysisItem changeFirstEvent(String newName) {
    SelenideElement firstEvent = elt.find(".project-activity-event:first-child");
    firstEvent.find(".js-change-event").click();

    SelenideElement modal = Selenide.$(".modal");
    modal.shouldBe(Condition.visible);
    modal.find("input").setValue(newName);
    modal.find("button[type=\"submit\"]").click();

    firstEvent.shouldHave(Condition.text(newName));
    return this;
  }

  public ProjectAnalysisItem deleteFirstEvent() {
    int eventsCount = elt.findAll(".project-activity-event").size();

    SelenideElement firstEvent = elt.find(".project-activity-event:first-child");
    firstEvent.find(".js-delete-event").click();

    SelenideElement modal = Selenide.$(".modal");
    modal.shouldBe(Condition.visible);
    modal.find("button[type=\"submit\"]").click();

    elt.findAll(".project-activity-event").shouldHaveSize(eventsCount - 1);

    return this;
  }
}
