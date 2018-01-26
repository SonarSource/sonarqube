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
import java.util.Locale;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class RuleDetails {

  public RuleDetails shouldHaveType(String type) {
    $$(".coding-rules-detail-property").findBy(text(type)).shouldBe(visible);
    return this;
  }

  public RuleDetails shouldHaveSeverity(String severity) {
    $(".coding-rules-detail-property .icon-severity-" + severity.toLowerCase(Locale.ENGLISH)).shouldBe(visible);
    return this;
  }

  public RuleDetails shouldHaveNoTags() {
    $(".coding-rules-detail-tag-list").shouldHave(text("No tags"));
    return this;
  }

  public RuleDetails shouldHaveDescription(String description) {
    $(".js-rule-description").shouldHave(text(description));
    return this;
  }

  public RuleDetails shouldBeActivatedOn(String profileName) {
    $("#coding-rules-detail-quality-profiles").shouldHave(text(profileName));
    return this;
  }

  public RuleDetails shouldHaveTotalIssues(int issues) {
    $(".js-rule-issues h3").shouldHave(text(String.valueOf(issues)));
    return this;
  }

  public RuleDetails shouldHaveIssuesOnProject(String projectName, int issues) {
    $(".coding-rules-most-violated-projects").shouldHave(
      Condition.and("", text(projectName), text(String.valueOf(issues))));
    return this;
  }

  public ExtendedDescription extendDescription() {
    return new ExtendedDescription().start();
  }

  public static class ExtendedDescription {
    public ExtendedDescription start() {
      $("#coding-rules-detail-extend-description").click();
      getTextArea().shouldBe(visible);
      return this;
    }

    public ExtendedDescription cancel() {
      $("#coding-rules-detail-extend-description-cancel").click();
      getTextArea().shouldNotBe(visible);
      return this;
    }

    public ExtendedDescription type(String text) {
      getTextArea().val(text);
      return this;
    }

    public ExtendedDescription submit() {
      $("#coding-rules-detail-extend-description-submit").click();
      getTextArea().shouldNotBe(visible);
      return this;
    }

    public ExtendedDescription remove() {
      $("#coding-rules-detail-extend-description-remove").click();
      $("button[data-confirm=\"yes\"").click();
      getTextArea().shouldNotBe(visible);
      return this;
    }

    private SelenideElement getTextArea() {
      return $("#coding-rules-detail-extend-description-text");
    }
  }

}
