/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.server.plugins;

import org.junit.Test;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.Version;

import static org.fest.assertions.Assertions.assertThat;

public class SonarUpdateTest {

  @Test
  public void incompatibleUpdateIfSomePluginsAreIncompatible() {
    SonarUpdate update = new SonarUpdate(new Release(new Sonar(), "2.3"));
    update.addIncompatiblePlugin(new Plugin("old"));

    assertThat(update.isIncompatible()).isTrue();
    assertThat(update.hasWarnings()).isTrue();
    assertThat(update.requiresPluginUpgrades()).isFalse();
  }

  @Test
  public void incompatibleUpdateIfRequiredPluginUpgrades() {
    SonarUpdate update = new SonarUpdate(new Release(new Sonar(), "2.3"));
    update.addPluginToUpgrade(new Release(new Plugin("old"), Version.create("0.2")));

    assertThat(update.isIncompatible()).isFalse();
    assertThat(update.hasWarnings()).isTrue();
    assertThat(update.requiresPluginUpgrades()).isTrue();
  }

  @Test
  public void equals() {
    SonarUpdate update1 = new SonarUpdate(new Release(new Sonar(), "2.2"));
    SonarUpdate update2 = new SonarUpdate(new Release(new Sonar(), "2.3"));

    assertThat(update1).isEqualTo(update1);
    assertThat(update1).isEqualTo(new SonarUpdate(new Release(new Sonar(), "2.2")));
    assertThat(update1).isNotEqualTo(update2);
  }
}
