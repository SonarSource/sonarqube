/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.java.signature;

public class Parameter {

  private final JvmJavaType jvmJavaType;
  private final String className;
  private final boolean isArray;

  public Parameter(JvmJavaType jvmJavaType, boolean isArray) {
    this(jvmJavaType, null, isArray);
  }

  public Parameter(String classCanonicalName, boolean isArray) {
    this(JvmJavaType.L, classCanonicalName, isArray);
  }

  Parameter(JvmJavaType jvmJavaType, String classCanonicalName, boolean isArray) {
    if (jvmJavaType == JvmJavaType.L && (classCanonicalName == null || "".equals(classCanonicalName))) {
      throw new IllegalStateException("With an Object JavaType, this is mandatory to specify the canonical name of the class.");
    }
    this.jvmJavaType = jvmJavaType;
    this.className = extractClassName(classCanonicalName);
    this.isArray = isArray;
  }

  public Parameter(Parameter parameter) {
    this(parameter.jvmJavaType, parameter.className, parameter.isArray);
  }

  public boolean isVoid() {
    return jvmJavaType == JvmJavaType.V;
  }

  public JvmJavaType getJvmJavaType() {
    return jvmJavaType;
  }

  public String getClassName() {
    return className;
  }

  public boolean isArray() {
    return isArray;
  }

  public boolean isOject() {
    return jvmJavaType == JvmJavaType.L;
  }

  private String extractClassName(String classCanonicalName) {
    if (classCanonicalName == null) {
      return null;
    }
    int slashIndex = classCanonicalName.lastIndexOf('/');
    int dollarIndex = classCanonicalName.lastIndexOf('$');
    if (slashIndex != -1 || dollarIndex != -1) {
      return classCanonicalName.substring(Math.max(slashIndex, dollarIndex) + 1);
    }
    return classCanonicalName;
  }
}
