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

import java.util.Locale;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3
 */
@Deprecated
public abstract class CheckTemplateProperty implements Comparable<CheckTemplateProperty> {

  protected String key;
  
  public String getKey() {
    return key;
  }

  public void setKey(String s) {
    this.key = s;
  }

  public abstract String getTitle(Locale locale);

  public String getDescription() {
    return getDescription(Locale.ENGLISH);
  }


  public abstract String getDescription(Locale locale);

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CheckTemplateProperty)) {
      return false;
    }

    CheckTemplateProperty that = (CheckTemplateProperty) o;
    return key.equals(that.key);
  }

  @Override
  public final int hashCode() {
    return key.hashCode();
  }

  public int compareTo(CheckTemplateProperty o) {
    return getKey().compareTo(o.getKey());
  }
}
