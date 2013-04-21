/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.measures;

import org.sonar.api.task.TaskComponent;

import org.sonar.api.ServerComponent;

import java.util.Collection;
import java.util.List;

/**
 * @since 2.5
 */
public interface MetricFinder extends TaskComponent, ServerComponent {

  Metric findById(int id);

  Metric findByKey(String key);

  Collection<Metric> findAll(List<String> metricKeys);

  Collection<Metric> findAll();
}
