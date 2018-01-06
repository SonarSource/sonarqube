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
package org.sonar.api.measures;

import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

/**
 * @since 2.5
 * @deprecated since 5.1 use {@link org.sonar.api.batch.measure.MetricFinder} on scanner side.
 */
@Deprecated
@ScannerSide
@ServerSide
@ComputeEngineSide
public interface MetricFinder {

  @CheckForNull
  Metric findById(int id);

  @CheckForNull
  Metric findByKey(String key);

  Collection<Metric> findAll(List<String> metricKeys);

  Collection<Metric> findAll();
}
