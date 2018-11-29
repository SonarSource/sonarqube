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
package org.sonar.api.batch.measure;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

import org.sonar.api.scanner.ScannerSide;

/**
 * @since 4.5
 */
@ScannerSide
@ThreadSafe
public interface MetricFinder {

  @CheckForNull
  <G extends Serializable> Metric<G> findByKey(String key);

  Collection<Metric<Serializable>> findAll(List<String> metricKeys);

  Collection<Metric<Serializable>> findAll();
}
