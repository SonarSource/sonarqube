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
package org.sonarqube.pageobjects;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class ProjectCodePage {

  public ProjectCodePage() {}

  public ProjectCodePage openFirstComponent() {
    $$(".code-name-cell a").first().click();
    return this;
  }

  public ProjectCodePage search(String query) {
    $(".search-box-input").val(query);
    return this;
  }

  public ProjectCodePage shouldHaveComponent(String name) {
    $(".code-components").shouldHave(text(name));
    return this;
  }

  public ProjectCodePage shouldHaveCode(String code) {
    $(".source-viewer").shouldHave(text(code));
    return this;
  }

  public ProjectCodePage shouldHaveBreadcrumbs(String... breadcrumbs) {
    for (String breadcrumb : breadcrumbs) {
      $(".code-breadcrumbs").shouldHave(text(breadcrumb));
    }
    return this;
  }

  public ProjectCodePage shouldSearchResult(String name) {
    $(".code-search-with-results").shouldHave(text(name));
    return this;
  }
}
