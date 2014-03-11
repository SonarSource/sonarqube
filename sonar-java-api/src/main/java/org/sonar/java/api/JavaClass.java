/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.api;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.*;

/**
 * @since 2.6
 * @deprecated in 4.2. Only file system is handled by SonarQube, not logical components.
 */
@Deprecated
public final class JavaClass extends Resource {

  public static final String SCOPE = Scopes.PROGRAM_UNIT;
  public static final String QUALIFIER = Qualifiers.CLASS;
  public static final int UNKNOWN_LINE = -1;

  private int fromLine = UNKNOWN_LINE;
  private int toLine = UNKNOWN_LINE;

  private JavaClass(String name) {
    setKey(name);
  }

  private JavaClass(String name, int fromLine, int toLine) {
    setKey(name);
    this.fromLine = fromLine;
    this.toLine = toLine;
  }

  public String getPackageName() {
    if (StringUtils.contains(getKey(), JavaUtils.PACKAGE_SEPARATOR)) {
      return StringUtils.substringBeforeLast(getKey(), JavaUtils.PACKAGE_SEPARATOR);
    }
    return "";
  }

  public String getClassName() {
    String className = StringUtils.substringAfterLast(getKey(), JavaUtils.PACKAGE_SEPARATOR);
    return StringUtils.defaultIfEmpty(className, getKey());
  }

  public int getFromLine() {
    return fromLine;
  }

  public int getToLine() {
    return toLine;
  }

  @Override
  public String getName() {
    return getKey();
  }

  @Override
  public String getLongName() {
    return getKey();
  }

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public Language getLanguage() {
    return Java.INSTANCE;
  }

  @Override
  public String getScope() {
    return SCOPE;
  }

  @Override
  public String getQualifier() {
    return QUALIFIER;
  }

  @Override
  public Resource getParent() {
    return null;
  }

  @Override
  public boolean matchFilePattern(String antPattern) {
    return false;
  }

  @Override
  public String toString() {
    return getName();
  }

  public static JavaClass create(String name) {
    return new JavaClass(name);
  }

  public static JavaClass create(String packageName, String className) {
    if (StringUtils.isBlank(packageName)) {
      return new JavaClass(className);
    }
    return new JavaClass(toName(packageName, className));
  }

  private static String toName(String packageName, String className) {
    if (StringUtils.isBlank(packageName)) {
      return className;
    }
    return new StringBuilder().append(packageName).append(JavaUtils.PACKAGE_SEPARATOR).append(className).toString();
  }

  public static class Builder {
    private String name;
    private int fromLine = UNKNOWN_LINE;
    private int toLine = UNKNOWN_LINE;

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setName(String packageName, String className) {
      this.name = toName(packageName, className);
      return this;
    }

    public Builder setFromLine(int fromLine) {
      this.fromLine = Math.max(UNKNOWN_LINE, fromLine);
      return this;
    }

    public Builder setToLine(int toLine) {
      this.toLine = Math.max(UNKNOWN_LINE, toLine);
      return this;
    }

    public JavaClass create() {
      return new JavaClass(name, fromLine, toLine);
    }
  }
}
