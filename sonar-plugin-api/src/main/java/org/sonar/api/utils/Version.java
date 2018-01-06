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

import com.google.common.base.Splitter;
import java.util.List;
import javax.annotation.concurrent.Immutable;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.apache.commons.lang.StringUtils.trimToEmpty;

/**
 * Version composed of maximum four fields (major, minor, patch and build ID numbers) and optionally a qualifier.
 * <p>
 * Examples: 1.0, 1.0.0, 1.2.3, 1.2-beta1, 1.2.1-beta-1, 1.2.3.4567
 * <p>
 * <h3>IMPORTANT NOTE</h3>
 * Qualifier is ignored when comparing objects (methods {@link #equals(Object)}, {@link #hashCode()}
 * and {@link #compareTo(Version)}).
 * <p>
 * <pre>
 *   assertThat(Version.parse("1.2")).isEqualTo(Version.parse("1.2-beta1"));
 *   assertThat(Version.parse("1.2").compareTo(Version.parse("1.2-beta1"))).isZero();
 * </pre>
 *
 * @since 5.5
 */
@Immutable
public class Version implements Comparable<Version> {

  private static final long DEFAULT_BUILD_NUMBER = 0L;
  private static final int DEFAULT_PATCH = 0;
  private static final String DEFAULT_QUALIFIER = "";
  private static final String QUALIFIER_SEPARATOR = "-";
  private static final char SEQUENCE_SEPARATOR = '.';
  private static final Splitter SEQUENCE_SPLITTER = Splitter.on(SEQUENCE_SEPARATOR);

  private final int major;
  private final int minor;
  private final int patch;
  private final long buildNumber;
  private final String qualifier;

  private Version(int major, int minor, int patch, long buildNumber, String qualifier) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.buildNumber = buildNumber;
    this.qualifier = requireNonNull(qualifier, "Version qualifier must not be null");
  }

  public int major() {
    return major;
  }

  public int minor() {
    return minor;
  }

  public int patch() {
    return patch;
  }

  /**
   * Build number if the fourth field, for example {@code 12345} for "6.3.0.12345".
   * If absent, then value is zero.
   * @since 6.3
   */
  public long buildNumber() {
    return buildNumber;
  }

  /**
   * @return non-null suffix. Empty if absent, else the suffix without the first character "-"
   */
  public String qualifier() {
    return qualifier;
  }

  /**
   * Convert a {@link String} to a Version. Supported formats:
   * <ul>
   *   <li>1</li>
   *   <li>1.2</li>
   *   <li>1.2.3</li>
   *   <li>1-beta-1</li>
   *   <li>1.2-beta-1</li>
   *   <li>1.2.3-beta-1</li>
   *   <li>1.2.3.4567</li>
   *   <li>1.2.3.4567-beta-1</li>
   * </ul>
   * Note that the optional qualifier is the part after the first "-".
   *
   * @throws IllegalArgumentException if parameter is badly formatted, for example
   * if it defines 5 integer-sequences.
   */
  public static Version parse(String text) {
    String s = trimToEmpty(text);
    String qualifier = substringAfter(s, QUALIFIER_SEPARATOR);
    if (!qualifier.isEmpty()) {
      s = substringBefore(s, QUALIFIER_SEPARATOR);
    }
    List<String> fields = SEQUENCE_SPLITTER.splitToList(s);
    int major = 0;
    int minor = 0;
    int patch = DEFAULT_PATCH;
    long buildNumber = DEFAULT_BUILD_NUMBER;
    int size = fields.size();
    if (size > 0) {
      major = parseFieldAsInt(fields.get(0));
      if (size > 1) {
        minor = parseFieldAsInt(fields.get(1));
        if (size > 2) {
          patch = parseFieldAsInt(fields.get(2));
          if (size > 3) {
            buildNumber = parseFieldAsLong(fields.get(3));
            if (size > 4) {
              throw new IllegalArgumentException("Maximum 4 fields are accepted: " + text);
            }
          }
        }
      }
    }
    return new Version(major, minor, patch, buildNumber, qualifier);
  }

  public static Version create(int major, int minor) {
    return new Version(major, minor, DEFAULT_PATCH, DEFAULT_BUILD_NUMBER, DEFAULT_QUALIFIER);
  }

  public static Version create(int major, int minor, int patch) {
    return new Version(major, minor, patch, DEFAULT_BUILD_NUMBER, DEFAULT_QUALIFIER);
  }

  /**
   * @deprecated in 6.3 to avoid ambiguity with build number (see {@link #buildNumber()}
   */
  @Deprecated
  public static Version create(int major, int minor, int patch, String qualifier) {
    return new Version(major, minor, patch, DEFAULT_BUILD_NUMBER, qualifier);
  }

  private static int parseFieldAsInt(String field) {
    if (field.isEmpty()) {
      return 0;
    }
    return parseInt(field);
  }

  private static long parseFieldAsLong(String field) {
    if (field.isEmpty()) {
      return 0L;
    }
    return parseLong(field);
  }

  public boolean isGreaterThanOrEqual(Version than) {
    return this.compareTo(than) >= 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Version version = (Version) o;
    if (major != version.major) {
      return false;
    }
    if (minor != version.minor) {
      return false;
    }
    if (patch != version.patch) {
      return false;
    }
    return buildNumber == version.buildNumber;
  }

  @Override
  public int hashCode() {
    int result = major;
    result = 31 * result + minor;
    result = 31 * result + patch;
    result = 31 * result + (int) (buildNumber ^ (buildNumber >>> 32));
    return result;
  }

  @Override
  public int compareTo(Version other) {
    int c = major - other.major;
    if (c == 0) {
      c = minor - other.minor;
      if (c == 0) {
        c = patch - other.patch;
        if (c == 0) {
          long diff = buildNumber - other.buildNumber;
          c = (diff > 0) ? 1 : ((diff < 0) ? -1 : 0);
        }
      }
    }
    return c;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(major).append(SEQUENCE_SEPARATOR).append(minor);
    if (patch > 0 || buildNumber > 0) {
      sb.append(SEQUENCE_SEPARATOR).append(patch);
      if (buildNumber > 0) {
        sb.append(SEQUENCE_SEPARATOR).append(buildNumber);
      }
    }
    if (!qualifier.isEmpty()) {
      sb.append(QUALIFIER_SEPARATOR).append(qualifier);
    }
    return sb.toString();
  }
}
