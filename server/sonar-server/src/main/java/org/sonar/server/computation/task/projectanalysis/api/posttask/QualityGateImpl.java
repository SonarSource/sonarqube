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
package org.sonar.server.computation.task.projectanalysis.api.posttask;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.posttask.QualityGate;

import static java.util.Objects.requireNonNull;

@Immutable
class QualityGateImpl implements QualityGate {
  private final String id;
  private final String name;
  private final Status status;
  private final Collection<Condition> conditions;

  public QualityGateImpl(String id, String name, Status status, Collection<Condition> conditions) {
    this.id = requireNonNull(id, "id can not be null");
    this.name = requireNonNull(name, "name can not be null");
    this.status = requireNonNull(status, "status can not be null");
    this.conditions = ImmutableList.copyOf(requireNonNull(conditions, "conditions can not be null"));
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Status getStatus() {
    return status;
  }

  @Override
  public Collection<Condition> getConditions() {
    return conditions;
  }

  @Override
  public String toString() {
    return "QualityGateImpl{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", status=" + status +
      ", conditions=" + conditions +
      '}';
  }
}
