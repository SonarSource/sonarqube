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
package org.sonar.server.computation.task.projectanalysis.measure;

import com.google.common.base.MoreObjects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

/**
 * A QualityGate status has a level and an optional describing text.
 */
@Immutable
public final class QualityGateStatus {
  private final Measure.Level status;
  @CheckForNull
  private final String text;

  /**
   * Creates a QualityGateStatus without a text.
   */
  public QualityGateStatus(Measure.Level status) {
    this(status, null);
  }

  /**
   * Creates a QualityGateStatus with a optional text.
   */
  public QualityGateStatus(Measure.Level status, @Nullable String text) {
    this.status = requireNonNull(status);
    this.text = text;
  }

  public Measure.Level getStatus() {
    return status;
  }

  @CheckForNull
  public String getText() {
    return text;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QualityGateStatus that = (QualityGateStatus) o;
    return status == that.status && java.util.Objects.equals(text, that.text);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(status, text);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("status", status)
        .add("text", text)
        .toString();
  }
}
