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
package org.sonar.plugins.googleanalytics;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.CoreProperties;
import org.sonar.api.web.Footer;

public class GoogleAnalyticsWebFooter implements Footer {

  private Configuration configuration;

  public GoogleAnalyticsWebFooter(Configuration configuration) {
    this.configuration = configuration;
  }

  protected String getIdAccount() {
    return configuration.getString(CoreProperties.GOOGLE_ANALYTICS_ACCOUNT_PROPERTY, "");
  }

  public String getKey() {
    return "webfooter_" + CoreProperties.GOOGLE_ANALYTICS_PLUGIN;
  }

  public String getHtml() {
    String id = getIdAccount();
    if (id != null && !"".equals(id)) {
      return "<script type=\"text/javascript\">\n" +
          "var gaJsHost = ((\"https:\" == document.location.protocol) ? \"https://ssl.\" : \"http://www.\");\n" +
          "document.write(unescape(\"%3Cscript src='\" + gaJsHost + \"google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E\"));\n" +
          "</script>\n" +
          "<script type=\"text/javascript\">\n" +
          "var pageTracker = _gat._getTracker(\"" + id + "\");\n" +
          "pageTracker._initData();\n" +
          "pageTracker._trackPageview();\n" +
          "</script>";
    }
    return null;
  }

  @Override
  public String toString() {
    return getKey();
  }

}
