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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Evgeny Mandrikov
 */
public class PluginTest {
  @Test
  public void testToJsonObject() {
    /*
    Plugin plugin = new Plugin("org.sonar.plugins.test.Test");
    plugin.setVersion("0.1");
    plugin.setName("Sonar Test Plugin");
    plugin.setRequiredSonarVersion("2.0");
    assertEquals(
        "{\"sonarVersion\":\"2.0\"" +
            ",\"id\":\"org.sonar.plugins.test.Test\"" +
            ",\"name\":\"Sonar Test Plugin\"" +
            ",\"version\":\"0.1\"" +
            "}",
        plugin.toJsonObject().toJSONString()
    );

    plugin.setDownloadUrl("http://download");
    plugin.setHomepage("http://homepage");
    assertEquals(
        "{\"sonarVersion\":\"2.0\"" +
            ",\"id\":\"org.sonar.plugins.test.Test\"" +
            ",\"name\":\"Sonar Test Plugin\"" +
            ",\"downloadUrl\":\"http:\\/\\/download\"" +
            ",\"homepage\":\"http:\\/\\/homepage\"" +
            ",\"version\":\"0.1\"" +
            "}",
        plugin.toJsonObject().toJSONString()
    );
    */
  }
}
