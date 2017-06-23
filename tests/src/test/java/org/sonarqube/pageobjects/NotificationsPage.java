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
package org.sonarqube.pageobjects;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class NotificationsPage extends Navigation {

  private final String EMAIL = "EmailNotificationChannel";

  public NotificationsPage() {
    $("#account-page").shouldHave(text("Overall notifications"));
  }

  public NotificationsPage shouldHaveGlobalNotification(String type) {
    return shouldHaveGlobalNotification(type, EMAIL);
  }

  public NotificationsPage shouldHaveGlobalNotification(String type, String channel) {
    return shouldBeChecked(globalCheckboxSelector(type, channel));
  }

  public NotificationsPage shouldNotHaveGlobalNotification(String type) {
    return shouldNotHaveGlobalNotification(type, EMAIL);
  }

  public NotificationsPage shouldNotHaveGlobalNotification(String type, String channel) {
    return shouldNotBeChecked(globalCheckboxSelector(type, channel));
  }

  public NotificationsPage shouldHaveProjectNotification(String project, String type, String channel) {
    return shouldBeChecked(projectCheckboxSelector(project, type, channel));
  }

  public NotificationsPage shouldNotHaveProjectNotification(String project, String type, String channel) {
    return shouldNotBeChecked(projectCheckboxSelector(project, type, channel));
  }

  public NotificationsPage addGlobalNotification(String type) {
    return addGlobalNotification(type, EMAIL);
  }

  public NotificationsPage addGlobalNotification(String type, String channel) {
    shouldNotHaveGlobalNotification(type, channel);
    toggleCheckbox(globalCheckboxSelector(type, channel));
    shouldHaveGlobalNotification(type, channel);
    return this;
  }

  public NotificationsPage removeGlobalNotification(String type) {
    return removeGlobalNotification(type, EMAIL);
  }

  public NotificationsPage removeGlobalNotification(String type, String channel) {
    shouldHaveGlobalNotification(type, channel);
    toggleCheckbox(globalCheckboxSelector(type, channel));
    shouldNotHaveGlobalNotification(type, channel);
    return this;
  }

  public NotificationsPage addProjectNotification(String project, String type, String channel) {
    shouldNotHaveProjectNotification(project, type, channel);
    toggleCheckbox(projectCheckboxSelector(project, type, channel));
    shouldHaveProjectNotification(project, type, channel);
    return this;
  }

  public NotificationsPage removeProjectNotification(String project, String type, String channel) {
    shouldHaveProjectNotification(project, type, channel);
    toggleCheckbox(projectCheckboxSelector(project, type, channel));
    shouldNotHaveProjectNotification(project, type, channel);
    return this;
  }

  private String globalCheckboxSelector(String type, String channel) {
    return "#global-notification-" + type + "-" + channel;
  }

  private String projectCheckboxSelector(String project, String type, String channel) {
    return "#project-notification-" + project + "-" + type + "-" + channel;
  }

  private NotificationsPage shouldBeChecked(String selector) {
    $(selector)
      .shouldBe(visible)
      .shouldHave(cssClass("icon-checkbox-checked"));
    return this;
  }

  private NotificationsPage shouldNotBeChecked(String selector) {
    $(selector)
      .shouldBe(visible)
      .shouldNotHave(cssClass("icon-checkbox-checked"));
    return this;
  }

  private void toggleCheckbox(String selector) {
    $(selector).click();
  }
}
