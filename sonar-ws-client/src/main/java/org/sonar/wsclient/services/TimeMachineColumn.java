/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.services;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @since 2.5
 */
public class TimeMachineColumn {

  private int index;
  private String metricKey;
  private String modelName;
  private String characteristicKey;

  public TimeMachineColumn(int index, @Nullable String metricKey, @Nullable String modelName, @Nullable String characteristicKey) {
    this.index = index;
    this.metricKey = metricKey;
    this.modelName = modelName;
    this.characteristicKey = characteristicKey;
  }

  @CheckForNull
  public String getMetricKey() {
    return metricKey;
  }

  @CheckForNull
  public String getModelName() {
    return modelName;
  }

  @CheckForNull
  public String getCharacteristicKey() {
    return characteristicKey;
  }

  public int getIndex() {
    return index;
  }
}
