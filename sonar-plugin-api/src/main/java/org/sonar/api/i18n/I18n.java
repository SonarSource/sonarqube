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
package org.sonar.api.i18n;

import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;

import java.util.Locale;

/**
 * 
 * The <code>I18n</code> Interface is the entry point for the internationalization of the Sonar application and plugins.<br>The corresponding implementation is located in the core I18n plugin. 
 * <p/>
 * I18n is managed in Sonar through the use of key-based resource bundles.
 * <br>
 * Though any key can be used, the following key-naming conventions, which are applied in the Sonar application and core plugins, are given as guidelines:
 * <ul>
 * <li>
 * Title of <code>View</code> extensions (pages, tabs, widgets): 
 * <blockquote><code>view.<i>view_id</i>.title</code></blockquote>
 * where <i>view_id</i> is the value returned by the <code>View.getId()</code> method
 * </li>
 * <br>
 * <li>
 * Free text translation in <code>View</code> extensions (pages, tabs, widgets):
 * <blockquote><code>view.<i>view_id</i>.<i>key</i></code></blockquote>
 * where:
 *  <ul>
 *    <li>
 *      <code><i>view_id</i></code> is the value returned by the <code>View.getId()</code> method
 *    </li>
 *    <li>
 *      <code><i>key</i></code> is the key of the text in the code of this view (rails page resource for a widget)
 *    </li>  
 *  </ul>
 * </li>
 * <br>
 * <li>
 * Free text translation in the files of the Rails web application:
 * <blockquote><code>app.<i>type</i>.<i>path</i>.<i>key</i></code></blockquote>
 * where:
 *  <ul>
 *    <li>
 *      <code><i>type</i></code> is the type of rails application file ('controller', 'helper', 'model', 'view').
 *    </li>
 *    <li>
 *      <code><i>path</i></code> is the path of the rails page related to the rails component type subdirectory ('controllers', 'views', etc ...).
 *    </li>
 *    <li>
 *      <code><i>key</i></code> is the key of the text in the code of this file
 *    </li>  
 *  </ul><br>
 *  For example, searching for the <code>login</code> key in the rails file <code>views/layouts/_layout.html.erb</code> gives the following full i18n key:
 *  <blockquote><code>app.view.layouts.layout.login</code></blockquote>
 * </li>
 * <br>
 * <li>
 * Metric name or description:
 * <blockquote><code>metric.<i>metric_key</i>.<i>field</i></code></blockquote>
 * where:
 *  <ul>
 *    <li>
 *      <code><i>metric_key</i></code> is the key of the metric ('ncloc', etc ...),
 *    </li>
 *    <li>
 *      <code><i>field</i></code> is the field to translate on the metric ('name' or 'description').
 *    </li>
 *  </ul>
 * </li>
 * <br>
 * <li>
 * Metric domain:
 * <blockquote><code>domain.<i>domain_text</i></code></blockquote>
 * where:
 *  <ul>
 *    <li>
 *      <code><i>domain_text</i></code> is the default (english) text of the domain as defined in the Metric object.
 *    </li>
 *  </ul>
 * </li>
 * <br>
 * <li>
 * General columns which are not metrics (key, build date, etc ...):
 * <blockquote><code>general_columns.<i>column_key</i></code></blockquote>
 * where:
 *  <ul>
 *    <li>
 *      <code><i>column_key</i></code> is the key of the column ('key', 'date', 'links', etc ...).
 *    </li>
 *  </ul>
 * </li>
 * <br>
 * <li>
 * Rules:
 * <blockquote><code>rule.<i>repository_key</i>.<i>rule_key</i>.<i>field</i></code></blockquote>
 * where:
 *  <ul>
 *    <li>
 *      <code><i>repository_key</i></code> is the key of the rule repository,
 *    </li>
 *    <li>
 *      <code><i>rule_key</i></code> is the key of the rule in the repository,
 *    </li>
 *    <li>
 *      <code><i>field</i></code> is the field to translate on the rule ('name' or 'description').
 *    </li>
 *  </ul>
 * </li>
 * <br>
 * <li>
 * Rule severities:
 * <blockquote><code>severity.<i>severity_text</i></code></blockquote>
 * where:
 *  <ul>
 *    <li>
 *      <code><i>severity_text</i></code> is the upper_case text of the RulePriority enum value ('MAJOR', 'MINOR', etc ...).
 *    </li>
 *  </ul>
 * </li>
 * </ul>
 *
 * @since 2.9
 */
public interface I18n extends ServerComponent, BatchComponent {

  /**
   * Searches the message of the <code>key</code> for the <code>locale</code> in the list of available bundles.
   * <br>
   * If not found in any bundle, <code>defaultText</code> is returned.
   * 
   * If additional parameters are given (in the objects list), the result is used as a message pattern 
   * to use in a MessageFormat object along with the given parameters.  
   *
   * @param locale the locale to translate into
   * @param key the key of the pattern to translate
   * @param defaultValue the default pattern returned when the key is not found in any bundle
   * @param parameters the parameters used to format the message from the translated pattern.
   * @return the message formatted with the translated pattern and the given parameters 
   */
  public abstract String message(final Locale locale, final String key, final String defaultValue, final Object... parameters);
}
