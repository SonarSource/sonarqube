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
package org.sonarqube.qa.util.pageobjects.settings;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.Selectors.byText;

public class SettingsPage {

  public SettingsPage() {
    Selenide.$("#settings-page").shouldBe(Condition.visible);
  }

  public SettingsPage assertMenuContains(String categoryName) {
    Selenide.$(".side-tabs-menu").$(By.linkText(categoryName)).shouldBe(Condition.visible);
    return this;
  }

  public SettingsPage assertSettingDisplayed(String settingKey) {
    Selenide.$(".settings-definition[data-key='" + settingKey + "']").shouldBe(Condition.visible);
    return this;
  }

  public SettingsPage assertSettingNotDisplayed(String settingKey) {
    Selenide.$(".settings-definition[data-key='" + settingKey + "']").shouldNotBe(Condition.visible);
    return this;
  }

  public SettingsPage openCategory(String categoryName) {
    Selenide.$(".side-tabs-menu").$(By.linkText(categoryName)).click();
    return this;
  }

  public SettingsPage assertStringSettingValue(String settingKey, String value) {
    Selenide.$("input[name=\"settings[" + settingKey + "]\"]").shouldHave(Condition.exactValue(value));
    return this;
  }

  public SettingsPage assertSettingValueIsNotedAsDefault(String settingKey) {
    Selenide.$("[data-key=\"" + settingKey + "\"]").find(".spacer-top.note")
      .shouldBe(Condition.text("(default)"));
    return this;
  }

  public SettingsPage assertBooleanSettingValue(String settingKey, boolean value) {
    SelenideElement toggle = Selenide.$("button[name=\"settings[" + settingKey + "]\"]");
    if (value) {
      toggle.shouldHave(Condition.cssClass("boolean-toggle-on"));
    } else {
      toggle.shouldNotHave(Condition.cssClass("boolean-toggle-on"));
    }
    return this;
  }

  public SettingsPage assertSettingValueCanBeSaved(String settingKey) {
    Selenide.$("[data-key=\"" + settingKey + "\"]").find(byText("Save"))
      .should(Condition.exist)
      .shouldNotBe(Condition.attribute("disabled"));
    return this;
  }

  public SettingsPage assertSettingValueCannotBeSaved(String settingKey) {
    Selenide.$("[data-key=\"" + settingKey + "\"]").find(byText("Save"))
      .should(Condition.exist)
      .shouldBe(Condition.attribute("disabled"));
    return this;
  }

  public SettingsPage assertSettingValueCanBeReset(String settingKey) {
    Selenide.$("[data-key=\"" + settingKey + "\"]").find(byText("Reset"))
      .should(Condition.exist);
    return this;
  }

  public SettingsPage assertSettingValueCanBeCanceled(String settingKey) {
    Selenide.$("[data-key=\"" + settingKey + "\"]").find(byText("Cancel"))
      .should(Condition.exist);
    return this;
  }

  public SettingsPage assertInputCount(String settingKey, int count) {
    Selenide.$("[data-key=\"" + settingKey + "\"]").findAll("input").shouldHaveSize(count);
    return this;
  }

  public SettingsPage changeSettingValue(String settingKey, String value) {
    Selenide.$("[data-key=\"" + settingKey + "\"]").find("input").val(value);
    return this;
  }

  public SettingsPage changeSettingValue(String settingKey, int index, String value) {
    Selenide.$("[data-key=\"" + settingKey + "\"]").findAll("input").get(index).val(value);
    return this;
  }

  public SettingsPage clickOnCancel(String settingKey) {
    click(Selenide.$("[data-key=\"" + settingKey + "\"]").find(byText("Cancel")));
    return this;
  }

  public SettingsPage removeFirstValue(String settingKey) {
    click(Selenide.$("[data-key=\"" + settingKey + "\"]").find(".js-remove-value"));
    return this;
  }

  public SettingsPage sendDeleteKeyToSettingField(String settingKey) {
    Selenide.$("[data-key=\"" + settingKey + "\"]").find("input").sendKeys(Keys.BACK_SPACE);
    return this;
  }

  public SettingsPage setStringValue(String settingKey, String value) {
    SelenideElement setting = Selenide.$(".settings-definition[data-key=\"" + settingKey + "\"]");
    setting.find("input").val(value);
    setting.find(".button-success").click();
    setting.find(".button-success").shouldNot(Condition.exist);
    return this;
  }

  public PropertySetInput getPropertySetInput(String settingKey) {
    SelenideElement setting = Selenide.$(".settings-definition[data-key=\"" + settingKey + "\"]");
    return new PropertySetInput(setting);
  }

  private void click(SelenideElement selenideElement){
    // FIXME Temporary fix to correctly scroll in Firefox 46
    Selenide.executeJavaScript("arguments[0].click()", selenideElement);
  }

}
