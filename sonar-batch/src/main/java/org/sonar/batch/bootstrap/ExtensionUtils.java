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
package org.sonar.batch.bootstrap;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.RequiresDB;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

public class ExtensionUtils {

  private ExtensionUtils() {
    // only static methods
  }

  public static boolean isInstantiationStrategy(Object extension, String strategy) {
    InstantiationStrategy annotation = AnnotationUtils.getAnnotation(extension, InstantiationStrategy.class);
    if (annotation != null) {
      return strategy.equals(annotation.value());
    }
    return InstantiationStrategy.PER_PROJECT.equals(strategy);
  }

  public static boolean isBatchSide(Object extension) {
    return AnnotationUtils.getAnnotation(extension, BatchSide.class) != null;
  }

  public static boolean supportsEnvironment(Object extension, EnvironmentInformation environment) {
    SupportedEnvironment env = AnnotationUtils.getAnnotation(extension, SupportedEnvironment.class);
    if (env == null) {
      return true;
    }
    for (String supported : env.value()) {
      if (StringUtils.equalsIgnoreCase(environment.getKey(), supported)) {
        return true;
      }
    }
    return false;
  }

  public static boolean requiresDB(Object extension) {
    return AnnotationUtils.getAnnotation(extension, RequiresDB.class) != null;
  }

  public static boolean isMavenExtensionOnly(Object extension) {
    SupportedEnvironment env = AnnotationUtils.getAnnotation(extension, SupportedEnvironment.class);
    return env != null && env.value().length == 1 && StringUtils.equalsIgnoreCase("maven", env.value()[0]);
  }

  public static boolean isType(Object extension, Class<?> extensionClass) {
    Class clazz = extension instanceof Class ? (Class) extension : extension.getClass();
    return extensionClass.isAssignableFrom(clazz);
  }
}
