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

/**
 * @since 3.6
 */
public final class TimeUtils {

  private TimeUtils() {
  }

  /**
   * Label for a duration, expressed in numbers of ms, seconds or minutes.
   * <br>
   * Examples:
   * <ul>
   *   <li>10 -&gt; "10ms"</li>
   *   <li>100 -&gt; "100ms"</li>
   *   <li>10000 -&gt; "10s"</li>
   *   <li>100000 -&gt; "1min 40s"</li>
   * </ul>
   */
  public static String formatDuration(long durationInMs) {
    if (durationInMs < 1000) {
      return String.format("%sms", durationInMs);
    } else {
      long sec = durationInMs / 1000;
      if (sec < 60) {
        return String.format("%ss", sec);
      } else {
        long min = sec / 60;
        long remainingSec = sec - (min * 60);
        if (remainingSec > 0) {
          return String.format("%smin %ss", min, remainingSec);
        } else {
          return String.format("%smin", min);
        }
      }
    }
  }
}
