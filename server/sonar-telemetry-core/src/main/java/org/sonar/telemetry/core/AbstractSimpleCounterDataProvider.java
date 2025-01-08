/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.telemetry.core;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractSimpleCounterDataProvider extends AbstractTelemetryDataProvider<Integer> {
  private final AtomicInteger count = new AtomicInteger(0);

  protected AbstractSimpleCounterDataProvider(String metricKey) {
    super(metricKey, Dimension.INSTALLATION, Granularity.ADHOC, TelemetryDataType.INTEGER);
  }

  @Override
  public Optional<Integer> getValue() {
    return Optional.of(count.intValue());
  }

  @Override
  public void after() {
    count.set(0);
  }

  public void incrementCount() {
    count.incrementAndGet();
  }
}
