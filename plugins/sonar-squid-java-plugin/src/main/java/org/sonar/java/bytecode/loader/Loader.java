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
package org.sonar.java.bytecode.loader;

import java.net.URL;

/**
 * Specifies resource loading behavior.
 */
interface Loader {

  /**
   * Finds the resource with the given name.
   * 
   * @param name resource name
   * @return an <tt>URL</tt> object for reading the resource, or
   *          <tt>null</tt> if the resource could not be found
   * @throws IllegalStateException if loader has been closed
   */
  URL findResource(String name);

  /**
   * Loads bytes of the resource with the given name.
   * 
   * @param name resource name
   * @return an array of <tt>byte</tt>s, or
   *         <tt>null</tt> if the resource could not be found or could not be loaded for some reason
   * @throws IllegalStateException if loader has been closed
   */
  byte[] loadBytes(String name);

  /**
   * Closes this loader, so that it can no longer be used to load new resources.
   * If loader is already closed, then invoking this method has no effect.
   */
  void close();

}
