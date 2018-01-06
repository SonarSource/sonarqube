/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.plugin;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private PluginDao underTest = db.getDbClient().pluginDao();

  @Test
  public void selectByKey() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectByKey(db.getSession(), "java2")).isEmpty();

    Optional<PluginDto> plugin = underTest.selectByKey(db.getSession(), "java");
    assertThat(plugin.isPresent()).isTrue();
    assertThat(plugin.get().getUuid()).isEqualTo("a");
    assertThat(plugin.get().getKee()).isEqualTo("java");
    assertThat(plugin.get().getBasePluginKey()).isNull();
    assertThat(plugin.get().getFileHash()).isEqualTo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    assertThat(plugin.get().getCreatedAt()).isEqualTo(1500000000000L);
    assertThat(plugin.get().getUpdatedAt()).isEqualTo(1600000000000L);
  }

  @Test
  public void selectAll() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectAll(db.getSession())).hasSize(2);
  }

  @Test
  public void insert() {
    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.insert(db.getSession(), new PluginDto()
      .setUuid("c")
      .setKee("javascript")
      .setBasePluginKey("java")
      .setFileHash("cccccccccccccccccccccccccccccccc")
      .setCreatedAt(1L)
      .setUpdatedAt(2L));

    Optional<PluginDto> plugin = underTest.selectByKey(db.getSession(), "javascript");
    assertThat(plugin.isPresent()).isTrue();
    assertThat(plugin.get().getUuid()).isEqualTo("c");
    assertThat(plugin.get().getKee()).isEqualTo("javascript");
    assertThat(plugin.get().getBasePluginKey()).isEqualTo("java");
    assertThat(plugin.get().getFileHash()).isEqualTo("cccccccccccccccccccccccccccccccc");
    assertThat(plugin.get().getCreatedAt()).isEqualTo(1L);
    assertThat(plugin.get().getUpdatedAt()).isEqualTo(2L);
  }

  @Test
  public void update() {
    db.prepareDbUnit(getClass(), "shared.xml");

    PluginDto plugin = underTest.selectByKey(db.getSession(), "java").get();

    plugin.setBasePluginKey("foo");
    plugin.setFileHash("abc");
    plugin.setUpdatedAt(3L);

    underTest.update(db.getSession(), plugin);

    plugin = underTest.selectByKey(db.getSession(), "java").get();
    assertThat(plugin.getUuid()).isEqualTo("a");
    assertThat(plugin.getKee()).isEqualTo("java");
    assertThat(plugin.getBasePluginKey()).isEqualTo("foo");
    assertThat(plugin.getFileHash()).isEqualTo("abc");
    assertThat(plugin.getCreatedAt()).isEqualTo(1500000000000L);
    assertThat(plugin.getUpdatedAt()).isEqualTo(3L);
  }
}
