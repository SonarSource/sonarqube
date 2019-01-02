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
package org.sonar.api.utils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * @since 2.2
 */
public final class ManifestUtils {

  private ManifestUtils() {
  }

  /**
   * Search for a property in all the manifests found in the classloader
   *
   * @return the values, an empty list if the property is not found.
   */
  public static List<String> getPropertyValues(ClassLoader classloader, String key) {
    List<String> values = new ArrayList<>();
    try {
      Enumeration<URL> resources = classloader.getResources("META-INF/MANIFEST.MF");
      while (resources.hasMoreElements()) {
        Manifest manifest = new Manifest(resources.nextElement().openStream());
        Attributes attributes = manifest.getMainAttributes();
        String value = attributes.getValue(key);
        if (value != null) {
          values.add(value);
        }
      }
    } catch (IOException e) {
      throw new SonarException("Fail to load manifests from classloader: " + classloader, e);
    }
    return values;
  }
}
