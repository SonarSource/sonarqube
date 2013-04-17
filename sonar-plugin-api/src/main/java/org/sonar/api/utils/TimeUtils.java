/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.api.utils;

/**
 * @since 3.6
 */
public final class TimeUtils {

  private TimeUtils() {
  }

  public static String formatDuration(long durationInMs) {
    if (durationInMs < 1000) {
      return String.format("%sms", durationInMs);
    }
    else {
      long sec = durationInMs / 1000;
      if (sec < 60) {
        return String.format("%ss", sec);
      }
      else {
        long min = sec / 60;
        long remainingSec = sec - (min * 60);
        if (remainingSec > 0) {
          return String.format("%smin %ss", min, remainingSec);
        }
        else {
          return String.format("%smin", min);
        }
      }
    }
  }

}
