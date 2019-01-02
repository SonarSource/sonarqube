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
package org.sonar.ce.monitoring;

import java.util.List;

public interface CeTasksMBean {

  String OBJECT_NAME = "SonarQube:name=ComputeEngineTasks";

  /**
   * Count of batch reports waiting for processing since startup, including reports received before instance startup.
   */
  long getPendingCount();

  /**
   * Count of batch reports under processing.
   */
  long getInProgressCount();

  /**
   * Count of batch reports which processing ended with an error since instance startup.
   */
  long getErrorCount();

  /**
   * Count of batch reports which processing ended successfully since instance startup.
   */
  long getSuccessCount();

  /**
   * Time spent processing reports since startup, in milliseconds.
   */
  long getProcessingTime();

  /**
   * Configured maximum number of workers.
   */
  int getWorkerMaxCount();

  /**
   * Configured number of Workers.
   */
  int getWorkerCount();

  List<String> getWorkerUuids();

  List<String> getEnabledWorkerUuids();
}
