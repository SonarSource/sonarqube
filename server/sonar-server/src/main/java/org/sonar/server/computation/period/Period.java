/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.period;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Objects.toStringHelper;
import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

@Immutable
public class Period {
  private final int index;
  private final String mode;
  @CheckForNull
  private final String modeParameter;
  private final long snapshotDate;
  private final long snapshotId;

  public Period(int index, String mode, @Nullable String modeParameter,
    long snapshotDate, long snapshotId) {
    if (!isValidPeriodIndex(index)) {
      throw new IllegalArgumentException(String.format("Period index (%s) must be > 0 and < 6", index));
    }
    this.index = index;
    this.mode = requireNonNull(mode);
    this.modeParameter = modeParameter;
    this.snapshotDate = snapshotDate;
    this.snapshotId = snapshotId;
  }

  public static boolean isValidPeriodIndex(int i) {
    return i > 0 && i < 6;
  }

  /**
   * Index of periods is 1-based. It goes from 1 to 5.
   */
  public int getIndex() {
    return index;
  }

  public String getMode() {
    return mode;
  }

  @CheckForNull
  public String getModeParameter() {
    return modeParameter;
  }

  public long getSnapshotDate() {
    return snapshotDate;
  }

  public long getSnapshotId() {
    return snapshotId;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Period period = (Period) o;
    return index == period.index
      && snapshotDate == period.snapshotDate
      && snapshotId == period.snapshotId
      && mode.equals(period.mode)
      && java.util.Objects.equals(modeParameter, period.modeParameter);
  }

  @Override
  public int hashCode() {
    return hash(index, mode, modeParameter, snapshotDate, snapshotId);
  }

  @Override
  public String toString() {
    return toStringHelper(this)
      .add("index", index)
      .add("mode", mode)
      .add("modeParameter", modeParameter)
      .add("snapshotDate", snapshotDate)
      .add("snapshotId", snapshotId)
      .toString();
  }
}
