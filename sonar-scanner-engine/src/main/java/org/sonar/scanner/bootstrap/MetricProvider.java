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
package org.sonar.scanner.bootstrap;

import com.google.common.collect.Lists;
import java.util.List;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

@ScannerSide
public class MetricProvider extends ExtensionProvider {

  private Metrics[] factories;

  public MetricProvider(Metrics[] factories) {
    this.factories = factories;
  }

  public MetricProvider() {
    this.factories = new Metrics[0];
  }

  @Override
  public List<Metric> provide() {
    List<Metric> metrics = Lists.newArrayList(CoreMetrics.getMetrics());
    for (Metrics factory : factories) {
      metrics.addAll(factory.getMetrics());
    }
    return metrics;
  }
}
