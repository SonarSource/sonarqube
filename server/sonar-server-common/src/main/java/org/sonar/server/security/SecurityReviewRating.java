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

package org.sonar.server.security;

import org.sonar.server.measure.Rating;

public class SecurityReviewRating {

  private SecurityReviewRating() {
    // Only static method
  }

  public static Rating compute(int ncloc, int securityHotspots) {
    if (ncloc == 0) {
      return Rating.A;
    }
    double ratio = (double) securityHotspots * 1000d / (double) ncloc;
    if (ratio <= 3d) {
      return Rating.A;
    } else if (ratio <= 10) {
      return Rating.B;
    } else if (ratio <= 15) {
      return Rating.C;
    } else if (ratio <= 25) {
      return Rating.D;
    } else {
      return Rating.E;
    }
  }
}
