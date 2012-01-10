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
package org.sonar.plugins.design.ui.dependencies.client;

import org.sonar.wsclient.services.Dependency;
import org.sonar.wsclient.services.Resource;

import java.util.List;

public class Data {

  private long resourceId;
  private List<Dependency> dependencies = null;
  private Resource resource = null;

  public Data(long resourceId) {
    this.resourceId = resourceId;
  }

  public long getResourceId() {
    return resourceId;
  }

  public boolean isLoaded() {
    return dependencies!=null && resource!=null;
  }

  public boolean canDisplay() {
    return resource.getMeasure("ca")!=null && resource.getMeasure("ce")!=null; 
  }

  public List<Dependency> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<Dependency> dependencies) {
    this.dependencies = dependencies;
  }

  public Resource getResource() {
    return resource;
  }

  public void setMeasures(Resource resource) {
    this.resource = resource;
  }
}
