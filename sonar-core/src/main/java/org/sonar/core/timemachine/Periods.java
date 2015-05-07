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
package org.sonar.core.timemachine;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchSide;
import org.sonar.api.CoreProperties;
import org.sonar.api.ServerSide;
import org.sonar.api.batch.RequiresDB;
import org.sonar.api.config.Settings;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.i18n.I18n;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.sonar.api.utils.DateUtils.longToDate;

@RequiresDB
@BatchSide
@ServerSide
public class Periods {

  private final Settings settings;
  private final I18n i18n;

  public Periods(Settings settings, I18n i18n) {
    this.settings = settings;
    this.i18n = i18n;
  }

  @CheckForNull
  public String label(Snapshot snapshot, int periodIndex) {
    return label(snapshot.getPeriodMode(periodIndex), snapshot.getPeriodModeParameter(periodIndex), longToDate(snapshot.getPeriodDateMs(periodIndex)));
  }

  @CheckForNull
  public String abbreviation(Snapshot snapshot, int periodIndex) {
    return abbreviation(snapshot.getPeriodMode(periodIndex), snapshot.getPeriodModeParameter(periodIndex), longToDate(snapshot.getPeriodDateMs(periodIndex)));
  }

  @CheckForNull
  public String label(int periodIndex) {
    String periodProperty = settings.getString(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex);
    PeriodParameters periodParameters = new PeriodParameters(periodProperty);
    return label(periodParameters.getMode(), periodParameters.getParam(), periodParameters.getDate());
  }

  @CheckForNull
  public String abbreviation(int periodIndex) {
    String periodProperty = settings.getString(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex);
    PeriodParameters periodParameters = new PeriodParameters(periodProperty);
    return abbreviation(periodParameters.getMode(), periodParameters.getParam(), periodParameters.getDate());
  }

  @CheckForNull
  public String label(String mode, String param, Date date) {
    return label(mode, param, convertDate(date), false);
  }

  @CheckForNull
  public String label(String mode, String param, String date) {
    return label(mode, param, date, false);
  }

  @CheckForNull
  public String abbreviation(String mode, String param, Date date) {
    return label(mode, param, convertDate(date), true);
  }

  @CheckForNull
  private String label(String mode, @Nullable String param, @Nullable String date, boolean shortLabel) {
    String label;
    if (CoreProperties.TIMEMACHINE_MODE_DAYS.equals(mode)) {
      label = label("over_x_days", shortLabel, param);
      if (date != null) {
        label = label("over_x_days_detailed", shortLabel, param, date);
      }
    } else if (CoreProperties.TIMEMACHINE_MODE_VERSION.equals(mode)) {
      label = label("since_version", shortLabel, param);
      if (date != null) {
        label = label("since_version_detailed", shortLabel, param, date);
      }
    } else if (CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS.equals(mode)) {
      label = label("since_previous_analysis", shortLabel);
      if (date != null) {
        label = label("since_previous_analysis_detailed", shortLabel, date);
      }
    } else if (CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION.equals(mode)) {
      label = label("since_previous_version", shortLabel);
      if (param != null) {
        label = label("since_previous_version_detailed", shortLabel, param);
        if (date != null) {
          label = label("since_previous_version_detailed", shortLabel, param, date);
        }
      }
    } else if (CoreProperties.TIMEMACHINE_MODE_DATE.equals(mode)) {
      label = label("since_x", shortLabel, date);
    } else {
      throw new IllegalArgumentException("This mode is not supported : " + mode);
    }
    return label;
  }

  private String label(String key, boolean shortLabel, Object... parameters) {
    String msgKey = key;
    if (shortLabel) {
      msgKey += ".short";
    }
    return i18n.message(getLocale(), msgKey, null, parameters);
  }

  @CheckForNull
  private String convertDate(Date date) {
    if (date != null) {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MMM dd");
      return dateFormat.format(date);
    }
    return null;
  }

  private Locale getLocale() {
    return Locale.ENGLISH;
  }

  private static class PeriodParameters {

    private String mode = null;
    private String param = null;
    private Date date = null;

    public PeriodParameters(String periodProperty) {
      if (CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS.equals(periodProperty) || CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION.equals(periodProperty)) {
        mode = periodProperty;
      } else if (findByDays(periodProperty) != null) {
        mode = CoreProperties.TIMEMACHINE_MODE_DAYS;
        param = Integer.toString(findByDays(periodProperty));
      } else if (findByDate(periodProperty) != null) {
        mode = CoreProperties.TIMEMACHINE_MODE_DATE;
        date = findByDate(periodProperty);
      } else if (StringUtils.isNotBlank(periodProperty)) {
        mode = CoreProperties.TIMEMACHINE_MODE_VERSION;
        param = periodProperty;
      } else {
        throw new IllegalArgumentException("Unknown period property : " + periodProperty);
      }
    }

    private Integer findByDays(String property) {
      try {
        return Integer.parseInt(property);
      } catch (NumberFormatException e) {
        return null;
      }
    }

    private Date findByDate(String property) {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
      try {
        return format.parse(property);
      } catch (ParseException e) {
        return null;
      }
    }

    public String getMode() {
      return mode;
    }

    public String getParam() {
      return param;
    }

    public Date getDate() {
      return date;
    }
  }

}
