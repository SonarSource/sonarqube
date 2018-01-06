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
package org.sonarqube.qa.util.pageobjects.issues;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import java.util.List;
import java.util.stream.Collectors;

public class IssuesPage {

  public IssuesPage() {
    Selenide.$(".issues").should(Condition.exist);
  }

  public List<Issue> getIssues() {
    return getIssuesElements()
      .stream()
      .map(Issue::new)
      .collect(Collectors.toList());
  }

  public IssuesPage issuesCount(Integer count) {
    getIssuesElements().shouldHaveSize(count);
    return this;
  }

  public Issue getFirstIssue() {
    getIssuesElements().shouldHave(CollectionCondition.sizeGreaterThan(0));
    return new Issue(getIssuesElements().first());
  }

  public IssuesPage componentsShouldContain(String path) {
    getIssuesPathComponents().forEach(element -> element.shouldHave(Condition.text(path)));
    return this;
  }

  public IssuesPage componentsShouldNotContain(String path) {
    getIssuesPathComponents().forEach(element -> element.shouldNotHave(Condition.text(path)));
    return this;
  }

  public IssuesPage bulkChangeOpen() {
    Selenide.$("#issues-bulk-change").shouldBe(Condition.visible).click();
    Selenide.$("#bulk-change-form").shouldBe(Condition.visible);
    return this;
  }

  public IssuesPage bulkChangeAssigneeSearchCount(String query, Integer count) {
    Selenide.$("#issues-bulk-change-assignee .Select-input input").val(query);
    Selenide.$$("#issues-bulk-change-assignee .Select-option").shouldHaveSize(count);
    Selenide.$("#issues-bulk-change-assignee .Select-input input").pressEscape();
    return this;
  }

  private static ElementsCollection getIssuesElements() {
    return Selenide.$$(".issues .issue");
  }

  private static ElementsCollection getIssuesPathComponents() {
    return Selenide.$$(".issues-workspace-list-component");
  }
}
