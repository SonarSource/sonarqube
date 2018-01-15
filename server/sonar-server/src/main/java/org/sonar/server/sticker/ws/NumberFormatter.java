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
package org.sonar.server.sticker.ws;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import static org.apache.commons.lang.StringUtils.trim;

public class NumberFormatter {

  private static final String SUFFIX_LIST = " kmbt";

  private NumberFormatter() {
    // Only static methods
  }

  static String formatNumber(long value) {
    if (value == 0) {
      return "0";
    }

    NumberFormat formatter = DecimalFormat.getInstance(Locale.ENGLISH);
    formatter.setMaximumFractionDigits(1);
    int power = (int) StrictMath.log10(value);
    double valueToFormat = value / (Math.pow(10, Math.floorDiv(power, 3) * 3d));
    String formattedNumber = formatter.format(valueToFormat);
    formattedNumber = formattedNumber + SUFFIX_LIST.charAt(power / 3);
    return formattedNumber.length() > 4 ? trim(formattedNumber.replaceAll("\\.[0-9]+", "")) : trim(formattedNumber);
  }
}
