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
package org.sonar.api.checks.profiles;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.check.Priority;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3
 */
@Deprecated
public class Check {

  private String repositoryKey;
  private String templateKey;
  private Priority priority = null;
  private final Map<String, String> properties = new HashMap<String,String>();

  public Check() {
  }

  public Check(String repositoryKey, String templateKey) {
    this.repositoryKey = repositoryKey;
    this.templateKey = templateKey;
  }

  public String getTemplateKey() {
    return templateKey;
  }

  public String getRepositoryKey() {
    return repositoryKey;
  }

  public void setRepositoryKey(String repositoryKey) {
    this.repositoryKey = repositoryKey;
  }

  public void setTemplateKey(String templateKey) {
    this.templateKey = templateKey;
  }

  public Priority getPriority() {
    return priority;
  }

  public void setPriority(Priority priority) {
    this.priority = priority;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public String getProperty(String key) {
    return properties.get(key);
  }

  public void addProperty(String key, Object value) {
    properties.put(key, value.toString());
  }

  public void addProperties(Map<String, String> properties) {
    this.properties.putAll(properties);
  }

  public void setProperties(Map<String, String> properties) {
    this.properties.clear();
    this.properties.putAll(properties);
  }

  @Override
  public boolean equals(Object o) {
    return o == this;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("repository", repositoryKey)
        .append("template", templateKey)
        .append("priority", priority)
        .toString();
  }
}
