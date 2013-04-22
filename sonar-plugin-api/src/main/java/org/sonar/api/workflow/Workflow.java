/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.workflow;

import com.google.common.annotations.Beta;
import org.sonar.api.ServerComponent;
import org.sonar.api.workflow.condition.Condition;
import org.sonar.api.workflow.function.Function;
import org.sonar.api.workflow.screen.Screen;

import java.util.List;
import java.util.Set;

/**
 * Experimental component to customize the actions that can be
 * executed on reviews.
 *
 * @since 3.1
 */
@Beta
public interface Workflow extends ServerComponent {
  Workflow addCommand(String key);

  Set<String> getCommands();

  List<Condition> getConditions(String commandKey);

  Workflow addCondition(String commandKey, Condition condition);

  List<Function> getFunctions(String commandKey);

  Workflow addFunction(String commandKey, Function function);

  Screen getScreen(String commandKey);

  Workflow setScreen(String commandKey, Screen screen);
}
