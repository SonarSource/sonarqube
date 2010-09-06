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
package org.sonar.core.plugin;

import org.junit.Before;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JpaPluginDaoTest extends AbstractDbUnitTestCase {

  private JpaPluginDao dao;

  @Before
  public void before() {
    dao = new JpaPluginDao(getSessionFactory());
  }

  @Test
  public void getPlugins() {
    setupData("shared");

    List<JpaPlugin> plugins = dao.getPlugins();

    assertEquals(1, plugins.size());
    assertEquals("checkstyle", plugins.get(0).getKey());
    assertEquals(2, plugins.get(0).getFiles().size());
  }


  @Test
  public void savePluginAndFiles() {
    setupData("shared");
    JpaPlugin pmd = JpaPlugin.create("pmd");
    pmd.setCore(false);
    pmd.setName("PMD");
    pmd.setVersion("2.2");
    pmd.setPluginClass("org.sonar.pmd.Main");

    pmd.createFile("sonar-pmd-plugin-2.2.jar");
    pmd.createFile("pmd-extension.jar");
    pmd.createFile("pmd-extension2.jar");

    getSession().saveWithoutFlush(pmd);
    checkTables("savePluginAndFiles", "plugins", "plugin_files");
  }

  @Test
  public void saveDeprecatedPlugin() {
    setupData("shared");
    JpaPlugin pmd = JpaPlugin.create("pmd");
    pmd.setCore(false);
    pmd.setName("PMD");
    pmd.setPluginClass("org.sonar.pmd.Main");

    pmd.createFile("sonar-pmd-plugin-2.2.jar");

    getSession().saveWithoutFlush(pmd);
    checkTables("saveDeprecatedPlugin", "plugins", "plugin_files");
  }

  @Test
  public void removePreviousFilesWhenRegisteringPlugin() {
    setupData("shared");

    List<JpaPlugin> plugins = dao.getPlugins();
    plugins.get(0).removeFiles();
    plugins.get(0).createFile("newfile.jar");

    dao.register(plugins);

    checkTables("removePreviousFilesWhenRegisteringPlugin", "plugins", "plugin_files");
  }

  @Test
  public void registerManyPlugins() {
    setupData("shared");

    List<JpaPlugin> plugins = createManyPlugins();
    dao.register(plugins);

    assertThat(dao.getPlugins().size(), is(150));
    assertThat(dao.getPluginFiles().size(), is(150 * 20)); // initial plugin "checkstyle" has been deleted
  }

  private List<JpaPlugin> createManyPlugins() {
    List<JpaPlugin> plugins = new ArrayList<JpaPlugin>();
    for (int i=0 ; i<150 ; i++) {
      JpaPlugin plugin = JpaPlugin.create("plugin-" + i);
      for (int j=0 ; j<20 ; j++) {
        plugin.createFile("file-" + i + "-" + j + ".jar");
      }
      plugins.add(plugin);
    }
    return plugins;
  }
}
