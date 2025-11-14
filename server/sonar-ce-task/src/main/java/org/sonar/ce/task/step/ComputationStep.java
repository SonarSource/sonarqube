/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import javax.annotation.Nullable;

/**
 * A way of splitting the processing of a task into smaller items which can be executed sequentially
 * by {@link ComputationStepExecutor}.
 */
public interface ComputationStep {

  void execute(Context context);

  String getDescription();

  /**
   * Statistics are displayed in the step ending log. They help
   * understanding the runtime context and potential performance hotspots.
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
   * <p/>
   * Statistics are logged in the order of insertion.
   */
  interface Statistics {
    /**
     * Adds a key-value pair to be logged at the end of the step.
     * The keys are in a per-step context so do not need to be namespaced
     * to uniquely identify them across steps, just short names are fine.
     *
     * @return this
     * @throws NullPointerException if key or value is null
     * @throws IllegalArgumentException if key has already been set
     * @throws IllegalArgumentException if key is "time", to avoid conflict with the profiler field with same name
     */
    Statistics add(String key, Object value);
  }

  interface Context {
    /**
     * Get a statistics object from the current task context. The object
     * is cleared between each step, and used to log statistics at the
     * end of the step.
     *
     * @return the Statistics instance
     */
    Statistics getStatistics();

    /**
     * Add a metric to the telemetry data. The given prefix will be prepended to the key
     * with a period separating it from the key. Using a prefix is advisable because telemetry
     * is in a global namespace, but if needed the prefix may be null to leave it off.
     * The naming convention for keys is to use snake_case with underscores.
     *<p>
     * Typically it will make sense to prefer {@link #addTelemetryWithStatistic(String, String, Object)}
     * so the information is visible in the logs as well as the telemetry. But if you know you're
     * already logging the information yourself, you might not need it to be a statistic.
     *</p>
     * @param telemetryPrefix to prepend to the key with a period separating them, or null for none
     * @param key key following snake_case convention
     * @param value telemetry value
     */
    void addTelemetryMetricOnly(@Nullable String telemetryPrefix, String key, Object value);

    /**
     * Adds a metric to the telemetry data and also adds it to the statistics log line at the same
     * time. The telemetry metric gets the prefix and the statistic does not. For full control of
     * statistic and telemetry separately, use {@link Context#getStatistics()} and
     * {@link Context#addTelemetryMetricOnly(String, String, Object)}. The telemetryPrefix is
     * not nullable on this method because the key really should be different for the telemetry
     * and the statistic (telemetry is globally namespaced, statistics are per-step).
     * <p>
     *   This method is preferred over telemetry only, it can't hurt to show the information in both
     *   places most of the time.
     * </p>
     * 
     * @param telemetryPrefix to prepend to the key for the telemetry, but not for the statistic
     * @param key key following snake_case convention
     * @param value telemetry/statistic value
     */
    void addTelemetryWithStatistic(String telemetryPrefix, String key, Object value);
  }
}
