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
package pageobjects.issues;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import java.util.List;
import java.util.stream.Collectors;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class IssuesPage {

  public IssuesPage() {
    $(".issues").should(exist);
  }

  private ElementsCollection getIssuesElements() {
    return $$(".issues .issue");
  }

  public List<Issue> getIssues() {
    return getIssuesElements()
      .stream()
      .map(Issue::new)
      .collect(Collectors.toList());
  }

  public Issue getFirstIssue() {
    getIssuesElements().shouldHave(sizeGreaterThan(0));
    return new Issue(getIssuesElements().first());
  }

  public IssuesPage bulkChangeOpen() {
    $("#issues-bulk-change").shouldBe(visible).click();
    $("a.js-bulk-change").click();
    $("#bulk-change-form").shouldBe(visible);
    return this;
  }

  public IssuesPage bulkChangeAssigneeSearchCount(String query, Integer count) {
    if (!$(".select2-drop-active").isDisplayed()) {
      $("#bulk-change-form #s2id_assignee").shouldBe(visible).click();
    }
    SelenideElement input = $(".select2-drop-active input").shouldBe(visible);
    input.val("").sendKeys(query);
    if (count > 0) {
      $(".select2-drop-active .select2-results li.select2-result").shouldBe(visible);
    } else {
      $(".select2-drop-active .select2-results li.select2-no-results").shouldBe(visible);
    }
    $$(".select2-drop-active .select2-results li.select2-result").shouldHaveSize(count);
    return this;
  }
}
