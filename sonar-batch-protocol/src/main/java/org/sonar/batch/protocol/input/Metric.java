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
package org.sonar.batch.protocol.input;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class Metric {

  private final int id;

  private final String key;

  private final String valueType;

  private final String description;

  private final int direction;

  private final String name;

  private final boolean qualitative;

  private final boolean userManaged;

  private final Double worstValue;

  private final Double bestValue;

  private final boolean optimizedBestValue;

  public Metric(int id,
    String key,
    String valueType,
    @Nullable String description,
    int direction,
    String name,
    boolean qualitative,
    boolean userManaged,
    @Nullable Double worstValue,
    @Nullable Double bestValue,
    boolean optimizedBestValue) {
    this.id = id;
    this.key = key;
    this.valueType = valueType;
    this.description = description;
    this.direction = direction;
    this.name = name;
    this.qualitative = qualitative;
    this.userManaged = userManaged;
    this.worstValue = worstValue;
    this.bestValue = bestValue;
    this.optimizedBestValue = optimizedBestValue;
  }

  public int id() {
    return id;
  }

  public String key() {
    return key;
  }

  public String valueType() {
    return valueType;
  }

  @CheckForNull
  public String description() {
    return description;
  }

  public int direction() {
    return direction;
  }

  public String name() {
    return name;
  }

  public boolean isQualitative() {
    return qualitative;
  }

  public boolean isUserManaged() {
    return userManaged;
  }

  @CheckForNull
  public Double worstValue() {
    return worstValue;
  }

  @CheckForNull
  public Double bestValue() {
    return bestValue;
  }

  public boolean isOptimizedBestValue() {
    return optimizedBestValue;
  }

}
