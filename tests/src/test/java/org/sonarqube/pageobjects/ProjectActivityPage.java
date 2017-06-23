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

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import java.util.List;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Condition.hasText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class ProjectActivityPage {

  public ProjectActivityPage() {
    $("#project-activity").should(Condition.exist);
  }

  public ElementsCollection getAnalyses() {
    return $$(".project-activity-analysis");
  }

  public List<ProjectAnalysisItem> getAnalysesAsItems() {
    return getAnalyses()
      .stream()
      .map(ProjectAnalysisItem::new)
      .collect(Collectors.toList());
  }

  public ProjectAnalysisItem getLastAnalysis() {
    return new ProjectAnalysisItem($(".project-activity-analysis"));
  }

  public ProjectAnalysisItem getFirstAnalysis() {
    return new ProjectAnalysisItem($$(".project-activity-analysis").last());
  }

  public ProjectActivityPage assertFirstAnalysisOfTheDayHasText(String day, String text) {
    $("#project-activity")
      .find(".project-activity-day[data-day=\"" + day + "\"]")
      .find(".project-activity-analysis")
      .should(hasText(text));
    return this;
  }
}
