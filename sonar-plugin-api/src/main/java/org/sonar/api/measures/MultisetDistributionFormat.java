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
package org.sonar.api.measures;

import com.google.common.collect.Multiset;
import org.sonar.api.utils.KeyValueFormat;

/**
 * Format internal {@link com.google.common.collect.Multiset} of {@link CountDistributionBuilder}
 * and {@link RangeDistributionBuilder}
 */
class MultisetDistributionFormat {

  private MultisetDistributionFormat() {
    // only statics
  }

  static String format(Multiset countBag) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Object obj : countBag.elementSet()) {
      if (!first) {
        sb.append(KeyValueFormat.PAIR_SEPARATOR);
      }
      sb.append(obj.toString());
      sb.append(KeyValueFormat.FIELD_SEPARATOR);
      // -1 allows to include zero values
      sb.append(countBag.count(obj) - 1);
      first = false;
    }
    return sb.toString();
  }
}
