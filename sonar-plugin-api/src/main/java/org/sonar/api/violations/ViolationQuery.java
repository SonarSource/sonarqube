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
package org.sonar.api.violations;

import org.sonar.api.resources.Resource;

/**
 * Class that allows to query the Sonar index about violations.
 * 
 * @since 2.8
 * @deprecated in 3.6 for the merge of violations and reviews into issues.
 */
@Deprecated
public final class ViolationQuery {

  public enum SwitchMode {
    OFF, ON, BOTH
  }

  private SwitchMode switchMode = SwitchMode.ON;
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
   * Specifies if the query should return only switched-off violations.
   * 
   * @param b
   *          if true, the query will return only switched-off violations. if false, it will return only active violations.
   * @return the current violation query
   */
  public ViolationQuery setSwitchedOff(boolean b) {
    this.switchMode = b ? SwitchMode.OFF : SwitchMode.ON;
    return this;
  }

  /**
   * Tells if the query should return only switched-off violations.
   */
  public boolean isSwitchedOff() {
    return switchMode == SwitchMode.OFF;
  }

  public SwitchMode getSwitchMode() {
    return switchMode;
  }

  public ViolationQuery setSwitchMode(SwitchMode s) {
    this.switchMode = s;
    return this;
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
