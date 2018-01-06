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
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectActivityPage {

  public ProjectActivityPage() {
    Selenide.$("#project-activity").should(Condition.exist);
  }

  public ElementsCollection getAnalyses() {
    return Selenide.$$(".project-activity-analysis");
  }

  public List<ProjectAnalysisItem> getAnalysesAsItems() {
    return getAnalyses()
      .stream()
      .map(ProjectAnalysisItem::new)
      .collect(Collectors.toList());
  }

  public ProjectAnalysisItem getLastAnalysis() {
    return new ProjectAnalysisItem(Selenide.$(".project-activity-analysis"));
  }

  public ProjectAnalysisItem getFirstAnalysis() {
    return new ProjectAnalysisItem(Selenide.$$(".project-activity-analysis").last());
  }

  public ProjectActivityPage assertFirstAnalysisOfTheDayHasText(String day, String text) {
    Selenide.$("#project-activity")
      .find(".project-activity-day[data-day=\"" + day + "\"]")
      .find(".project-activity-analysis")
      .should(Condition.text(text));
    return this;
  }
}
