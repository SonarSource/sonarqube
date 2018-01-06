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
package org.sonar.api.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Utility to parse various inputs
 *
 * @since 1.10
 */
public final class ParsingUtils {

  private ParsingUtils() {
  }

  /**
   * Parses a string with a locale and returns the corresponding number
   *
   * @throws ParseException if number cannot be parsed
   */
  public static double parseNumber(String number, Locale locale) throws ParseException {
    if ("".equals(number)) {
      return Double.NaN;
    }
    return NumberFormat.getNumberInstance(locale).parse(number).doubleValue();
  }

  /**
   * Parses a string with the default locale and returns the corresponding number
   *
   * @throws ParseException if number cannot be parsed
   */
  public static double parseNumber(String number) throws ParseException {
    return parseNumber(number, Locale.ENGLISH);
  }

  /**
   * Scales a double value, taking into account 2 decimals
   */
  public static double scaleValue(double value) {
    return scaleValue(value, 2);
  }

  /**
   * Scales a double value with decimals
   */
  public static double scaleValue(double value, int decimals) {
    BigDecimal bd = BigDecimal.valueOf(value);
    return bd.setScale(decimals, RoundingMode.HALF_UP).doubleValue();
  }

}
