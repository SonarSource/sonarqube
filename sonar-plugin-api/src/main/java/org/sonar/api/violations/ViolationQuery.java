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
package org.sonar.api.violations;

import org.sonar.api.resources.Resource;

/**
 * Class that allows to query the Sonar index about violations.
 * 
 * @since 2.8
 */
public final class ViolationQuery {

  private boolean ignoreSwitchedOff;
  private Resource resource;

  /**
   * Use the factory method <code>create()</code>
   */
  ViolationQuery() {
  }

  /**
   * Creates a new {@link ViolationQuery} object.
   * 
   * @return the new query
   */
  public static ViolationQuery create() {
    return new ViolationQuery();
  }

  /**
   * Specifies if the query should returned switched-off violations or not.
   * 
   * @param ignore
   *          if true, the query will return only active violations.
   * @return the current violation query
   */
  public ViolationQuery ignoreSwitchedOff(boolean ignore) {
    this.ignoreSwitchedOff = ignore;
    return this;
  }

  /**
   * Tells if the query should returned switched-off violations or not.
   * 
   * @return
   */
  public boolean ignoreSwitchedOff() {
    return ignoreSwitchedOff;
  }

  /**
   * Specifies the resource which violations are search from.
   * 
   * @param resource
   *          the resource
   */
  public ViolationQuery forResource(Resource resource) {
    this.resource = resource;
    return this;
  }

  /**
   * Returns the resource which violations are search from.
   * 
   * @return the resource
   */
  public Resource getResource() {
    return resource;
  }
}
