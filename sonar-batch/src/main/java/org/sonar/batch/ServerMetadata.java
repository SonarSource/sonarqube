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
package org.sonar.batch;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.platform.Server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerMetadata extends Server {

  private String id;
  private String version;
  private String url;
  private Date startTime;

  public ServerMetadata(Configuration conf) {
    id = conf.getString(CoreProperties.SERVER_ID);
    version = conf.getString(CoreProperties.SERVER_VERSION);
    url = getUrl(conf);
    String dateString = conf.getString(CoreProperties.SERVER_STARTTIME);
    if (dateString!=null) {
      try {
        this.startTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(dateString);

      } catch (ParseException e) {
        LoggerFactory.getLogger(getClass()).error("The property " + CoreProperties.SERVER_STARTTIME + " is badly formatted.", e);
      }
    }
  }

  public static String getUrl(Configuration conf) {
    return StringUtils.removeEnd(conf.getString("sonar.host.url", "http://localhost:9000"), "/");
  }

  public String getId() {
    return id;
  }

  public String getVersion() {
    return version;
  }

  public Date getStartedAt() {
    return startTime;
  }

  public String getUrl() {
    return url;
  }
}
