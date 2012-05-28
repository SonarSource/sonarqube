/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.batch.bootstrap;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.Extension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.core.NotDryRun;

public final class ExtensionUtils {

  private ExtensionUtils() {
    // only static methods
  }

  static boolean isInstantiationStrategy(Object extension, String strategy) {
    Class clazz = (extension instanceof Class ? (Class) extension : extension.getClass());
    InstantiationStrategy extStrategy = AnnotationUtils.getAnnotation(clazz, InstantiationStrategy.class);
    if (extStrategy != null) {
      return strategy.equals(extStrategy.value());
    }
    return InstantiationStrategy.PER_PROJECT.equals(strategy);
  }

  static boolean isBatchExtension(Object extension) {
    return isType(extension, BatchExtension.class);
  }

  static boolean isSupportedEnvironment(Object extension, EnvironmentInformation environment) {
    Class clazz = (extension instanceof Class ? (Class) extension : extension.getClass());
    SupportedEnvironment env = AnnotationUtils.getAnnotation(clazz, SupportedEnvironment.class);
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

  static boolean checkDryRun(Object extension, boolean dryRun) {
    return !dryRun || AnnotationUtils.getAnnotation(extension, NotDryRun.class) == null;
  }

  static boolean isMavenExtensionOnly(Object extension) {
    Class clazz = (extension instanceof Class ? (Class) extension : extension.getClass());
    SupportedEnvironment env = AnnotationUtils.getAnnotation(clazz, SupportedEnvironment.class);
    return env!=null && env.value().length==1 && StringUtils.equalsIgnoreCase("maven", env.value()[0]);
  }

  static boolean isType(Object extension, Class<? extends Extension> extensionClass) {
    Class clazz = (extension instanceof Class ? (Class) extension : extension.getClass());
    return extensionClass.isAssignableFrom(clazz);
  }
}
