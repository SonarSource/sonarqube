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

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class SourceViewer {

  private final SelenideElement el;

  SourceViewer(SelenideElement el) {
    this.el = el;
  }

  public SelenideElement openCoverageDetails(int line) {
    this.el.$(".source-line-coverage[data-line-number=\"" + line + "\"").click();
    return $(".bubble-popup").shouldBe(visible);
  }

  public SourceViewer shouldHaveNewLines(int ...lines) {
    for (int line : lines) {
      this.el.$(".source-line-filtered[data-line-number=\"" + line + "\"").shouldBe(visible);
    }
    return this;
  }

  public SourceViewer shouldNotHaveNewLines(int ...lines) {
    for (int line : lines) {
      this.el.$(".source-line-filtered[data-line-number=\"" + line + "\"").shouldNotBe(visible);
    }
    return this;
  }

}
