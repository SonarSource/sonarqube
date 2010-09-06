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
package org.sonar.updatecenter.deprecated;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Evgeny Mandrikov
 */
public class HistoryTest {
  @Test
  public void testLatest() {
    History<Plugin> history = new History<Plugin>();

    history.addArtifact(new DefaultArtifactVersion("0.1"), newPlugin("0.1"));
    assertEquals(1, history.getAllVersions().size());
    assertEquals("0.1", history.latest().getVersion());

    history.addArtifact(new DefaultArtifactVersion("1.0"), newPlugin("1.0"));
    assertEquals(2, history.getAllVersions().size());
    assertEquals("1.0", history.latest().getVersion());

    history.addArtifact(new DefaultArtifactVersion("0.2"), newPlugin("0.2"));
    assertEquals(3, history.getAllVersions().size());
    assertEquals("1.0", history.latest().getVersion());
  }

  private Plugin newPlugin(String version) {
    Plugin plugin = new Plugin(version);
    plugin.setVersion(version);
    return plugin;
  }
}
