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
package pageobjects;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class ProjectAnalysisItem {

  private final SelenideElement elt;

  public ProjectAnalysisItem(SelenideElement elt) {
    this.elt = elt;
  }

  public void delete() {
    this.elt.find(".js-delete-analysis").click();

    SelenideElement modal = $(".modal");
    modal.shouldBe(visible);
    modal.find("button[type=\"submit\"]").click();

    this.elt.shouldNotBe(visible);
  }

  public ProjectAnalysisItem addCustomEvent(String name) {
    this.elt.find(".js-add-event").click();

    SelenideElement modal = $(".modal");
    modal.shouldBe(visible);
    modal.find("input").setValue(name);
    modal.find("button[type=\"submit\"]").click();

    this.elt.find(".project-activity-event:last-child").shouldHave(text(name));

    return this;
  }

  public ProjectAnalysisItem changeLastEvent(String newName) {
    SelenideElement lastEvent = this.elt.find(".project-activity-event:last-child");
    lastEvent.find(".js-change-event").click();

    SelenideElement modal = $(".modal");
    modal.shouldBe(visible);
    modal.find("input").setValue(newName);
    modal.find("button[type=\"submit\"]").click();

    lastEvent.shouldHave(text(newName));

    return this;
  }

  public ProjectAnalysisItem deleteLastEvent() {
    int eventsCount = this.elt.findAll(".project-activity-event").size();

    SelenideElement lastEvent = this.elt.find(".project-activity-event:last-child");
    lastEvent.find(".js-delete-event").click();

    SelenideElement modal = $(".modal");
    modal.shouldBe(visible);
    modal.find("button[type=\"submit\"]").click();

    this.elt.findAll(".project-activity-event").shouldHaveSize(eventsCount - 1);

    return this;
  }
}
