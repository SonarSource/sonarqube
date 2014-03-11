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
public final class JavaMethod extends Method {

  public static final String QUALIFIER = Qualifiers.METHOD;

  public static final int UNKNOWN_LINE = -1;
  private static final String CLASS_SEPARATOR = "#";

  private String signature;
  private String className;
  private int fromLine;
  private int toLine;
  private boolean isAccessor = false;

  private JavaMethod(String className, String signature) {
    super(toKey(className, signature), QUALIFIER, Java.INSTANCE);
    this.className = className;
    this.signature = signature;
  }

  private JavaMethod(String className, String signature, int fromLine, int toLine, boolean isAccessor) {
    this(className, signature);
    this.fromLine = fromLine;
    this.toLine = toLine;
    this.isAccessor = isAccessor;
  }

  public int getFromLine() {
    return fromLine;
  }

  public int getToLine() {
    return toLine;
  }

  public String getSignature() {
    return signature;
  }

  public String getClassName() {
    return className;
  }

  public boolean isAccessor() {
    return isAccessor;
  }

  @Override
  public String getName() {
    return signature;
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
  public Resource getParent() {
    return null;
  }

  @Override
  public String toString() {
    return getKey();
  }

  public static JavaMethod createRef(String key) {
    String[] parts = splitClassAndMethodFromKey(key);
    return new JavaMethod(parts[0], parts[1]);
  }

  private static String[] splitClassAndMethodFromKey(String key) {
    String[] parts = StringUtils.split(key, CLASS_SEPARATOR);
    if (parts.length!=2) {
      throw new IllegalArgumentException("Java method does not respect the format: org.foo.Bar#methodName(LString;)V. Got: " + key);
    }
    return parts;
  }

  public static JavaMethod createRef(JavaClass javaClass, String signature) {
    return new JavaMethod(javaClass.getName(), signature);
  }

  static String toKey(JavaClass javaClass, String signature) {
    return toKey(javaClass.getName(), signature);
  }

  static String toKey(String className, String signature) {
    return new StringBuilder().append(className).append(CLASS_SEPARATOR).append(signature).toString();
  }

  public static class Builder {
    private String className;
    private String signature;
    private int fromLine = UNKNOWN_LINE;
    private int toLine = UNKNOWN_LINE;
    private boolean isAccessor = false;

    public Builder setKey(String key) {
      String[] parts = splitClassAndMethodFromKey(key);
      this.className = parts[0];
      this.signature = parts[1];
      return this;
    }

    public Builder setClass(String className) {
      this.className = className;
      return this;
    }

    public Builder setClass(JavaClass javaClass) {
      this.className = javaClass.getName();
      return this;
    }

    public Builder setSignature(String signature) {
      this.signature = signature;
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

    public Builder setAccessor(boolean accessor) {
      isAccessor = accessor;
      return this;
    }

    public JavaMethod create() {
      return new JavaMethod(className, signature, fromLine, toLine, isAccessor);
    }
  }
}
