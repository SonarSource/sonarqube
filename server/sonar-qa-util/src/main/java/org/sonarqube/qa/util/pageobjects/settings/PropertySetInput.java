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
import com.codeborne.selenide.SelenideElement;

public class PropertySetInput {

  private final SelenideElement elt;

  public PropertySetInput(SelenideElement elt) {
    this.elt = elt;
  }

  public PropertySetInput setFieldValue(int index, String fieldKey, String value) {
    elt.findAll("input[name$=\"[" + fieldKey + "]\"]").get(index).val(value);
    return this;
  }

  public PropertySetInput setFieldValue(String fieldKey, String value) {
    return setFieldValue(0, fieldKey, value);
  }

  public PropertySetInput save() {
    elt.find(".js-save-changes").click();
    elt.find(".js-save-changes").shouldNot(Condition.exist);
    return this;
  }
}
