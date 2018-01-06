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

public class SystemInfoPageItem {
  private final SelenideElement elt;

  public SystemInfoPageItem(SelenideElement elt) {
    this.elt = elt;
  }

  public SystemInfoPageItem shouldHaveHealth() {
    elt.$(".system-info-health-info .status-indicator").should(Condition.exist);
    return this;
  }

  public SystemInfoPageItem shouldHaveSection(String section) {
    ensureOpen();
    elt.$$("h4").findBy(Condition.text(section)).should(Condition.exist);
    return this;
  }

  public SystemInfoPageItem shouldNotHaveSection(String section) {
    ensureOpen();
    elt.$$("h4").findBy(Condition.text(section)).shouldNot(Condition.exist);
    return this;
  }

  public SystemInfoPageItem shouldHaveMainSection() {
    ensureOpen();
    elt.$$(".system-info-section").get(0).find("h4").shouldNot(Condition.exist);
    return this;
  }

  public SystemInfoPageItem shouldHaveField(String field) {
    ensureOpen();
    elt.$$(".system-info-section-item-name").findBy(Condition.text(field)).should(Condition.exist);
    return this;
  }

  public SystemInfoPageItem shouldNotHaveField(String field) {
    ensureOpen();
    elt.$$(".system-info-section-item-name").findBy(Condition.exactText(field)).shouldNot(Condition.exist);
    return this;
  }

  public SystemInfoPageItem shouldHaveFieldWithValue(String field, String value) {
    ensureOpen();
    SelenideElement fieldElem = elt.$$(".system-info-section-item-name").findBy(Condition.text(field)).should(Condition.exist);
    fieldElem.parent().parent().$$("td").shouldHaveSize(2).get(1).shouldHave(Condition.text(value));
    return this;
  }

  public SystemInfoPageItem ensureOpen() {
    if (!isOpen()) {
      elt.click();
      elt.$(".boxed-group-inner").should(Condition.exist);
    }
    return this;
  }

  private boolean isOpen() {
    return elt.$(".boxed-group-inner").exists();
  }
}
