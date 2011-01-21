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
package org.sonar.java.api;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;

/**
 * @since 2.6
 */
public final class JavaMethod extends Resource {

  public static final int UNKNOWN_LINE = -1;
  private static final String CLASS_SEPARATOR = "#";

  private String signature;
  private String className;
  private int fromLine;
  private int toLine;

  private JavaMethod(String className, String signature) {
    setKey(toKey(className, signature));
    this.className = className;
    this.signature = signature;
  }

  private JavaMethod(String className, String signature, int fromLine, int toLine) {
    this(className, signature);
    this.fromLine = fromLine;
    this.toLine = toLine;
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
  public Language getLanguage() {
    return Java.INSTANCE;
  }

  @Override
  public String getScope() {
    return null;
  }

  @Override
  public String getQualifier() {
    return Qualifiers.METHOD;
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    JavaMethod that = (JavaMethod) o;
    return getKey().equals(that.getKey());
  }

  @Override
  public int hashCode() {
    return getKey().hashCode();
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

    public JavaMethod create() {
      return new JavaMethod(className, signature, fromLine, toLine);
    }
  }
}
