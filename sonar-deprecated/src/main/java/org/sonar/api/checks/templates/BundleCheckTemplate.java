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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3
 */
@Deprecated
public class BundleCheckTemplate extends CheckTemplate {
  private static final Logger LOG = LoggerFactory.getLogger(BundleCheckTemplate.class);

  private String bundleBaseName;
  private String defaultTitle;
  private String defaultDescription;

  protected BundleCheckTemplate(String key, String bundleBaseName) {
    super(key);
    this.bundleBaseName = bundleBaseName;
  }

  protected BundleCheckTemplate(String key, Class bundleClass) {
    this(key, bundleClass.getCanonicalName());
  }

  protected String getDefaultTitle() {
    if (defaultTitle == null || "".equals(defaultTitle)) {
      return getKey();
    }
    return defaultTitle;
  }

  protected void setDefaultTitle(String defaultTitle) {
    this.defaultTitle = defaultTitle;
  }

  protected String getDefaultDescription() {
    return defaultDescription;
  }

  protected void setDefaultDescription(String defaultDescription) {
    this.defaultDescription = defaultDescription;
  }

  @Override
  public String getTitle(Locale locale) {
    return getText("title", locale, getDefaultTitle());
  }

  @Override
  public String getDescription(Locale locale) {
    return getText("description", locale, getDefaultDescription());
  }

  @Override
  public String getMessage(Locale locale, String key, Object... params) {
    return null;
  }

  protected String getText(String key, Locale locale, String defaultValue) {
    String result = null;
    ResourceBundle bundle = getBundle(locale);
    if (bundle != null) {
      try {
        result = bundle.getString(key);
      } catch (MissingResourceException e) {
        LOG.debug(e.getMessage());
      }
    }
    if (result == null) {
      result = defaultValue;
    }
    return result;
  }

  protected ResourceBundle getBundle(Locale locale) {
    try {
      if (locale != null) {
        return ResourceBundle.getBundle(bundleBaseName, locale);
      }
    } catch (MissingResourceException e) {
      // do nothing : use the default values
    }
    return null;
  }
}
