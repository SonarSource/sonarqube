/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.period;

import java.util.Date;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.DateUtils.truncateToSeconds;

@Immutable
public class Period {
  private final String mode;
  private final String modeParameter;
  private final Long date;

  public Period(String mode, @Nullable String modeParameter, @Nullable Long date) {
    this.mode = requireNonNull(mode);
    this.modeParameter = modeParameter;
    this.date = date;
  }

  public String getMode() {
    return mode;
  }

  @CheckForNull
  public String getModeParameter() {
    return modeParameter;
  }

  @CheckForNull
  public Long getDate() {
    return date;
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
    return Objects.equals(date, period.date) && Objects.equals(mode, period.mode) && Objects.equals(modeParameter, period.modeParameter);
  }

  public boolean isOnPeriod(Date date) {
    if (this.date == null) {
      return false;
    }
    return date.getTime() > truncateToSeconds(this.date);
  }

  @Override
  public int hashCode() {
    return hash(mode, modeParameter, date);
  }

  @Override
  public String toString() {
    return toStringHelper(this)
      .add("mode", mode)
      .add("modeParameter", modeParameter)
      .add("date", date)
      .toString();
  }
}
