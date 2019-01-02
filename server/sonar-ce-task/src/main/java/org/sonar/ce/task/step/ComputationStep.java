/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.step;

/**
 * A way of splitting the processing of a task into smaller items which can be executed sequentially
 * by {@link ComputationStepExecutor}.
 */
public interface ComputationStep {

  /**
   * Statistics are displayed in the step ending log. They help
   * understanding the runtime context and potential performance hotspots.
   *
   * <p/>
   * Example:
   * <code>
   *   statistics.add("inserts", 200);
   *   statistics.add("updates", 50);
   * </code>
   * adds two parameters to the log:
   * <code>
   * 2018.07.26 10:22:58 DEBUG ce[AWTVrwb-KZf9YbDx-laU][o.s.s.c.t.s.ComputationStepExecutor] Persist issues | inserts=200 | updates=50 | time=30ms
   * </code>
   *
   * <p/>
   * Statistics are logged in the order of insertion.
   */
  interface Statistics {
    /**
     * @return this
     * @throws NullPointerException if key or value is null
     * @throws IllegalArgumentException if key has already been set
     * @throws IllegalArgumentException if key is "time", to avoid conflict with the profiler field with same name
     */
    Statistics add(String key, Object value);
  }

  interface Context {
    Statistics getStatistics();
  }

  void execute(Context context);

  String getDescription();
}
