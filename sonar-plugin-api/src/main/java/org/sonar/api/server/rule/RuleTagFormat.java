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
package org.sonar.api.server.rule;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.toSet;

/**
 * The characters allowed in rule tags are the same as those on StackOverflow, basically lower-case
 * letters, digits, plus (+), sharp (#), dash (-) and dot (.)
 * See http://meta.stackoverflow.com/questions/22624/what-symbols-characters-are-not-allowed-in-tags
 * @since 4.2
 */
public class RuleTagFormat {

  private static final String ERROR_MESSAGE_SUFFIX = "Rule tags accept only the characters: a-z, 0-9, '+', '-', '#', '.'";

  private static final String VALID_CHARACTERS_REGEX = "^[a-z0-9\\+#\\-\\.]+$";

  private RuleTagFormat() {
    // only static methods
  }

  public static boolean isValid(String tag) {
    return StringUtils.isNotBlank(tag) && tag.matches(VALID_CHARACTERS_REGEX);
  }

  public static String validate(String tag) {
    if (!isValid(tag)) {
      throw new IllegalArgumentException(format("Tag '%s' is invalid. %s", tag, ERROR_MESSAGE_SUFFIX));
    }
    return tag;
  }

  public static Set<String> validate(Collection<String> tags) {
    Set<String> sanitizedTags = tags.stream()
      .filter(Objects::nonNull)
      .filter(tag -> !tag.isEmpty())
      .map(tag -> tag.toLowerCase(ENGLISH))
      .collect(toSet());
    Set<String> invalidTags = sanitizedTags.stream()
      .filter(tag -> !isValid(tag))
      .collect(toSet());
    if (invalidTags.isEmpty()) {
      return sanitizedTags;
    }
    throw new IllegalArgumentException(format("Tags '%s' are invalid. %s", join(", ", invalidTags), ERROR_MESSAGE_SUFFIX));
  }

}
