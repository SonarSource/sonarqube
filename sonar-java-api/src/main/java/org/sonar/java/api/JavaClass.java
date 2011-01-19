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
import org.sonar.api.resources.*;

/**
 * @since 2.6
 */
public final class JavaClass extends Resource {

  private String name;

  private JavaClass(String name) {
    this.name = name;
    setKey(name);
  }

  public String getPackageName() {
    return StringUtils.substringBeforeLast(name, JavaUtils.PACKAGE_SEPARATOR);
  }

  public String getClassName() {
    return StringUtils.substringAfterLast(name, JavaUtils.PACKAGE_SEPARATOR);
  }

  @Override
  public String getName() {
    return getClassName();
  }

  @Override
  public String getLongName() {
    return name;
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
    return ResourceScopes.TYPE;
  }

  @Override
  public String getQualifier() {
    return ResourceQualifiers.CLASS;
  }

  @Override
  public Resource getParent() {
    return null;
  }

  @Override
  public boolean matchFilePattern(String antPattern) {
    return false;
  }

  public static JavaClass create(String name) {
    return new JavaClass(name);
  }

  public static JavaClass create(String packageName, String className) {
    if (StringUtils.isBlank(packageName)) {
      return new JavaClass(className);
    }
    String name = new StringBuilder().append(packageName).append(JavaUtils.PACKAGE_SEPARATOR).append(className).toString();
    return new JavaClass(name);
  }
}
