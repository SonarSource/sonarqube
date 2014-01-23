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
package org.sonar.api.i18n;

import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;

import javax.annotation.Nullable;

import java.util.Date;
import java.util.Locale;

/**
 * Main component that provides translation facilities.
 * 
 * @since 2.10
 */
public interface I18n extends ServerComponent, BatchComponent {

  /**
   * Searches the message of the <code>key</code> for the <code>locale</code> in the list of available bundles.
   * <br>
   * If not found in any bundle, <code>defaultValue</code> is returned.
   * <p/>
   * If additional parameters are given (in the objects list), the result is used as a message pattern
   * to use in a MessageFormat object along with the given parameters.
   *
   * @param locale the locale to translate into
   * @param key the key of the pattern to translate
   * @param defaultValue the default pattern returned when the key is not found in any bundle
   * @param parameters the parameters used to format the message from the translated pattern.
   * @return the message formatted with the translated pattern and the given parameters
   */
  String message(final Locale locale, final String key, @Nullable final String defaultValue, final Object... parameters);

  /**
   * TODO add documentation
   * @since 4.2
   */
  String instant(Locale locale, long durationInMillis);

  /**
   * TODO add documentation
   * @since 4.2
   */
  String instant(Locale locale, Date date);

  /**
   * TODO add documentation
   * @since 4.2
   */
  String ago(Locale locale, long durationInMillis);

  /**
   * TODO add documentation
   * @since 4.2
   */
  String ago(Locale locale, Date date);

  /**
   * TODO add documentation
   * @since 4.2
   */
  String formatDateTime(Locale locale, Date date);

  /**
   * @since 4.2
   * TODO add documentation
   */
  String formatDate(Locale locale, Date date);

}
