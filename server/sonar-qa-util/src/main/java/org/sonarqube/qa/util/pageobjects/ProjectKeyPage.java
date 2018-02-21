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
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class ProjectKeyPage {

  public ProjectKeyPage() {
    $("#project-key").should(Condition.exist);
  }

  public ProjectKeyPage assertSimpleUpdate() {
    $("#update-key-new-key").shouldBe(visible);
    $("#update-key-submit").shouldBe(visible);
    return this;
  }

  public ProjectKeyPage trySimpleUpdate(String newKey) {
    $("#update-key-new-key").val(newKey);
    $("#update-key-submit").click();
    $(".modal").shouldBe(visible);
    $(".modal button[type=\"submit\"]").click();
    return this;
  }

  public ProjectKeyPage openFineGrainedUpdate() {
    $("#update-key-tab-fine").click();
    $("#project-key-fine-grained-update").shouldBe(visible);
    return this;
  }

  public ProjectKeyPage tryFineGrainedUpdate(String key, String newKey) {
    SelenideElement form = $(".js-fine-grained-update[data-key=\"" + key + "\"]");
    form.shouldBe(visible);

    form.$("input").val(newKey);
    form.$("button").click();

    $(".modal").shouldBe(visible);
    $(".modal button[type=\"submit\"]").click();
    return this;
  }

  public ProjectKeyPage assertBulkChange() {
    $("#bulk-update-replace").shouldBe(visible);
    $("#bulk-update-by").shouldBe(visible);
    $("#bulk-update-see-results").shouldBe(visible);
    return this;
  }

  public ProjectKeyPage simulateBulkChange(String replace, String by) {
    $("#bulk-update-replace").val(replace);
    $("#bulk-update-by").val(by);
    $("#bulk-update-see-results").click();

    $("#bulk-update-simulation").shouldBe(visible);
    return this;
  }

  public ProjectKeyPage assertBulkChangeSimulationResult(String oldKey, String newKey) {
    SelenideElement row = $("#bulk-update-results").$("[data-key=\"" + oldKey + "\"]");
    row.$(".js-old-key").should(Condition.text(oldKey));
    row.$(".js-new-key").should(Condition.text(newKey));
    return this;
  }

  public ProjectKeyPage assertDuplicated(String oldKey) {
    SelenideElement row = $("#bulk-update-results").$("[data-key=\"" + oldKey + "\"]");
    row.$(".js-new-key").$(".badge-danger").shouldBe(visible);
    return this;
  }

  public ProjectKeyPage confirmBulkUpdate() {
    $("#bulk-update-confirm").click();
    return this;
  }

  public ProjectKeyPage assertSuccessfulBulkUpdate() {
    $(".process-spinner")
      .shouldBe(visible)
      .shouldHave(Condition.text("The key has successfully been updated for all required resources"));
    return this;
  }
}
