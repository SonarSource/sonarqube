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
package org.sonar.scanner.task;

import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.MessageException;

/**
 * This task is deprecated since the refresh of portfolios and application is now automatic
 * This task does nothing
 *
 * @deprecated since 7.1
 */
@Deprecated
public class ViewsTask implements Task {

  private static final String KEY = "views";

  public static final TaskDefinition DEFINITION = TaskDefinition.builder()
    .key(KEY)
    .description("Removed - was used to trigger portfolios refresh")
    .taskClass(ViewsTask.class)
    .build();

  @Override
  public void execute() {
    throw MessageException.of("The task 'views' was removed with SonarQube 7.1. You can safely remove this call since portfolios and applications are automatically re-calculated.");
  }
}
