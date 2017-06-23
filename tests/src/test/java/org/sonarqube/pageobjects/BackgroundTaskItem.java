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

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.hasText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class BackgroundTaskItem {

  private final SelenideElement elt;

  public BackgroundTaskItem(SelenideElement elt) {
    this.elt = elt;
  }

  public SelenideElement getComponent() {
    return elt.$("td:nth-child(2)");
  }

  public BackgroundTaskItem openActions() {
    elt.$(".js-task-action > .dropdown-toggle").click();
    elt.$(".js-task-action > .dropdown-menu").shouldBe(visible);
    return this;
  }

  public BackgroundTaskItem openScannerContext () {
    elt.$(".js-task-show-scanner-context").click();
    $(".js-task-scanner-context").shouldBe(visible);
    return this;
  }

  public BackgroundTaskItem assertScannerContextContains(String text) {
    $(".js-task-scanner-context").should(hasText(text));
    return this;
  }

  public BackgroundTaskItem openErrorStacktrace () {
    elt.$(".js-task-show-stacktrace").click();
    $(".js-task-stacktrace").shouldBe(visible);
    return this;
  }

  public BackgroundTaskItem assertErrorStacktraceContains(String text) {
    $(".js-task-stacktrace").should(hasText(text));
    return this;
  }
}
