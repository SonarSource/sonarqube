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
package org.sonar.server.plugins;

import org.junit.Test;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SonarUpdateTest {

  @Test
  public void incompatibleUpdateIfSomePluginsAreIncompatible() {
    SonarUpdate update = new SonarUpdate(new Release(new Sonar(), "2.3"));
    update.addIncompatiblePlugin(new Plugin("old"));
    assertTrue(update.isIncompatible());
    assertTrue(update.hasWarnings());
    assertFalse(update.requiresPluginUpgrades());
  }

  @Test
  public void incompatibleUpdateIfRequiredPluginUpgrades() {
    SonarUpdate update = new SonarUpdate(new Release(new Sonar(), "2.3"));
    update.addPluginToUpgrade(new Plugin("old"));
    assertFalse(update.isIncompatible());
    assertTrue(update.hasWarnings());
    assertTrue(update.requiresPluginUpgrades());
  }

  @Test
  public void equals() {
    SonarUpdate update1 = new SonarUpdate(new Release(new Sonar(), "2.2"));
    SonarUpdate update2 = new SonarUpdate(new Release(new Sonar(), "2.3"));
    assertTrue(update1.equals(update1));
    assertTrue(update1.equals(new SonarUpdate(new Release(new Sonar(), "2.2"))));
    assertFalse(update1.equals(update2));
  }
}
