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
package pageobjects;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class ProjectsManagementPage {

  public ProjectsManagementPage() {
    $("#projects-management-page").should(exist);
  }

  public ProjectsManagementPage shouldHaveProjectsCount(int count) {
    $$("#projects-management-page-projects tr").shouldHaveSize(count);
    return this;
  }

  public ProjectsManagementPage shouldHaveProject(String key) {
    $("#projects-management-page-projects").shouldHave(text(key));
    return this;
  }

  public ProjectsManagementPage createProject(String key, String name, String visibility) {
    $("#create-project").click();
    $("#create-project-name").val(key);
    $("#create-project-key").val(name);
    $("#visibility-" + visibility).click();
    $("#create-project-submit").submit();
    return this;
  }
}
