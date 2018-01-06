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

public class ProjectsManagementPage {

  public ProjectsManagementPage() {
    Selenide.$("#projects-management-page").should(Condition.exist);
  }

  public ProjectsManagementPage shouldHaveProjectsCount(int count) {
    Selenide.$$("#projects-management-page-projects tbody tr").shouldHaveSize(count);
    return this;
  }

  public ProjectsManagementPage shouldHaveProject(String key) {
    Selenide.$("#projects-management-page-projects").shouldHave(Condition.text(key));
    return this;
  }

  public ProjectsManagementPage createProject(String key, String name, String visibility) {
    Selenide.$("#create-project").click();
    Selenide.$("#create-project-name").val(key);
    Selenide.$("#create-project-key").val(name);
    Selenide.$("#visibility-" + visibility).click();
    Selenide.$("#create-project-submit").submit();
    return this;
  }

  public ProjectsManagementPage bulkApplyPermissionTemplate(String template) {
    Selenide.$(".js-bulk-apply-permission-template").click();
    Selenide.$(".modal .Select-value").click();
    Selenide.$$(".modal .Select-option").findBy(Condition.text(template)).click();
    Selenide.$(".modal-foot button").click();
    Selenide.$(".modal-body .alert-success").shouldBe(Condition.visible);
    return this;
  }
}
