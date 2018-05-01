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

public class DocumentationPage {

  public DocumentationPage() {
    Selenide.$(".markdown").shouldBe(Condition.visible);
  }

  public DocumentationPage shouldHaveLinks() {
    Selenide.$(".api-documentation-results .list-group .list-group-item").exists();
    return this;
  }

  public DocumentationPage selectLink(int index) {
    Selenide.$$(".api-documentation-results .list-group .list-group-item").get(index).click();
    return this;
  }

  public String getDocumentationContent() {
    return Selenide.$(".markdown.cut-margins.boxed-group-inner").getText();
  }

}
