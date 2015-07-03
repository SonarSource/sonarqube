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

package org.sonar.server.computation.period;

import com.google.common.base.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

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
  public String toString() {
    return Objects.toStringHelper(this)
      .add("index", index)
      .add("mode", mode)
      .add("modeParameter", modeParameter)
      .add("snapshotDate", snapshotDate)
      .add("snapshotId", snapshotId)
      .toString();
  }
}
