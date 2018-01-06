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
import org.openqa.selenium.By;

public class BackgroundTasksPage {

  public BackgroundTasksPage() {
    Selenide.$(By.cssSelector(".background-tasks")).should(Condition.exist);
  }

  public ElementsCollection getTasks() {
    return Selenide.$$(".background-tasks > tbody > tr");
  }

  public List<BackgroundTaskItem> getTasksAsItems() {
    return getTasks()
      .stream()
      .map(BackgroundTaskItem::new)
      .collect(Collectors.toList());
  }
}
