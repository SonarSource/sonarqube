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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.sonar.api.CoreProperties;

import java.text.ParseException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ServerMetadataTest {

  @Test
  public void testLoadProperties() throws ParseException {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.SERVER_ID, "123");
    conf.setProperty(CoreProperties.SERVER_VERSION, "2.2");
    conf.setProperty(CoreProperties.SERVER_STARTTIME, "2010-05-18T17:59:00+0000");
    conf.setProperty("sonar.host.url", "http://foo.com");

    ServerMetadata server = new ServerMetadata(conf);

    assertThat(server.getId(), is("123"));
    assertThat(server.getVersion(), is("2.2"));
    assertThat(server.getStartedAt().getDate(), is(18));
    assertThat(server.getUrl(), is("http://foo.com"));
  }

  /**
   * http://jira.codehaus.org/browse/SONAR-1685
   * The maven plugin fails if the property sonar.host.url ends with a slash
   */
  @Test
  public void urlMustNotEndWithSlash() throws ParseException {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty("sonar.host.url", "http://localhost:80/");

    ServerMetadata server = new ServerMetadata(conf);
    assertThat(server.getUrl(), is("http://localhost:80"));
  }
}
