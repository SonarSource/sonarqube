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

public class ProjectPermissionsPage {

  public ProjectPermissionsPage() {
    Selenide.$("#project-permissions-page").should(Condition.exist);
  }

  public ProjectPermissionsPage shouldBePublic() {
    Selenide.$("#visibility-public .icon-radio.is-checked").shouldBe(Condition.visible);
    return this;
  }

  public ProjectPermissionsPage shouldBePrivate() {
    Selenide.$("#visibility-private .icon-radio.is-checked").shouldBe(Condition.visible);
    return this;
  }

  public ProjectPermissionsPage turnToPublic() {
    Selenide.$("#visibility-public").click();
    Selenide.$("#confirm-turn-to-public").click();
    shouldBePublic();
    return this;
  }

  public ProjectPermissionsPage turnToPrivate() {
    Selenide.$("#visibility-private").click();
    shouldBePrivate();
    return this;
  }

  public ProjectPermissionsPage shouldNotAllowPrivate() {
    Selenide.$("#visibility-private").shouldHave(Condition.cssClass("text-muted"));
    Selenide.$(".upgrade-organization-box").shouldBe(Condition.visible);
    return this;
  }
}
