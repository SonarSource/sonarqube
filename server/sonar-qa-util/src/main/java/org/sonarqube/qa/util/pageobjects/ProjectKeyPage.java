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

public class ProjectKeyPage {

  public ProjectKeyPage() {
    Selenide.$("#project-key").should(Condition.exist);
  }

  public ProjectKeyPage assertSimpleUpdate() {
    Selenide.$("#update-key-new-key").shouldBe(Condition.visible);
    Selenide.$("#update-key-submit").shouldBe(Condition.visible);
    return this;
  }

  public ProjectKeyPage trySimpleUpdate(String newKey) {
    Selenide.$("#update-key-new-key").val(newKey);
    Selenide.$("#update-key-submit").click();
    Selenide.$("#update-key-confirm").click();
    return this;
  }

  public ProjectKeyPage openFineGrainedUpdate() {
    Selenide.$("#update-key-tab-fine").click();
    Selenide.$("#project-key-fine-grained-update").shouldBe(Condition.visible);
    return this;
  }

  public ProjectKeyPage tryFineGrainedUpdate(String key, String newKey) {
    SelenideElement form = Selenide.$(".js-fine-grained-update[data-key=\"" + key + "\"]");
    form.shouldBe(Condition.visible);

    form.$("input").val(newKey);
    form.$("button").click();

    Selenide.$("#update-key-confirm").click();
    return this;
  }

  public ProjectKeyPage assertBulkChange() {
    Selenide.$("#bulk-update-replace").shouldBe(Condition.visible);
    Selenide.$("#bulk-update-by").shouldBe(Condition.visible);
    Selenide.$("#bulk-update-see-results").shouldBe(Condition.visible);
    return this;
  }

  public ProjectKeyPage simulateBulkChange(String replace, String by) {
    Selenide.$("#bulk-update-replace").val(replace);
    Selenide.$("#bulk-update-by").val(by);
    Selenide.$("#bulk-update-see-results").click();

    Selenide.$("#bulk-update-simulation").shouldBe(Condition.visible);
    return this;
  }

  public ProjectKeyPage assertBulkChangeSimulationResult(String oldKey, String newKey) {
    SelenideElement row = Selenide.$("#bulk-update-results").$("[data-key=\"" + oldKey + "\"]");
    row.$(".js-old-key").should(Condition.text(oldKey));
    row.$(".js-new-key").should(Condition.text(newKey));
    return this;
  }

  public ProjectKeyPage assertDuplicated(String oldKey) {
    SelenideElement row = Selenide.$("#bulk-update-results").$("[data-key=\"" + oldKey + "\"]");
    row.$(".js-new-key").$(".badge-danger").shouldBe(Condition.visible);
    return this;
  }

  public ProjectKeyPage confirmBulkUpdate() {
    Selenide.$("#bulk-update-confirm").click();
    return this;
  }

  public ProjectKeyPage assertSuccessfulBulkUpdate() {
    Selenide.$(".process-spinner")
      .shouldBe(Condition.visible)
      .shouldHave(Condition.text("The key has successfully been updated for all required resources"));
    return this;
  }
}
