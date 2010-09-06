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
package org.sonar.updatecenter.common;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UpdateCenterSerializerTest {

  @Test
  public void testToProperties() throws IOException, URISyntaxException {
    UpdateCenter center = new UpdateCenter();

    center.getSonar().addRelease(Version.create("2.0"));
    center.getSonar().addRelease(Version.create("2.1"));
    center.addPlugin(new Plugin("foo").setName("Foo").setOrganizationUrl("http://www.sonarsource.org"));
    Plugin barPlugin = new Plugin("bar");
    center.addPlugin(barPlugin);
    barPlugin.addRelease(
        new Release(barPlugin, Version.create("1.2"))
            .addRequiredSonarVersions(Version.create("2.0"))
            .addRequiredSonarVersions(Version.create("2.1")));

    Properties properties = UpdateCenterSerializer.toProperties(center);
    properties.store(System.out, null);

    assertProperty(properties, "sonar.versions", "2.0,2.1");
    assertProperty(properties, "plugins", "foo,bar");
    assertProperty(properties, "foo.name", "Foo");
    assertProperty(properties, "foo.organizationUrl", "http://www.sonarsource.org");
    assertProperty(properties, "bar.versions", "1.2");
    assertProperty(properties, "bar.1.2.requiredSonarVersions", "2.0,2.1");
  }

  private void assertProperty(Properties props, String key, String value) {
    assertThat(props.getProperty(key), is(value));
  }
}
