/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class LocalizedMessages extends ResourceBundle {

  private static final Logger LOG = Loggers.get(LocalizedMessages.class);

  private Locale locale;
  private List<ResourceBundle> bundles;

  /**
   * Constructs a resource bundle from a list of other resource bundles. If
   * there are duplicate keys, the key from the resource bundle with the
   * smallest index takes precedence.
   */
  public LocalizedMessages(Locale locale, String... basenames) {
    this.locale = locale;
    bundles = new ArrayList<>(basenames.length);
    for (String basename : basenames) {
      bundles.add(getBundle("sonar.bundles." + basename, locale));
    }
  }

  @Override
  public Locale getLocale() {
    return locale;
  }

  public String format(String key, Object... args) {
    return format(true, key, args);
  }

  public String formatQuietly(String key, Object... args) {
    return format(false, key, args);
  }

  private String format(boolean logIfMissing, String key, Object... args) {
    try {
      String message = getString(key);
      return String.format(locale, message, args);

    } catch (MissingResourceException e) {
      if (logIfMissing) {
        LOG.warn("Missing translation: key==" + key + ",locale=" + locale);
      }
      return key;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.ResourceBundle#getKeys()
   */
  @Override
  public Enumeration<String> getKeys() {
    return new KeyEnumeration();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.ResourceBundle#handleGetObject(java.lang.String)
   */
  @Override
  protected Object handleGetObject(String key) {
    for (ResourceBundle b : bundles) {
      try {
        return b.getObject(key);
      } catch (MissingResourceException mre) {
        // iterate
      }
    }
    throw new MissingResourceException(null, null, key);
  }

  private class KeyEnumeration implements Enumeration<String> {
    private Set<String> keys = new HashSet<>();

    // Set iterator to simulate enumeration
    private Iterator<String> i;

    // Constructor
    {
      for (ResourceBundle b : bundles) {
        Enumeration<String> bundleKeys = b.getKeys();
        while (bundleKeys.hasMoreElements()) {
          keys.add(bundleKeys.nextElement());
        }
      }
      i = keys.iterator();
    }

    @Override
    public boolean hasMoreElements() {
      return i.hasNext();
    }

    @Override
    public String nextElement() {
      return i.next();
    }
  }
}
