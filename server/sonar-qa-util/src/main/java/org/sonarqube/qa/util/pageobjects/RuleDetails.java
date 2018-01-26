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

  public RuleDetails shouldHaveCustomRule(String ruleName) {
    takeCustomRuleByName(ruleName).shouldBe(visible);
    return this;
  }

  public RuleDetails shouldNotHaveCustomRule(String ruleName) {
    takeCustomRuleByName(ruleName).shouldNotBe(visible);
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

  public RuleDetails deleteOnlyCustomRule() {
    $$(".js-delete-custom-rule").shouldHaveSize(1).first().click();
    modal().shouldBe(visible);
    modal().find("button").click();
    modal().shouldNotBe(visible);
    return this;
  }

  public RuleDetails openOnlyCustomRule() {
    $$(".coding-rules-detail-list-name a").shouldHaveSize(1).first().click();
    return this;
  }

  public RuleActivation activateOnOnlyProfile() {
    $("#coding-rules-quality-profile-activate").click();
    modal().shouldBe(visible);
    return new RuleActivation();
  }

  private SelenideElement modal() {
    return $(".modal");
  }

  private SelenideElement takeCustomRuleByName(String ruleName) {
    return $$(".coding-rules-detail-list-name").findBy(text(ruleName));
  }

  public ExtendedDescription extendDescription() {
    return new ExtendedDescription().start();
  }

  public Tags tags() {
    return new Tags();
  }

  public EditForm edit() {
    $("#coding-rules-detail-custom-rule-change").click();
    modal().shouldBe(visible);
    return new EditForm();
  }

  public RuleActivation changeOnlyActivation() {
    $$(".coding-rules-detail-quality-profile-change").shouldHaveSize(1).first().click();
    modal().shouldBe(visible);
    return new RuleActivation();
  }

  public RuleDetails onlyActivationShouldHaveParameter(String parameter, String value) {
    $$(".coding-rules-detail-quality-profile-parameter")
      .findBy(Condition.and("", text(parameter), text(value)))
      .shouldBe(visible);
    return this;
  }

  public RuleDetails onlyActivationShouldHaveSeverity(String severity) {
    $(".coding-rules-detail-quality-profile-severity .icon-severity-" + severity.toLowerCase(Locale.ENGLISH)).shouldBe(visible);
    return this;
  }

  public RuleDetails revertOnlyActivationToParentDefinition() {
    $(".coding-rules-detail-quality-profile-revert").click();
    $("button[data-confirm=\"yes\"").click();
    return this;
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

  public static class Tags {
    public Tags shouldHaveTags(String... tags) {
      for (String tag : tags) {
        $(".coding-rules-detail-tag-list").shouldHave(text(tag));
      }
      return this;
    }

    public Tags edit() {
      $(".coding-rules-detail-tags-change").click();
      $(".coding-rules-detail-tag-edit").shouldBe(visible);
      return this;
    }

    public Tags select(String tag) {
      $$(".select2-result-selectable").findBy(text(tag)).click();
      return this;
    }

    public Tags search(String query) {
      $(".coding-rules-detail-tag-input").val(query);
      return this;
    }

    public Tags done() {
      $(".coding-rules-detail-tag-edit-done").click();
      return this;
    }
  }

  public static class EditForm {

    public EditForm changeSeverity(String severity) {
      $(".modal .select2-choice").click();
      $$(".modal .select2-result-selectable").findBy(text(severity)).click();
      return this;
    }

    public EditForm save() {
      $(".coding-rules-custom-rule-creation-create").click();
      $(".modal").shouldNotBe(visible);
      return this;
    }
  }

  public static class RuleActivation {
    public RuleActivation fill(String parameter, String value) {
      $(".modal-field input[name=\"" + parameter + "\"]").val(value);
      return this;
    }

    public RuleActivation save() {
      $("#coding-rules-quality-profile-activation-activate").click();
      $(".modal").shouldNotBe(visible);
      return this;
    }
  }
}
