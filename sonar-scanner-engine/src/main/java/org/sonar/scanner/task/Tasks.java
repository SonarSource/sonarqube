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

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;

@ScannerSide
@InstantiationStrategy(InstantiationStrategy.PER_TASK)
public class Tasks {

  private final SortedMap<String, TaskDefinition> byKey;

  public Tasks(TaskDefinition[] definitions) {
    SortedMap<String, TaskDefinition> map = Maps.newTreeMap();
    for (TaskDefinition definition : definitions) {
      if (map.containsKey(definition.key())) {
        throw new IllegalStateException("Task '" + definition.key() + "' is declared twice");
      }
      map.put(definition.key(), definition);
    }
    this.byKey = ImmutableSortedMap.copyOf(map);
  }

  public TaskDefinition definition(String taskKey) {
    return byKey.get(taskKey);
  }

  public Collection<TaskDefinition> definitions() {
    return byKey.values();
  }

  /**
   * Perform validation of task definitions
   */
  public void start() {
    checkDuplicatedClasses();
  }

  private void checkDuplicatedClasses() {
    Map<Class<? extends Task>, TaskDefinition> byClass = Maps.newHashMap();
    for (TaskDefinition def : definitions()) {
      TaskDefinition other = byClass.get(def.taskClass());
      if (other == null) {
        byClass.put(def.taskClass(), def);
      } else {
        throw new IllegalStateException("Task '" + def.taskClass().getName() + "' is defined twice: first by '" + other.key() + "' and then by '" + def.key() + "'");
      }
    }
  }
}
