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
package org.sonar.plugins.pmd.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.util.ArrayList;
import java.util.List;

@XStreamAlias("rule")
public class PmdRule implements Comparable<String> {

  @XStreamAsAttribute
  private String ref;

  private String priority;

  private List<Property> properties;

  @XStreamOmitField
  private String description; //NOSONAR unused private field

  @XStreamOmitField
  private String exclude;//NOSONAR unused private field

  @XStreamOmitField
  private String example;//NOSONAR unused private field

  @XStreamOmitField
  @XStreamAlias(value = "class")
  private String clazz;//NOSONAR unused private field

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

  public void setProperties(List<Property> properties) {
    this.properties = properties;
  }

  public List<Property> getProperties() {
    return properties;
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

  public void addProperty(Property property) {
    if (properties == null) {
      properties = new ArrayList<Property>();
    }
    properties.add(property);
  }

}