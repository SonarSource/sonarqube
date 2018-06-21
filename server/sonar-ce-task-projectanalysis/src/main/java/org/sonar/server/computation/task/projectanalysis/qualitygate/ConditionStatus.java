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
package org.sonar.server.computation.task.projectanalysis.qualitygate;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@Immutable
public class ConditionStatus {
  public static final ConditionStatus NO_VALUE_STATUS = new ConditionStatus(EvaluationStatus.NO_VALUE, null);

  private final EvaluationStatus status;
  @CheckForNull
  private final String value;

  private ConditionStatus(EvaluationStatus status, @Nullable String value) {
    this.status = requireNonNull(status, "status can not be null");
    this.value = value;
  }

  public static ConditionStatus create(EvaluationStatus status, String value) {
    requireNonNull(status, "status can not be null");
    checkArgument(status != EvaluationStatus.NO_VALUE, "EvaluationStatus 'NO_VALUE' can not be used with this method, use constant ConditionStatus.NO_VALUE_STATUS instead.");
    requireNonNull(value, "value can not be null");
    return new ConditionStatus(status, value);
  }

  public EvaluationStatus getStatus() {
    return status;
  }

  /**
   * @return {@code null} when {@link #getStatus()} is {@link EvaluationStatus#NO_VALUE}, otherwise non {@code null}
   */
  @CheckForNull
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "ConditionStatus{" +
      "status=" + status +
      ", value='" + value + '\'' +
      '}';
  }

  public enum EvaluationStatus {
    NO_VALUE, OK, WARN, ERROR
  }

}
