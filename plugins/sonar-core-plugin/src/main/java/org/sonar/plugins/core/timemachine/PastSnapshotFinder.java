/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.timemachine;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.utils.Logs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PastSnapshotFinder implements BatchExtension {

  /**
   * IMPORTANT : please update default values in the ruby side too. See app/helpers/FiltersHelper.rb, method period_names()
   */
  public static final String DEFAULT_VALUE_1 = PastSnapshotFinderByPreviousAnalysis.MODE;
  public static final String DEFAULT_VALUE_2 = "5";
  public static final String DEFAULT_VALUE_3 = "30";
  public static final String PROPERTY_PREFIX = "sonar.timemachine.period";

  private PastSnapshotFinderByDays finderByDays;
  private PastSnapshotFinderByVersion finderByVersion;
  private PastSnapshotFinderByDate finderByDate;
  private PastSnapshotFinderByPreviousAnalysis finderByPreviousAnalysis;

  public PastSnapshotFinder(PastSnapshotFinderByDays finderByDays, PastSnapshotFinderByVersion finderByVersion,
                            PastSnapshotFinderByDate finderByDate, PastSnapshotFinderByPreviousAnalysis finderByPreviousAnalysis) {
    this.finderByDays = finderByDays;
    this.finderByVersion = finderByVersion;
    this.finderByDate = finderByDate;
    this.finderByPreviousAnalysis = finderByPreviousAnalysis;
  }

  public PastSnapshot find(Configuration conf, int index) {
    String propertyValue = getPropertyValue(conf, index);
    PastSnapshot pastSnapshot = find(index, propertyValue);
    if (pastSnapshot==null && StringUtils.isNotBlank(propertyValue)) {
      Logs.INFO.warn("The property " + PROPERTY_PREFIX + index + " has an unvalid value: " + propertyValue);
    }
    return pastSnapshot;
  }

  static String getPropertyValue(Configuration conf, int index) {
    String defaultValue = null;
    switch (index) {
      // only global settings (from 1 to 3) have default values
      case 1: defaultValue = DEFAULT_VALUE_1; break;
      case 2: defaultValue = DEFAULT_VALUE_2; break;
      case 3: defaultValue = DEFAULT_VALUE_3; break;
    }
    return conf.getString(PROPERTY_PREFIX + index, defaultValue);
  }

  public PastSnapshot find(int index, String property) {
    if (StringUtils.isBlank(property)) {
      return null;
    }

    PastSnapshot result = findByDays(property);
    if (result == null) {
      result = findByDate(property);
      if (result == null) {
        result = findByPreviousAnalysis(property);
        if (result == null) {
          result = findByVersion(property);
        }
      }
    }

    if (result != null) {
      result.setIndex(index);
    }

    return result;
  }

  private PastSnapshot findByPreviousAnalysis(String property) {
    PastSnapshot pastSnapshot = null;
    if (StringUtils.equals(PastSnapshotFinderByPreviousAnalysis.MODE, property)) {
      pastSnapshot = finderByPreviousAnalysis.findByPreviousAnalysis();
    }
    return pastSnapshot;
  }

  private PastSnapshot findByDate(String property) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    try {
      Date date = format.parse(property);
      return finderByDate.findByDate(date);

    } catch (ParseException e) {
      return null;
    }
  }

  private PastSnapshot findByVersion(String property) {
    return finderByVersion.findByVersion(property);
  }

  private PastSnapshot findByDays(String property) {
    try {
      int days = Integer.parseInt(property);
      return finderByDays.findFromDays(days);

    } catch (NumberFormatException e) {
      return null;
    }
  }

}
