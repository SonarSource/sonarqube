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
public class BundleCheckTemplateProperty extends CheckTemplateProperty {

  private BundleCheckTemplate check;
  private String defaultTitle;
  private String defaultDescription;

  public BundleCheckTemplateProperty(BundleCheckTemplate check, String key) {
    setKey(key);
    this.check = check;
  }

  public String getDefaultTitle() {
      if (defaultTitle == null || "".equals(defaultTitle)) {
        return getKey();
      }
      return defaultTitle;
    }

    public void setDefaultTitle(String s) {
      this.defaultTitle = s;
    }


  @Override
  public String getTitle(Locale locale) {
    return check.getText("property." + getKey() + ".title", locale, getDefaultTitle());
  }

  public String getDefaultDescription() {
    return defaultDescription;
  }

  public void setDefaultDescription(String s) {
    this.defaultDescription = s;
  }

  @Override
  public String getDescription(Locale locale) {
    return check.getText("property." + getKey() + ".description", locale, getDefaultDescription());
  }
}
