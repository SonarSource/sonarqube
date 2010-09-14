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
package org.sonar.api.checks.templates;

import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3
 */
@Deprecated
public abstract class CheckTemplate {

  protected String key;
  protected String configKey;
  protected Priority priority;
  protected IsoCategory isoCategory;
  protected List<CheckTemplateProperty> properties;

  public CheckTemplate(String key) {
    this.key = key;
  }

  public CheckTemplate() {
  }

  public String getKey() {
    return key;
  }

  public void setKey(String s) {
    this.key = s;
  }

  public Priority getPriority() {
    return priority;
  }

  public void setPriority(Priority p) {
    this.priority = p;
  }

  public IsoCategory getIsoCategory() {
    return isoCategory;
  }

  public void setIsoCategory(IsoCategory c) {
    this.isoCategory = c;
  }

  public String getConfigKey() {
    return configKey;
  }

  public void setConfigKey(String configKey) {
    this.configKey = configKey;
  }

  public abstract String getTitle(Locale locale);

  public abstract String getDescription(Locale locale);

  public abstract String getMessage(Locale locale, String key, Object... params);

  public List<CheckTemplateProperty> getProperties() {
    if (properties==null) {
      return Collections.emptyList();
    }
    return properties;
  }

  public void addProperty(CheckTemplateProperty p) {
    if (properties==null) {
      properties = new ArrayList<CheckTemplateProperty>();
    }
    properties.add(p);
  }

  public CheckTemplateProperty getProperty(String key) {
    if (properties!=null) {
      for (CheckTemplateProperty property : properties) {
        if (property.getKey().equals(key)) {
          return property;
        }
      }
    }
    return null;
  }

  /**
   * Checks are equal within the same plugin. Two plugins can have two different checks with the same key.
   */
  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CheckTemplate)) {
      return false;
    }

    CheckTemplate checkTemplate = (CheckTemplate) o;
    return key.equals(checkTemplate.key);
  }

  @Override
  public final int hashCode() {
    return key.hashCode();
  }

}