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
package org.sonar.api.resources;

/**
 * Resource scopes are not extendable by plugins.
 */
public interface Scopes {
  /**
   * For example view, subview, project, module or library. Persisted in database.
   */
  String PROJECT = "PRJ";

  /**
   * For example directory or Java package. Persisted in database.
   */
  String NAMESPACE = "DIR";

  /**
   * For example a Java file. Persisted in database.
   */
  String FILE = "FIL";

  /**
   * For example a Java class or a Java interface. Not persisted in database.
   */
  String TYPE = "TYP";

  /**
   * For example a Java method. Not persisted in database.
   */
  String METHOD = "MET";

  
  String[] ORDERED_SCOPES = {PROJECT, NAMESPACE, FILE, TYPE, METHOD};
}
