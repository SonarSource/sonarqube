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
package org.sonar.batch.qualitygate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sonar.api.BatchSide;

import javax.annotation.Nullable;

import java.util.Collection;

@BatchSide
public class QualityGate {

  private final String name;

  private final Collection<ResolvedCondition> conditions;

  QualityGate(@Nullable String name) {
    this.name = name;
    this.conditions = Lists.newArrayList();
  }

  void add(ResolvedCondition condition) {
    this.conditions.add(condition);
  }

  static QualityGate disabled() {
    return new QualityGate(null);
  }

  public String name() {
    return name;
  }

  public Collection<ResolvedCondition> conditions() {
    return ImmutableList.copyOf(conditions);
  }

  public boolean isEnabled() {
    return name != null;
  }
}
