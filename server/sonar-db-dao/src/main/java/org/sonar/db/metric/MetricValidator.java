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
package org.sonar.db.metric;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class MetricValidator {
  public static final int MAX_KEY_LENGTH = 64;
  public static final int MAX_NAME_LENGTH = 64;
  public static final int MAX_DOMAIN_LENGTH = 64;
  public static final int MAX_DESCRIPTION_LENGTH = 255;

  private MetricValidator() {
    // static utility methods only
  }

  public static String checkMetricKey(String key) {
    checkArgument(!isNullOrEmpty(key), "Metric key cannot be empty");
    checkArgument(key.length() <= MAX_NAME_LENGTH, "Metric key length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      key.length(), MAX_KEY_LENGTH, key);
    return key;
  }

  public static String checkMetricName(String name) {
    checkArgument(!isNullOrEmpty(name), "Metric name cannot be empty");
    checkArgument(name.length() <= MAX_NAME_LENGTH, "Metric name length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      name.length(), MAX_NAME_LENGTH, name);
    return name;
  }

  @CheckForNull
  public static String checkMetricDescription(@Nullable String description) {
    if (description == null) {
      return null;
    }

    checkArgument(description.length() <= MAX_DESCRIPTION_LENGTH, "Metric description length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      description.length(), MAX_DESCRIPTION_LENGTH, description);

    return description;
  }

  @CheckForNull
  public static String checkMetricDomain(@Nullable String domain) {
    if (domain == null) {
      return null;
    }

    checkArgument(domain.length() <= MAX_DOMAIN_LENGTH, "Metric domain length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      domain.length(), MAX_DOMAIN_LENGTH, domain);

    return domain;
  }
}
