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

public class EncryptionPage extends Navigation {

  public EncryptionPage() {
    Selenide.$("#encryption-page").should(Condition.exist);
  }

  public SelenideElement generationForm() {
    return Selenide.$("#generate-secret-key-form");
  }

  public SelenideElement newSecretKey() {
    return Selenide.$("#secret-key");
  }

  public String encryptValue(String value) {
    Selenide.$("#encryption-form-value").val(value);
    Selenide.$("#encryption-form").submit();
    return Selenide.$("#encrypted-value").shouldBe(Condition.visible).val();
  }

  public EncryptionPage generateNewKey() {
    Selenide.$("#encryption-new-key-form").submit();
    Selenide.$("#generate-secret-key-form").shouldBe(Condition.visible);
    return this;
  }
}
