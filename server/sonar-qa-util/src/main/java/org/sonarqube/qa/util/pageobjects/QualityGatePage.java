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

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class QualityGatePage {
  public QualityGatePage() {
    $("#quality-gates-page").shouldBe(visible);
  }

  public QualityGatePage countQualityGates(Integer count) {
    $$("#quality-gates-page .list-group-item").shouldHaveSize(count);
    return this;
  }

  public QualityGatePage canCreateQG() {
    $("#quality-gate-add").should(exist).shouldBe(visible);
    return this;
  }

  public QualityGatePage createQG(String name) {
    $("#quality-gate-add").click();
    $(".modal-head").should(exist).shouldHave(text("Create Quality Gate"));
    $("#quality-gate-form-name").sendKeys(name);
    $(".js-confirm").click();
    return this;
  }

  public QualityGatePage canNotCreateQG() {
    $("#quality-gate-add").shouldNot(exist);
    return this;
  }

  public QualityGatePage displayIntro() {
    $(".search-navigator-intro").should(exist).shouldBe(visible);
    return this;
  }

  public QualityGatePage displayQualityGateDetail(String qualityGateName) {
    $(".layout-page-main-header").shouldHave(text(qualityGateName));
    return this;
  }
}
