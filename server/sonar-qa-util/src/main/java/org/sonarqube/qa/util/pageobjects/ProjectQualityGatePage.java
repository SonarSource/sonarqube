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

public class ProjectQualityGatePage {

  public ProjectQualityGatePage() {
    Selenide.$("#project-quality-gate").should(Condition.exist);
  }

  public SelenideElement getSelectedQualityGate() {
    return Selenide.$(".Select-value-label");
  }

  public void assertNotSelected() {
    Selenide.$(".Select-placeholder").should(Condition.exist);
    Selenide.$(".Select-value-label").shouldNot(Condition.exist);
  }

  public void setQualityGate(String name) {
    Selenide.$(".Select-input input").val(name).pressEnter();
  }
}
