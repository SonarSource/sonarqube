/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.badge.ws;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.trim;

class SvgFormatter {

  private static final String ZERO = "0";

  private static final String NUMERIC_SUFFIX_LIST = " kmbt";
  private static final String NUMERIC_REGEXP = "\\.[0-9]+";

  private static final String DURATION_MINUTES_FORMAT = "%smin";
  private static final String DURATION_HOURS_FORMAT = "%sh";
  private static final String DURATION_DAYS_FORMAT = "%sd";
  private static final int DURATION_HOURS_IN_DAY = 8;
  private static final double DURATION_ALMOST_ONE = 0.9;
  private static final int DURATION_OF_ONE_HOUR_IN_MINUTES = 60;

  private SvgFormatter() {
    // Only static methods
  }

  static String formatNumeric(long value) {
    if (value == 0) {
      return ZERO;
    }
    NumberFormat numericFormatter = DecimalFormat.getInstance(Locale.ENGLISH);
    numericFormatter.setMaximumFractionDigits(1);
    int power = (int) StrictMath.log10(value);
    double valueToFormat = value / (Math.pow(10, Math.floorDiv(power, 3) * 3d));
    String formattedNumber = numericFormatter.format(valueToFormat);
    formattedNumber = formattedNumber + NUMERIC_SUFFIX_LIST.charAt(power / 3);
    return formattedNumber.length() > 4 ? trim(formattedNumber.replaceAll(NUMERIC_REGEXP, "")) : trim(formattedNumber);
  }

  static String formatPercent(double value) {
    DecimalFormat percentFormatter = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    return percentFormatter.format(value) + "%";
  }

  static String formatDuration(long durationInMinutes) {
    if (durationInMinutes == 0) {
      return ZERO;
    }
    double days = (double) durationInMinutes / DURATION_HOURS_IN_DAY / DURATION_OF_ONE_HOUR_IN_MINUTES;
    if (days > DURATION_ALMOST_ONE) {
      return format(DURATION_DAYS_FORMAT, Math.round(days));
    }
    double remainingDuration = durationInMinutes - (Math.floor(days) * DURATION_HOURS_IN_DAY * DURATION_OF_ONE_HOUR_IN_MINUTES);
    double hours = remainingDuration / DURATION_OF_ONE_HOUR_IN_MINUTES;
    if (hours > DURATION_ALMOST_ONE) {
      return format(DURATION_HOURS_FORMAT, Math.round(hours));
    }
    double minutes = remainingDuration - (Math.floor(hours) * DURATION_OF_ONE_HOUR_IN_MINUTES);
    return format(DURATION_MINUTES_FORMAT, Math.round(minutes));
  }

}
