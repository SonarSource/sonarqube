/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.AnnotationUtils;

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

  public static boolean isDeprecatedScannerSide(Object extension) {
    return AnnotationUtils.getAnnotation(extension, org.sonar.api.batch.ScannerSide.class) != null;
  }

  public static boolean isScannerSide(Object extension) {
    return AnnotationUtils.getAnnotation(extension, ScannerSide.class) != null;
  }

}
