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
package org.sonar.api;

/**
 * Plugin entry-point used to declare its extensions (see {@link org.sonar.api.Extension}.
 * <p/>
 * <p>The JAR manifest must declare the name of the implementation class in the property <code>Plugin-Class</code>.
 * This property is automatically set by sonar-packaging-maven-plugin when building plugin.</p>
 * 
 * @since 2.8
 */
public abstract class SonarPlugin implements Plugin {

  @Override
  public final String getKey() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final String getName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final String getDescription() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a string representation of the plugin, suitable for debugging purposes only.
   */
  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
