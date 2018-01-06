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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.lang.StringUtils;

/**
 * Implementation of Ant-style matching patterns.
 * Contrary to other implementations (like AntPathMatcher from Spring Framework) it is based on {@link Pattern Java Regular Expressions}.
 * To increase performance it holds an internal cache of all processed patterns.
 * <p>
 * Following rules are applied:
 * <ul>
 * <li>? matches single character</li>
 * <li>* matches zero or more characters</li>
 * <li>** matches zero or more 'directories'</li>
 * </ul>
 * <p>
 * Some examples of patterns:
 * <ul>
 * <li><code>org/T?st.java</code> - matches <code>org/Test.java</code> and also <code>org/Tost.java</code></li>
 * <li><code>org/*.java</code> - matches all <code>.java</code> files in the <code>org</code> directory,
 * e.g. <code>org/Foo.java</code> or <code>org/Bar.java</code></li>
 * <li><code>org/**</code> - matches all files underneath the <code>org</code> directory,
 * e.g. <code>org/Foo.java</code> or <code>org/foo/bar.jsp</code></li>
 * <li><code>org/&#42;&#42;/Test.java</code> - matches all <code>Test.java</code> files underneath the <code>org</code> directory,
 * e.g. <code>org/Test.java</code> or <code>org/foo/Test.java</code> or <code>org/foo/bar/Test.java</code></li>
 * <li><code>org/&#42;&#42;/*.java</code> - matches all <code>.java</code> files underneath the <code>org</code> directory,
 * e.g. <code>org/Foo.java</code> or <code>org/foo/Bar.java</code> or <code>org/foo/bar/Baz.java</code></li>
 * </ul>
 * <p>
 * Another implementation, which is also based on Java Regular Expressions, can be found in
 * <a href="https://github.com/JetBrains/intellij-community/blob/idea/107.743/platform/util/src/com/intellij/openapi/util/io/FileUtil.java#L847">FileUtil</a>
 * from IntelliJ OpenAPI.
 * 
 * @since 1.10
 */
@ThreadSafe
public class WildcardPattern {

  private static final Map<String, WildcardPattern> CACHE = Collections.synchronizedMap(new HashMap<>());
  private static final String SPECIAL_CHARS = "()[]^$.{}+|";

  private Pattern pattern;
  private String stringRepresentation;

  protected WildcardPattern(String pattern, String directorySeparator) {
    this.stringRepresentation = pattern;
    this.pattern = Pattern.compile(toRegexp(pattern, directorySeparator));
  }

  private static String toRegexp(String antPattern, String directorySeparator) {
    final String escapedDirectorySeparator = '\\' + directorySeparator;

    final StringBuilder sb = new StringBuilder(antPattern.length());

    sb.append('^');

    int i = antPattern.startsWith("/") || antPattern.startsWith("\\") ? 1 : 0;
    while (i < antPattern.length()) {
      final char ch = antPattern.charAt(i);

      if (SPECIAL_CHARS.indexOf(ch) != -1) {
        // Escape regexp-specific characters
        sb.append('\\').append(ch);
      } else if (ch == '*') {
        if (i + 1 < antPattern.length() && antPattern.charAt(i + 1) == '*') {
          // Double asterisk
          // Zero or more directories
          if (i + 2 < antPattern.length() && isSlash(antPattern.charAt(i + 2))) {
            sb.append("(?:.*").append(escapedDirectorySeparator).append("|)");
            i += 2;
          } else {
            sb.append(".*");
            i += 1;
          }
        } else {
          // Single asterisk
          // Zero or more characters excluding directory separator
          sb.append("[^").append(escapedDirectorySeparator).append("]*?");
        }
      } else if (ch == '?') {
        // Any single character excluding directory separator
        sb.append("[^").append(escapedDirectorySeparator).append("]");
      } else if (isSlash(ch)) {
        // Directory separator
        sb.append(escapedDirectorySeparator);
      } else {
        // Single character
        sb.append(ch);
      }

      i++;
    }

    sb.append('$');

    return sb.toString();
  }

  private static boolean isSlash(char ch) {
    return ch == '/' || ch == '\\';
  }

  /**
   * Returns string representation of this pattern.
   * 
   * @since 2.5
   */
  @Override
  public String toString() {
    return stringRepresentation;
  }

  /**
   * Returns true if specified value matches this pattern.
   */
  public boolean match(String value) {
    value = StringUtils.removeStart(value, "/");
    value = StringUtils.removeEnd(value, "/");
    return pattern.matcher(value).matches();
  }

  /**
   * Returns true if specified value matches one of specified patterns.
   * 
   * @since 2.4
   */
  public static boolean match(WildcardPattern[] patterns, String value) {
    for (WildcardPattern pattern : patterns) {
      if (pattern.match(value)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates pattern with "/" as a directory separator.
   * 
   * @see #create(String, String)
   */
  public static WildcardPattern create(String pattern) {
    return create(pattern, "/");
  }

  /**
   * Creates array of patterns with "/" as a directory separator.
   * 
   * @see #create(String, String)
   */
  public static WildcardPattern[] create(@Nullable String[] patterns) {
    if (patterns == null) {
      return new WildcardPattern[0];
    }
    WildcardPattern[] exclusionPAtterns = new WildcardPattern[patterns.length];
    for (int i = 0; i < patterns.length; i++) {
      exclusionPAtterns[i] = create(patterns[i]);
    }
    return exclusionPAtterns;
  }

  /**
   * Creates pattern with specified separator for directories.
   * <p>
   * This is used to match Java-classes, i.e. <code>org.foo.Bar</code> against <code>org/**</code>.
   * <b>However usage of character other than "/" as a directory separator is misleading and should be avoided,
   * so method {@link #create(String)} is preferred over this one.</b>
   * 
   * <p>
   * Also note that no matter whether forward or backward slashes were used in the <code>antPattern</code>
   * the returned pattern will use <code>directorySeparator</code>.
   * Thus to match Windows-style path "dir\file.ext" against pattern "dir/file.ext" normalization should be performed.
   * 
   */
  public static WildcardPattern create(String pattern, String directorySeparator) {
    String key = pattern + directorySeparator;
    return CACHE.computeIfAbsent(key, k -> new WildcardPattern(pattern, directorySeparator));
  }
}
