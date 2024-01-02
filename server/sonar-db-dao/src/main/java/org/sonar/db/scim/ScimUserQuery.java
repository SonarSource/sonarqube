/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.apache.commons.lang.StringUtils.isBlank;

public class ScimUserQuery {
  private static final Pattern USERNAME_FILTER_PATTERN = Pattern.compile("^userName\\s+eq\\s+\"([^\"]*?)\"$", CASE_INSENSITIVE);
  private static final String UNSUPPORTED_FILTER = "Unsupported filter value: %s. Format should be 'userName eq \"username\"'";

  private final String userName;

  private ScimUserQuery(String userName) {
    this.userName = userName;
  }

  @CheckForNull
  public String getUserName() {
    return userName;
  }

  public static ScimUserQuery empty() {
    return builder().build();
  }

  public static ScimUserQuery fromScimFilter(@Nullable String filter) {
    if (isBlank(filter)) {
      return empty();
    }

    String userName = getUserNameFromFilter(filter)
      .orElseThrow(() -> new IllegalStateException(String.format(UNSUPPORTED_FILTER, filter)));

    return builder().userName(userName).build();
  }

  private static Optional<String> getUserNameFromFilter(String filter) {
    Matcher matcher = USERNAME_FILTER_PATTERN.matcher(filter.trim());
    return matcher.find()
      ? Optional.of(matcher.group(1))
      : Optional.empty();
  }

  public static ScimUserQueryBuilder builder() {
    return new ScimUserQueryBuilder();
  }

  public static final class ScimUserQueryBuilder {

    private String userName;

    private ScimUserQueryBuilder() {
    }

    public ScimUserQueryBuilder userName(String userName) {
      this.userName = userName;
      return this;
    }

    public ScimUserQuery build() {
      return new ScimUserQuery(userName);
    }

  }
}
