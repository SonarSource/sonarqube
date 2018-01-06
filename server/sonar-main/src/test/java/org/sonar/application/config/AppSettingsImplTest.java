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
package org.sonar.application.config;

import java.util.Properties;
import org.junit.Test;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;

public class AppSettingsImplTest {

  @Test
  public void reload_updates_properties() {
    Props initialProps = new Props(new Properties());
    initialProps.set("foo", "bar");
    Props newProps = new Props(new Properties());
    newProps.set("foo", "baz");
    newProps.set("newProp", "newVal");

    AppSettingsImpl underTest = new AppSettingsImpl(initialProps);
    underTest.reload(newProps);

    assertThat(underTest.getValue("foo").get()).isEqualTo("baz");
    assertThat(underTest.getValue("newProp").get()).isEqualTo("newVal");
    assertThat(underTest.getProps().rawProperties()).hasSize(2);
  }
}
