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

public class RuleDetails {
  RuleDetails() {
    $(".coding-rule-details").shouldBe(visible);
  }

  public RuleDetails shouldHaveType(String type) {
    $(".coding-rules-detail-property[data-meta=\"type\"]").shouldHave(text(type));
    return this;
  }

  public RuleDetails shouldHaveSeverity(String severity) {
    $(".coding-rules-detail-property[data-meta=\"severity\"]").shouldHave(text(severity));
    return this;
  }

  public RuleDetails shouldHaveDescription(String description) {
    $(".js-rule-description").shouldHave(text(description));
    return this;
  }

  public RuleDetails shouldBeActivatedOn(String profileKey) {
    $("#coding-rules-detail-quality-profiles [data-profile=\"" + profileKey + "\"]").shouldBe(visible);
    return this;
  }

  public RuleDetails shouldNotBeActivatedOn(String profileName) {
    $("#coding-rules-detail-quality-profiles").shouldNotHave(text(profileName));
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

  public RuleDetails shouldHaveCustomRule(String ruleKey) {
    takeCustomRule(ruleKey).shouldBe(visible);
    return this;
  }

  public RuleDetails shouldNotHaveCustomRule(String ruleKey) {
    takeCustomRule(ruleKey).shouldNotBe(visible);
    return this;
  }

  public RuleDetails createCustomRule(String ruleName) {
    $(".js-create-custom-rule").click();
    modal().shouldBe(visible);

    $("#coding-rules-custom-rule-creation-name").val(ruleName);
    $("#coding-rules-custom-rule-creation-html-description").val("description");
    $("#coding-rules-custom-rule-creation-create").click();

    modal().shouldNotBe(visible);
    return this;
  }

  public RuleDetails reactivateCustomRule(String ruleName) {
    $(".js-create-custom-rule").click();
    modal().shouldBe(visible);

    $("#coding-rules-custom-rule-creation-name").val(ruleName);
    $("#coding-rules-custom-rule-creation-html-description").val("description");
    $("#coding-rules-custom-rule-creation-create").click();

    modal().find(".alert-warning").shouldBe(visible);
    $("#coding-rules-custom-rule-creation-reactivate").click();

    modal().shouldNotBe(visible);
    return this;
  }

  public RuleDetails deleteCustomRule(String ruleKey) {
    takeCustomRule(ruleKey).$(".js-delete-custom-rule").click();
    modal().shouldBe(visible);
    modal().find("button").click();
    modal().shouldNotBe(visible);
    return this;
  }

  public RuleActivation activate() {
    $("#coding-rules-quality-profile-activate").click();
    modal().shouldBe(visible);
    return new RuleActivation();
  }

  private static SelenideElement modal() {
    return $(".modal");
  }

  private static SelenideElement takeCustomRule(String ruleKey) {
    return $("#coding-rules-detail-custom-rules tr[data-rule=\"" + ruleKey + "\"]");
  }

  private static SelenideElement getActiveProfileElement(String profileKey) {
    return $("#coding-rules-detail-quality-profiles [data-profile=\"" + profileKey + "\"]");
  }

  public ExtendedDescription extendDescription() {
    return new ExtendedDescription().start();
  }

  public Tags tags() {
    return new Tags();
  }

  public RuleActivation changeActivationOn(String profileKey) {
    getActiveProfileElement(profileKey).$(".coding-rules-detail-quality-profile-change").click();
    modal().shouldBe(visible);
    return new RuleActivation();
  }

  public RuleDetails activationShouldHaveParameter(String profileKey, String parameter, String value) {
    getActiveProfileElement(profileKey).$$(".coding-rules-detail-quality-profile-parameter")
      .findBy(Condition.and("", text(parameter), text(value)))
      .shouldBe(visible);
    return this;
  }

  public RuleDetails activationShouldHaveSeverity(String profileKey, String severity) {
    getActiveProfileElement(profileKey).$(".coding-rules-detail-quality-profile-severity .icon-severity-" + severity.toLowerCase(Locale.ENGLISH)).shouldBe(visible);
    return this;
  }

  public RuleDetails revertActivationToParentDefinition(String profileKey) {
    getActiveProfileElement(profileKey).$(".coding-rules-detail-quality-profile-revert").click();
    modal().shouldBe(visible);
    $(".modal button").click();
    modal().shouldNotBe(visible);
    return this;
  }

  public static class ExtendedDescription {
    public ExtendedDescription start() {
      $("#coding-rules-detail-extend-description").click();
      textArea().shouldBe(visible);
      return this;
    }

    public ExtendedDescription cancel() {
      $("#coding-rules-detail-extend-description-cancel").click();
      textArea().shouldNotBe(visible);
      return this;
    }

    public ExtendedDescription type(String text) {
      textArea().val(text);
      return this;
    }

    public ExtendedDescription submit() {
      $("#coding-rules-detail-extend-description-submit").click();
      textArea().shouldNotBe(visible);
      return this;
    }

    public ExtendedDescription remove() {
      $("#coding-rules-detail-extend-description-remove").click();
      modal().shouldBe(visible);
      $("#coding-rules-detail-extend-description-remove-submit").click();
      modal().shouldNotBe(visible);
      textArea().shouldNotBe(visible);
      return this;
    }

    private static SelenideElement textArea() {
      return $("#coding-rules-detail-extend-description-text");
    }
  }

  public static class Tags {
    public Tags shouldHaveNoTags() {
      element().shouldHave(text("No tags"));
      return this;
    }

    public Tags shouldHaveTags(String... tags) {
      for (String tag : tags) {
        element().shouldHave(text(tag));
      }
      return this;
    }

    public Tags edit() {
      element().$("button").click();
      return this;
    }

    public Tags select(String tag) {
      element().$$(".menu a").findBy(text(tag)).click();
      return this;
    }

    public Tags search(String query) {
      element().$(".search-box-input").val(query);
      return this;
    }

    public Tags done() {
      element().$(".search-box-input").pressEscape();
      return this;
    }

    private static SelenideElement element() {
      return $(".coding-rules-detail-property[data-meta=\"tags\"]");
    }
  }

  public static class RuleActivation {
    public RuleActivation select(String profileKey) {
      $(".modal .js-profile .Select-input input").val(profileKey).pressEnter();
      return this;
    }

    public RuleActivation fill(String parameter, String value) {
      $(".modal-field input[name=\"" + parameter + "\"]").val(value);
      return this;
    }

    public RuleActivation save() {
      $(".modal button").click();
      modal().shouldNotBe(visible);
      return this;
    }
  }
}
