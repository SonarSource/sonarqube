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
    elt.$(".js-task-action > .dropdown-menu").shouldBe(Condition.visible);
    return this;
  }

  public BackgroundTaskItem openScannerContext() {
    elt.$(".js-task-show-scanner-context").click();
    Selenide.$(".js-task-scanner-context").shouldBe(Condition.visible);
    return this;
  }

  public BackgroundTaskItem assertScannerContextContains(String text) {
    Selenide.$(".js-task-scanner-context").should(Condition.text(text));
    return this;
  }

  public BackgroundTaskItem openErrorStacktrace() {
    elt.$(".js-task-show-stacktrace").click();
    Selenide.$(".js-task-stacktrace").shouldBe(Condition.visible);
    return this;
  }

  public BackgroundTaskItem assertErrorStacktraceContains(String text) {
    Selenide.$(".js-task-stacktrace").should(Condition.text(text));
    return this;
  }
}
