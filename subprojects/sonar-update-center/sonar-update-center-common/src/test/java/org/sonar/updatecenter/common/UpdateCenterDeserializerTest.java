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

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

public class UpdateCenterDeserializerTest {

  @Test
  public void fromProperties() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = UpdateCenterDeserializer.fromProperties(props);

      assertThat(center.getSonar().getVersions(), hasItems(Version.create("2.2"), Version.create("2.3")));
      assertThat(center.getSonar().getRelease(Version.create("2.2")).getDownloadUrl(), is("http://dist.sonar.codehaus.org/sonar-2.2.zip"));

      Plugin clirr = center.getPlugin("clirr");
      assertThat(clirr.getName(), is("Clirr"));
      assertThat(clirr.getDescription(), is("Clirr Plugin"));
      assertThat(clirr.getVersions(),hasItems(Version.create("1.0"), Version.create("1.1")));
      assertThat(clirr.getRelease(Version.create("1.0")).getDownloadUrl(), is("http://dist.sonar-plugins.codehaus.org/clirr-1.0.jar"));

    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
