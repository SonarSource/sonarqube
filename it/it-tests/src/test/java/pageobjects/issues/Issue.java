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
package pageobjects.issues;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;

public class Issue {

  private final SelenideElement elt;

  public Issue(SelenideElement elt) {
    this.elt = elt;
  }

  public Issue shouldAllowAssign() {
    elt.find(".js-issue-assign").shouldBe(visible);
    return this;
  }

  public Issue shouldAllowChangeType() {
    elt.find(".js-issue-set-type").shouldBe(visible);
    return this;
  }

  public Issue shouldNotAllowAssign() {
    elt.find(".js-issue-assign").shouldNotBe(visible);
    return this;
  }

  public Issue shouldNotAllowChangeType() {
    elt.find(".js-issue-set-type").shouldNotBe(visible);
    return this;
  }
}
