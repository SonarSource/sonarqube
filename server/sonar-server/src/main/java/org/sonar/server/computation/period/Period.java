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

import java.util.Calendar;
import java.util.Date;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.component.SnapshotDto;

public class Period {

  private int index;
  private String mode, modeParameter;
  private SnapshotDto projectSnapshot;
  /**
   * Date resolved from the settings of the period
   * - date : the given date
   * - days : nearest analysis date found
   * - previous analysis : date of the last analysis
   * - previous version : date of the analysis of the previous version
   * - version : date of the analysis of the given version
   */
  private Long targetDate = null;

  public Period(String mode, @Nullable Long targetDate, SnapshotDto projectSnapshot) {
    this.mode = mode;
    if (targetDate != null) {
      this.targetDate = targetDate;
    }
    this.projectSnapshot = projectSnapshot;
  }

  public Period setIndex(int index) {
    this.index = index;
    return this;
  }

  public int getIndex() {
    return index;
  }

  public SnapshotDto getProjectSnapshot() {
    return projectSnapshot;
  }

  /**
   * Date of the snapshot
   */
  public Long getSnapshotDate() {
    return projectSnapshot != null ? projectSnapshot.getCreatedAt() : null;
  }

  public String getMode() {
    return mode;
  }

  public String getModeParameter() {
    return modeParameter;
  }

  public Period setModeParameter(String s) {
    this.modeParameter = s;
    return this;
  }

  public Long getTargetDate() {
    return targetDate;
  }

  @Override
  public String toString() {
    Date snapshotDate = new Date(getSnapshotDate());
    if (StringUtils.equals(mode, CoreProperties.TIMEMACHINE_MODE_VERSION)) {
      String label = String.format("Compare to version %s", modeParameter);
      if (targetDate != null) {
        label += String.format(" (%s)", DateUtils.formatDate(snapshotDate));
      }
      return label;
    }
    if (StringUtils.equals(mode, CoreProperties.TIMEMACHINE_MODE_DAYS)) {
      return String.format("Compare over %s days (%s, analysis of %s)", modeParameter, formatDate(),DateUtils.formatDate(snapshotDate));
    }
    if (StringUtils.equals(mode, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS)) {
      return String.format("Compare to previous analysis (%s)", DateUtils.formatDate(snapshotDate));
    }
    if (StringUtils.equals(mode, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION)) {
      return String.format("Compare to previous version (%s)", DateUtils.formatDate(snapshotDate));
    }
    if (StringUtils.equals(mode, CoreProperties.TIMEMACHINE_MODE_DATE)) {
      return String.format("Compare to date %s (analysis of %s)", formatDate(), DateUtils.formatDate(snapshotDate));
    }
    return ReflectionToStringBuilder.toString(this);
  }

  private String formatDate() {
    return DateUtils.formatDate(org.apache.commons.lang.time.DateUtils.truncate(new Date(targetDate), Calendar.SECOND));
  }

}
