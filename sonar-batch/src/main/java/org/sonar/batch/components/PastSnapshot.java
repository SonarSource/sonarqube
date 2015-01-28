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
package org.sonar.batch.components;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.utils.DateUtils;

import javax.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;

import static org.sonar.api.utils.DateUtils.longToDate;

/**
 * Used by devcockpit
 */
public class PastSnapshot {

  private int index;
  private String mode, modeParameter;
  private Snapshot projectSnapshot;
  private Date targetDate = null;

  public PastSnapshot(String mode, @Nullable Date targetDate, @Nullable Snapshot projectSnapshot) {
    this.mode = mode;
    if (targetDate != null) {
      this.targetDate = org.apache.commons.lang.time.DateUtils.truncate(targetDate, Calendar.SECOND);
    }
    this.projectSnapshot = projectSnapshot;
  }

  public PastSnapshot(String mode, @Nullable Date targetDate) {
    this(mode, targetDate, null);
  }

  /**
   * See SONAR-2428 : even if previous analysis does not exist (no snapshot and no target date), we should perform comparison.
   */
  public PastSnapshot(String mode) {
    this(mode, null, null);
  }

  public PastSnapshot setIndex(int index) {
    this.index = index;
    return this;
  }

  public int getIndex() {
    return index;
  }

  public boolean isRelatedToSnapshot() {
    return projectSnapshot != null;
  }

  public Snapshot getProjectSnapshot() {
    return projectSnapshot;
  }

  public Date getDate() {
    return projectSnapshot != null ? longToDate(projectSnapshot.getCreatedAtMs()) : null;
  }

  public PastSnapshot setMode(String mode) {
    this.mode = mode;
    return this;
  }

  public String getMode() {
    return mode;
  }

  public String getModeParameter() {
    return modeParameter;
  }

  public PastSnapshot setModeParameter(String s) {
    this.modeParameter = s;
    return this;
  }

  public Integer getProjectSnapshotId() {
    return projectSnapshot != null ? projectSnapshot.getId() : null;
  }

  public String getQualifier() {
    return projectSnapshot != null ? projectSnapshot.getQualifier() : null;
  }

  /**
   * @deprecated in 4.2. Target date should only be used in labels.
   */
  @Deprecated
  public Date getTargetDate() {
    return targetDate;
  }

  public PastSnapshot clonePastSnapshot() {
    PastSnapshot clone = new PastSnapshot(mode, targetDate, projectSnapshot);
    clone.setIndex(index);
    clone.setModeParameter(modeParameter);
    return clone;
  }

  @Override
  public String toString() {
    if (StringUtils.equals(mode, CoreProperties.TIMEMACHINE_MODE_VERSION)) {
      String label = String.format("Compare to version %s", modeParameter);
      if (targetDate != null) {
        label += String.format(" (%s)", DateUtils.formatDate(getDate()));
      }
      return label;
    }
    if (StringUtils.equals(mode, CoreProperties.TIMEMACHINE_MODE_DAYS)) {
      String label = String.format("Compare over %s days (%s", modeParameter, DateUtils.formatDate(targetDate));
      if (isRelatedToSnapshot()) {
        label += ", analysis of " + getDate();
      }
      label += ")";
      return label;
    }
    if (StringUtils.equals(mode, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS)) {
      String label = "Compare to previous analysis";
      if (isRelatedToSnapshot()) {
        label += String.format(" (%s)", DateUtils.formatDate(getDate()));
      }
      return label;
    }
    if (StringUtils.equals(mode, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION)) {
      String label = "Compare to previous version";
      if (isRelatedToSnapshot()) {
        label += String.format(" (%s)", DateUtils.formatDate(getDate()));
      }
      return label;
    }
    if (StringUtils.equals(mode, CoreProperties.TIMEMACHINE_MODE_DATE)) {
      String label = "Compare to date " + DateUtils.formatDate(targetDate);
      if (isRelatedToSnapshot()) {
        label += String.format(" (analysis of %s)", DateUtils.formatDate(getDate()));
      }
      return label;
    }
    return ReflectionToStringBuilder.toString(this);
  }

}
