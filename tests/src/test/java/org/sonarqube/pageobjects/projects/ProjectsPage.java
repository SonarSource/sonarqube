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
package org.sonarqube.pageobjects.projects;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;

public class ProjectsPage {

  public ProjectsPage() {
    $("#projects-page").shouldBe(visible);
  }

  public ElementsCollection getProjects() {
    return $$(".projects-list > .boxed-group");
  }

  public ElementsCollection getFacets() {
    return $$(".search-navigator-facet-box");
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
    $("#projects-total").shouldHave(text(String.valueOf(total)));
    return this;
  }

  public ProjectsPage shouldDisplayAllProjects() {
    assertThat(url()).endsWith("/projects");
    return this;
  }

  public ProjectsPage shouldDisplayAllProjectsWidthSort(String sort) {
    assertThat(url()).endsWith("/projects?sort=" + sort);
    return this;
  }

  public ProjectsPage shouldDisplayFavoriteProjects() {
    assertThat(url()).endsWith("/projects/favorite");
    return this;
  }

  public ProjectsPage selectAllProjects() {
    $("#all-projects").click();
    return shouldDisplayAllProjects();
  }

  public ProjectsPage selectFavoriteProjects() {
    $("#favorite-projects").click();
    return shouldDisplayFavoriteProjects();
  }

  public ProjectsPage searchProject(String search) {
    SelenideElement searchInput = $(".projects-topbar-item-search input");
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
    getOpenTopBar().$(".js-projects-sorting-select a.button-icon").should(Condition.exist).click();
    return this;
  }

  private SelenideElement getOpenTopBar() {
    return $(".projects-topbar-items").should(Condition.exist);
  }
}
