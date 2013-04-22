/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LocalizedMessages extends ResourceBundle {

  private static final Logger LOG = LoggerFactory.getLogger(LocalizedMessages.class);

  private Locale locale;
  private List<ResourceBundle> bundles;

  /**
   * Constructs a resource bundle from a list of other resource bundles. If
   * there are duplicate keys, the key from the resource bundle with the
   * smallest index takes precedence.
   */
  public LocalizedMessages(Locale locale, String... basenames) {
    this.locale = locale;
    bundles = new ArrayList<ResourceBundle>(basenames.length);
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
    return new Enumeration<String>() {
      private Set<String> keys = new HashSet<String>();

      // Set iterator to simulate enumeration
      private Iterator<String> i;

      // Constructor
      {
        for (ResourceBundle b : bundles) {
          keys.addAll(Lists.newArrayList(Iterators.forEnumeration(b.getKeys())));
        }
        i = keys.iterator();
      }

      public boolean hasMoreElements() {
        return i.hasNext();
      }

      public String nextElement() {
        return i.next();
      }
    };
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
}
