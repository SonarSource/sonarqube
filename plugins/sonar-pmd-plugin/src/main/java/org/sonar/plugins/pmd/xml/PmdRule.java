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
package org.sonar.plugins.pmd.xml;

import java.util.ArrayList;
import java.util.List;

public class PmdRule {

  private String ref;

  private String priority;

  private String name;

  private String message;

  private List<PmdProperty> properties = new ArrayList<PmdProperty>();

  private String clazz;// NOSONAR unused private field

  public PmdRule(String ref) {
    this(ref, null);
  }

  public PmdRule(String ref, String priority) {
    this.ref = ref;
    this.priority = priority;
  }

  public String getRef() {
    return ref;
  }

  public void setProperties(List<PmdProperty> properties) {
    this.properties = properties;
  }

  public List<PmdProperty> getProperties() {
    return properties;
  }

  public PmdProperty getProperty(String propertyName) {
    for (PmdProperty prop : properties) {
      if (propertyName.equals(prop.getName())) {
        return prop;
      }
    }
    return null;
  }

  public int compareTo(String o) {
    return o.compareTo(ref);
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public void addProperty(PmdProperty property) {
    if (properties == null) {
      properties = new ArrayList<PmdProperty>();
    }
    properties.add(property);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public String getClazz() {
    return clazz;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }

  public void removeProperty(String propertyName) {
    PmdProperty prop = getProperty(propertyName);
    properties.remove(prop);
  }

  public void setClazz(String clazz) {
    this.clazz = clazz;
  }

  public String getName() {
    return name;
  }

  public boolean hasProperties() {
    return properties != null && !properties.isEmpty();
  }
}