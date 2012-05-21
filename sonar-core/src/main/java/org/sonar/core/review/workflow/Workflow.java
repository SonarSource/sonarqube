/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.review.workflow;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import org.sonar.api.ServerComponent;
import org.sonar.core.review.workflow.condition.Condition;
import org.sonar.core.review.workflow.condition.ProjectPropertyCondition;
import org.sonar.core.review.workflow.function.Function;
import org.sonar.core.review.workflow.screen.Screen;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Workflow implements ServerComponent {

  private Set<String> commands = Sets.newLinkedHashSet();
  private ListMultimap<String, Condition> conditionsByCommand = ArrayListMultimap.create();
  private ListMultimap<String, Function> functionsByCommand = ArrayListMultimap.create();
  private Map<String, Screen> screensByCommand = Maps.newLinkedHashMap();

  /**
   * Keys of all the properties that are required by conditions (see {@link org.sonar.core.review.workflow.condition.ProjectPropertyCondition}
   */
  private List<String> projectPropertyKeys = Lists.newArrayList();

  public Workflow addCommand(String key) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Empty command key");
    commands.add(key);
    return this;
  }

  public Set<String> getCommands() {
    return commands;
  }

  public boolean hasCommand(String key) {
    return commands.contains(key);
  }

  List<String> getProjectPropertyKeys() {
    return projectPropertyKeys;
  }

  public List<Condition> getConditions(String commandKey) {
    return conditionsByCommand.get(commandKey);
  }

  public Workflow addCondition(String commandKey, Condition condition) {
    Preconditions.checkArgument(hasCommand(commandKey), "Unknown command: " + commandKey);
    Preconditions.checkNotNull(condition);
    conditionsByCommand.put(commandKey, condition);
    if (condition instanceof ProjectPropertyCondition) {
      projectPropertyKeys.add(((ProjectPropertyCondition) condition).getPropertyKey());
    }
    return this;
  }

  public List<Function> getFunctions(String commandKey) {
    return functionsByCommand.get(commandKey);
  }

  public Workflow addFunction(String commandKey, Function function) {
    Preconditions.checkArgument(hasCommand(commandKey), "Unknown command: " + commandKey);
    Preconditions.checkNotNull(function);
    functionsByCommand.put(commandKey, function);
    return this;
  }

  public Screen getScreen(String commandKey) {
    return screensByCommand.get(commandKey);
  }

  public Workflow setScreen(String commandKey, Screen screen) {
    Preconditions.checkArgument(hasCommand(commandKey), "Unknown command: " + commandKey);
    Preconditions.checkNotNull(screen);
    Preconditions.checkState(Strings.isNullOrEmpty(screen.getCommandKey()), "Screen is already associated with command: " + screen.getCommandKey());
    screen.setCommandKey(commandKey);
    screensByCommand.put(commandKey, screen);
    return this;
  }

  Map<String, Screen> getScreensByCommand() {
    return screensByCommand;
  }
}
