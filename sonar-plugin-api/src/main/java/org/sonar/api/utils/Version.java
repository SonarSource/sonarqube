/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.apache.commons.lang.StringUtils.trimToEmpty;

/**
 * TODO document equals/comparison without qualifier
 * @since 5.5
 */
@Immutable
public class Version implements Comparable<Version> {

  private static final String QUALIFIER_SEPARATOR = "-";
  private static final char SEQUENCE_SEPARATOR = '.';
  private static final Splitter SEQUENCE_SPLITTER = Splitter.on(SEQUENCE_SEPARATOR);

  private final int major;
  private final int minor;
  private final int patch;
  private final String qualifier;

  private Version(int major, int minor, int patch, String qualifier) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.qualifier = qualifier;
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
   * @return non-null suffix
   */
  public String qualifier() {
    return qualifier;
  }

  public static Version parse(String text) {
    String s = trimToEmpty(text);
    String qualifier = substringAfter(s, QUALIFIER_SEPARATOR);
    if (!qualifier.isEmpty()) {
      s = substringBefore(s, QUALIFIER_SEPARATOR);
    }
    List<String> split = SEQUENCE_SPLITTER.splitToList(s);
    int major = 0;
    int minor = 0;
    int patch = 0;
    if (split.size() > 0) {
      major = parseSequence(split.get(0));
      if (split.size() > 1) {
        minor = parseSequence(split.get(1));
        if (split.size() > 2) {
          patch = parseSequence(split.get(2));
          if (split.size() > 3) {
            throw new IllegalArgumentException("Only 3 sequences are accepted");
          }
        }
      }
    }
    return new Version(major, minor, patch, qualifier);
  }

  public static Version create(int major, int minor) {
    return new Version(major, minor, 0, "");
  }

  public static Version create(int major, int minor, int patch) {
    return new Version(major, minor, patch, "");
  }

  public static Version create(int major, int minor, int patch, String qualifier) {
    return new Version(major, minor, patch, qualifier);
  }

  private static int parseSequence(String sequence) {
    if (sequence.isEmpty()) {
      return 0;
    }
    return parseInt(sequence);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Version)) {
      return false;
    }
    Version other = (Version) o;
    return major == other.major && minor == other.minor && patch == other.patch;
  }

  @Override
  public int hashCode() {
    int result = major;
    result = 31 * result + minor;
    result = 31 * result + patch;
    return result;
  }

  @Override
  public int compareTo(Version other) {
    int c = major - other.major;
    if (c == 0) {
      c = minor - other.minor;
      if (c == 0) {
        c = patch - other.patch;
      }
    }
    return c;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(major).append(SEQUENCE_SEPARATOR).append(minor);
    if (patch > 0) {
      sb.append(SEQUENCE_SEPARATOR).append(patch);
    }
    if (!qualifier.isEmpty()) {
      sb.append(QUALIFIER_SEPARATOR).append(qualifier);
    }
    return sb.toString();
  }
}
