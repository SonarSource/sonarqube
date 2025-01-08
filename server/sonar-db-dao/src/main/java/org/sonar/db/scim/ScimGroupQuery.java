/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.scim;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ScimGroupQuery {
  private static final Pattern DISPLAY_NAME_FILTER_PATTERN = Pattern.compile("^displayName\\s+eq\\s+\"([^\"]*?)\"$", CASE_INSENSITIVE);
  private static final String UNSUPPORTED_FILTER = "Unsupported filter or value: %s. The only supported filter and operator is 'displayName eq \"displayName\"";
  @VisibleForTesting
  static final ScimGroupQuery ALL = new ScimGroupQuery(null);

  private final String displayName;

  @VisibleForTesting
  protected ScimGroupQuery(@Nullable String displayName) {
    this.displayName = displayName;
  }

  public static ScimGroupQuery fromScimFilter(@Nullable String filter) {
    if (isBlank(filter)) {
      return ALL;
    }
    String userName = getDisplayNameFromFilter(filter)
      .orElseThrow(() -> new IllegalArgumentException(String.format(UNSUPPORTED_FILTER, filter)));

    return new ScimGroupQuery(userName);
  }

  private static Optional<String> getDisplayNameFromFilter(String filter) {
    Matcher matcher = DISPLAY_NAME_FILTER_PATTERN.matcher(filter.trim());
    return matcher.find()
      ? Optional.of(matcher.group(1))
      : Optional.empty();

  }

  public String getDisplayName() {
    return displayName;
  }
}
