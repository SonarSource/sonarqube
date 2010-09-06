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
package org.sonar.api.web.gwt.client.webservices;

import java.util.List;

public class Resources extends ResponsePOJO {

  private List<Resource> resources;

  public Resources(List<Resource> resources) {
    super();
    this.resources = resources;
  }

  public List<Resource> getResources() {
    return resources;
  }
  
  public Resource firstResource() {
    return resources.size() > 0 ? resources.get(0) : null;
  }

  public boolean onceContainsMeasure(WSMetrics.Metric metric) {
    for (Resource resource : resources) {
      if (resource.getMeasure(metric) != null) {
        return true;
      }
    }
    return false;
  }

  public boolean allContainsMeasure(WSMetrics.Metric metric) {
    for (Resource resource : resources) {
      if (resource.getMeasure(metric) == null) {
        return false;
      }
    }
    return true;
  }
  
  public boolean isEmpty() {
    return resources == null || resources.isEmpty();
  }

}
