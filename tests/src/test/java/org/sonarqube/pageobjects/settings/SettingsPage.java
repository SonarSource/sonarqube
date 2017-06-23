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
package org.sonarqube.pageobjects.settings;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exactValue;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class SettingsPage {

  public SettingsPage() {
    $("#settings-page").shouldBe(visible);
  }

  public SettingsPage assertMenuContains(String categoryName) {
    $(".side-tabs-menu").$(By.linkText(categoryName)).shouldBe(visible);
    return this;
  }

  public SettingsPage assertSettingDisplayed(String settingKey) {
    $(".settings-definition[data-key='" + settingKey + "']").shouldBe(visible);
    return this;
  }

  public SettingsPage assertSettingNotDisplayed(String settingKey) {
    $(".settings-definition[data-key='" + settingKey + "']").shouldNotBe(visible);
    return this;
  }

  public SettingsPage openCategory(String categoryName) {
    $(".side-tabs-menu").$(By.linkText(categoryName)).click();
    return this;
  }

  public SettingsPage assertStringSettingValue(String settingKey, String value) {
    $("input[name=\"settings[" + settingKey + "]\"]").shouldHave(exactValue(value));
    return this;
  }

  public SettingsPage assertBooleanSettingValue(String settingKey, boolean value) {
    SelenideElement toggle = $("button[name=\"settings[" + settingKey + "]\"]");
    if (value) {
      toggle.shouldHave(cssClass("boolean-toggle-on"));
    } else {
      toggle.shouldNotHave(cssClass("boolean-toggle-on"));
    }
    return this;
  }

  public SettingsPage setStringValue(String settingKey, String value) {
    SelenideElement setting = $(".settings-definition[data-key=\"" + settingKey + "\"]");
    setting.find("input").val(value);
    setting.find(".js-save-changes").click();
    setting.find(".js-save-changes").shouldNot(exist);
    return this;
  }

  public PropertySetInput getPropertySetInput(String settingKey) {
    SelenideElement setting = $(".settings-definition[data-key=\"" + settingKey + "\"]");
    return new PropertySetInput(setting);
  }
}
