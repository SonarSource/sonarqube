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
package org.sonar.core.timemachine;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.i18n.I18n;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DATE;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DAYS;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_PREVIOUS_VERSION;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_VERSION;

public class Periods {

  private final Configuration config;
  private final I18n i18n;

  public Periods(Configuration config, I18n i18n) {
    this.config = config;
    this.i18n = i18n;
  }

  @CheckForNull
  private static String convertDate(@Nullable Date date) {
    if (date != null) {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MMM dd");
      return dateFormat.format(date);
    }
    return null;
  }

  @CheckForNull
  public String label(int periodIndex) {
    String periodProperty = config.get(LEAK_PERIOD + periodIndex).orElse(null);
    PeriodParameters periodParameters = new PeriodParameters(periodProperty);
    return label(periodParameters.getMode(), periodParameters.getParam(), periodParameters.getDate());
  }

  @CheckForNull
  public String abbreviation(int periodIndex) {
    String periodProperty = config.get(LEAK_PERIOD + periodIndex).orElse(null);
    PeriodParameters periodParameters = new PeriodParameters(periodProperty);
    return abbreviation(periodParameters.getMode(), periodParameters.getParam(), periodParameters.getDate());
  }

  @CheckForNull
  public String label(String mode, @Nullable String param, @Nullable Date date) {
    return label(mode, param, convertDate(date), false);
  }

  @CheckForNull
  public String label(String mode, @Nullable String param, @Nullable String date) {
    return label(mode, param, date, false);
  }

  @CheckForNull
  public String abbreviation(String mode, @Nullable String param, @Nullable Date date) {
    return label(mode, param, convertDate(date), true);
  }

  @CheckForNull
  private String label(String mode, @Nullable String param, @Nullable String date, boolean shortLabel) {
    switch (mode) {
      case LEAK_PERIOD_MODE_DAYS:
        return labelForDays(param, date, shortLabel);
      case LEAK_PERIOD_MODE_VERSION:
        return labelForVersion(param, date, shortLabel);
      case LEAK_PERIOD_MODE_PREVIOUS_VERSION:
        return labelForPreviousVersion(param, date, shortLabel);
      case LEAK_PERIOD_MODE_DATE:
        return label("since_x", shortLabel, date);
      default:
        throw new IllegalArgumentException("This mode is not supported : " + mode);
    }
  }

  private String labelForDays(@Nullable String param, @Nullable String date, boolean shortLabel) {
    if (date == null) {
      return label("over_x_days", shortLabel, param);
    }
    return label("over_x_days_detailed", shortLabel, param, date);
  }

  private String labelForVersion(@Nullable String param, @Nullable String date, boolean shortLabel) {
    if (date == null) {
      return label("since_version", shortLabel, param);
    }
    return label("since_version_detailed", shortLabel, param, date);
  }

  private String labelForPreviousVersion(@Nullable String param, @Nullable String date, boolean shortLabel) {
    if (param == null && date == null) {
      return label("since_previous_version", shortLabel);
    }
    if (param == null) {
      // Special case when no snapshot for previous version is found. The first analysis is then returned -> Display only the date.
      return label("since_previous_version_with_only_date", shortLabel, date);
    }
    if (date == null) {
      return label("since_previous_version_detailed", shortLabel, param);
    }
    return label("since_previous_version_detailed", shortLabel, param, date);
  }

  private String label(String key, boolean shortLabel, Object... parameters) {
    String msgKey = key;
    if (shortLabel) {
      msgKey += ".short";
    }
    return i18n.message(ENGLISH, msgKey, null, parameters);
  }

  private static class PeriodParameters {

    private String mode = null;
    private String param = null;
    private Date date = null;

    public PeriodParameters(String periodProperty) {
      checkArgument(isNotBlank(periodProperty), "Period property should not be empty");
      Integer possibleDaysValue = findByDays(periodProperty);
      Date possibleDatesValue = findByDate(periodProperty);
      if (LEAK_PERIOD_MODE_PREVIOUS_VERSION.equals(periodProperty)) {
        mode = periodProperty;
      } else if (possibleDaysValue != null) {
        mode = LEAK_PERIOD_MODE_DAYS;
        param = Integer.toString(possibleDaysValue);
      } else if (possibleDatesValue != null) {
        mode = LEAK_PERIOD_MODE_DATE;
        date = possibleDatesValue;
      } else {
        mode = LEAK_PERIOD_MODE_VERSION;
        param = periodProperty;
      }
    }

    private static Integer findByDays(String property) {
      try {
        return Integer.parseInt(property);
      } catch (NumberFormatException e) {
        return null;
      }
    }

    private static Date findByDate(String property) {
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
