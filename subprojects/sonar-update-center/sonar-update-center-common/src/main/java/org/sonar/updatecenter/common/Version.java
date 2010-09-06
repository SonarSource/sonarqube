/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.updatecenter.common;

import org.apache.commons.lang.StringUtils;

public class Version implements Comparable<Version> {

  private String normalizedMajor = normalizePart("0");
  private String normalizedMinor = normalizePart("0");
  private String normalizedPatch = normalizePart("0");
  private String normalizedPatch2 = normalizePart("0");
  private String major = "0";
  private String minor = "0";
  private String patch = "0";
  private String patch2 = "0";
  private String name;

  private Version(String version) {
    this.name = StringUtils.trimToEmpty(version);
    String[] split = StringUtils.split(name, '.');
    if (split.length >= 1) {
      major = split[0];
      normalizedMajor = normalizePart(major);
    }
    if (split.length >= 2) {
      minor = split[1];
      normalizedMinor = normalizePart(minor);
    }
    if (split.length >= 3) {
      patch = split[2];
      normalizedPatch = normalizePart(patch);
    }
    if (split.length >= 4) {
      patch2 = split[3];
      normalizedPatch2 = normalizePart(patch2);
    }
  }

  private static String normalizePart(String part) {
    return StringUtils.leftPad(part, 4, '0');
  }

  public String getMajor() {
    return major;
  }

  public String getMinor() {
    return minor;
  }

  public String getPatch() {
    return patch;
  }

  public String getPatch2() {
    return patch2;
  }

  public String getName() {
    return name;
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
    if (!normalizedMajor.equals(version.normalizedMajor)) {
      return false;
    }
    if (!normalizedMinor.equals(version.normalizedMinor)) {
      return false;
    }
    if (!normalizedPatch.equals(version.normalizedPatch)) {
      return false;
    }
    if (!normalizedPatch2.equals(version.normalizedPatch2)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = normalizedMajor.hashCode();
    result = 31 * result + normalizedMinor.hashCode();
    result = 31 * result + normalizedPatch.hashCode();
    result = 31 * result + normalizedPatch2.hashCode();
    return result;
  }

  public int compareTo(Version other) {
    // TODO : manage RC, alpha, ...
    int c = normalizedMajor.compareTo(other.normalizedMajor);
    if (c == 0) {
      c = normalizedMinor.compareTo(other.normalizedMinor);
      if (c == 0) {
        c = normalizedPatch.compareTo(other.normalizedPatch);
        if (c == 0) {
          c = normalizedPatch2.compareTo(other.normalizedPatch2);
        }
      }
    }
    return c;
  }

  @Override
  public String toString() {
    return name;
  }

  public static Version create(String version) {
    return new Version(version);
  }
}
