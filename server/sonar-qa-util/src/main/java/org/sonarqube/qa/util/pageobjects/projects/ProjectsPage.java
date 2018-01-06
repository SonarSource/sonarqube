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
package org.sonarqube.qa.util.pageobjects.projects;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.assertj.core.api.Assertions;

public class ProjectsPage {

  public ProjectsPage() {
    Selenide.$("#projects-page").shouldBe(Condition.visible);
  }

  public ElementsCollection getProjects() {
    return Selenide.$$(".projects-list > .boxed-group");
  }

  public ElementsCollection getFacets() {
    return Selenide.$$(".search-navigator-facet-box");
  }

  public ProjectItem getProjectByKey(String projectKey) {
    SelenideElement element = getProjects().find(Condition.attribute("data-key", projectKey));
    return new ProjectItem(element);
  }

  public ProjectItem getProjectByIdx(Integer idx) {
    return new ProjectItem(getProjects().get(idx));
  }

  public FacetItem getFacetByProperty(String facetProperty) {
    SelenideElement element = getFacets().find(Condition.attribute("data-key", facetProperty));
    return new FacetItem(element);
  }

  public ProjectsPage shouldHaveTotal(int total) {
    // warning - number is localized
    Selenide.$("#projects-total").shouldHave(Condition.text(String.valueOf(total)));
    return this;
  }

  public ProjectsPage shouldDisplayAllProjects() {
    Assertions.assertThat(WebDriverRunner.url()).endsWith("/projects");
    return this;
  }

  public ProjectsPage shouldDisplayAllProjectsWidthSort(String sort) {
    Assertions.assertThat(WebDriverRunner.url()).endsWith("/projects?sort=" + sort);
    return this;
  }

  public ProjectsPage shouldDisplayFavoriteProjects() {
    Assertions.assertThat(WebDriverRunner.url()).endsWith("/projects/favorite");
    return this;
  }

  public ProjectsPage selectAllProjects() {
    Selenide.$("#all-projects").click();
    return shouldDisplayAllProjects();
  }

  public ProjectsPage selectFavoriteProjects() {
    Selenide.$("#favorite-projects").click();
    return shouldDisplayFavoriteProjects();
  }

  public ProjectsPage searchProject(String search) {
    SelenideElement searchInput = Selenide.$(".projects-topbar-item-search input");
    searchInput.setValue("").sendKeys(search);
    return this;
  }

  public ProjectsPage changePerspective(String perspective) {
    SelenideElement sortSelect = getOpenTopBar().$(".js-projects-perspective-select");
    sortSelect.$(".Select-value").should(Condition.exist).click();
    sortSelect.$(".Select-option[title='" + perspective + "']").should(Condition.exist).click();
    return this;
  }

  public ProjectsPage sortProjects(String sort) {
    SelenideElement sortSelect = getOpenTopBar().$(".js-projects-sorting-select");
    sortSelect.$(".Select-value").should(Condition.exist).click();
    sortSelect.$(".Select-option[title='" + sort + "']").should(Condition.exist).click();
    return this;
  }

  public ProjectsPage invertSorting() {
    getOpenTopBar().$(".js-projects-sorting-invert").should(Condition.exist).click();
    return this;
  }

  private static SelenideElement getOpenTopBar() {
    return Selenide.$(".projects-topbar-items").should(Condition.exist);
  }
}
