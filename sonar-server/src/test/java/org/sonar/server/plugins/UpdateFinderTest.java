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

import org.junit.Before;
import org.junit.Test;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class UpdateFinderTest {
  private UpdateCenter center;

  private Plugin foo;
  private Release foo10;
  private Release foo11;
  private Release foo12;

  private Plugin bar;
  private Release bar10;
  private Release bar11;

  @Before
  public void initCenter() {
    center = new UpdateCenter();
    foo = new Plugin("foo");
    foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1", "2.2").setDownloadUrl("http://server/foo-1.0.jar");
    foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1", "2.2", "2.3").setDownloadUrl("http://server/foo-1.1.jar");
    foo12 = new Release(foo, "1.2").addRequiredSonarVersions("2.3").setDownloadUrl("http://server/foo-1.2.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);
    foo.addRelease(foo12);
    center.addPlugin(foo);

    bar = new Plugin("bar");
    bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1", "2.2").setDownloadUrl("http://server/bar-1.0.jar");
    bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.2.2", "2.3").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar10);
    bar.addRelease(bar11);
    center.addPlugin(bar);
  }

  @Test
  public void findPluginUpdates() {
    UpdateFinder finder = new UpdateFinder(center, "2.1");
    finder.registerInstalledPlugin("foo", Version.create("1.0"));
    List<PluginUpdate> updates = finder.findPluginUpdates();
    assertThat(updates.size(), is(2));

    assertThat(updates.get(0).getRelease(), is(foo11));
    assertThat(updates.get(0).isCompatible(), is(true));

    assertThat(updates.get(1).getRelease(), is(foo12));
    assertThat(updates.get(1).isCompatible(), is(false));
    assertThat(updates.get(1).requiresSonarUpgrade(), is(true));
  }

  @Test
  public void noPluginUpdatesIfLastReleaseIsInstalled() {
    UpdateFinder finder = new UpdateFinder(center, "2.3");
    finder.registerInstalledPlugin("foo", Version.create("1.2"));
    assertTrue(finder.findPluginUpdates().isEmpty());
  }

  @Test
  public void availablePluginsAreOnlyTheBestReleases() {
    UpdateFinder finder = new UpdateFinder(center, "2.2");
    finder.registerInstalledPlugin("foo", Version.create("1.0"));

    List<PluginUpdate> availables = finder.findAvailablePlugins();

    // bar 1.0 is compatible with the installed sonar
    // bar 1.1 requires sonar to be upgraded to 2.2.2 or 2.3
    // => available plugin to install is bar 1.0
    assertThat(availables.size(), is(1));
    assertThat(availables.get(0).getRelease(), is(bar10));
    assertThat(availables.get(0).isCompatible(), is(true));
  }

  @Test
  public void availablePluginsRequireSonarUpgrade() {
    UpdateFinder finder = new UpdateFinder(center, "2.2.1");
    finder.registerInstalledPlugin("foo", Version.create("1.0"));

    List<PluginUpdate> availables = finder.findAvailablePlugins();

    // bar 1.0 is not compatible with the installed sonar
    // bar 1.1 requires sonar to be upgraded to 2.2.2 or 2.3
    // => available plugin to install is bar 1.1
    assertThat(availables.size(), is(1));
    assertThat(availables.get(0).getRelease(), is(bar11));
    assertThat(availables.get(0).requiresSonarUpgrade(), is(true));
  }

  @Test
  public void findSonarUpdates() {
    center.getSonar().addRelease(Version.create("2.3"));
    center.getSonar().addRelease(Version.create("2.4"));

    UpdateFinder finder = new UpdateFinder(center, "2.2");
    List<SonarUpdate> updates = finder.findSonarUpdates();

    // no plugins are installed, so both sonar versions are compatible
    assertThat(updates.size(), is(2));
    assertThat(updates.get(0).hasWarnings(), is(false));
    assertThat(updates.get(1).hasWarnings(), is(false));
  }

  @Test
  public void warningsOnSonarUpdates() {
    center.getSonar().addRelease(Version.create("2.3"));
    center.getSonar().addRelease(Version.create("2.4"));

    UpdateFinder finder = new UpdateFinder(center, "2.2");
    finder.registerInstalledPlugin("foo", Version.create("1.0"));
    finder.registerInstalledPlugin("bar", Version.create("1.0"));
    List<SonarUpdate> updates = finder.findSonarUpdates();

    assertThat(updates.size(), is(2));

    // sonar 2.3 supports foo 1.1/1.2 and bar 1.1
    // => 2 plugin upgrades are required
    assertThat(updates.get(0).hasWarnings(), is(true));
    assertThat(updates.get(0).requiresPluginUpgrades(), is(true));
    assertThat(updates.get(0).getPluginsToUpgrade().size(), is(2));

    // sonar 2.4 supports no plugins
    assertThat(updates.get(1).hasWarnings(), is(true));
    assertThat(updates.get(1).isIncompatible(), is(true));
    assertThat(updates.get(1).getIncompatiblePlugins().size(), is(2));
  }

  @Test
  public void excludePendingDownloadsFromPluginUpdates() {
    UpdateFinder finder = new UpdateFinder(center, "2.1");
    finder.registerInstalledPlugin("foo", Version.create("1.0"));
    finder.registerPendingPluginsByFilename("foo-1.0.jar");
    List<PluginUpdate> updates = finder.findPluginUpdates();
    assertThat(updates.size(), is(0));
  }

  @Test
  public void excludePendingDownloadsFromAvailablePlugins() {
    UpdateFinder finder = new UpdateFinder(center, "2.1");
    finder.registerPendingPluginsByFilename("foo-1.0.jar");
    finder.registerPendingPluginsByFilename("bar-1.1.jar");
    List<PluginUpdate> updates = finder.findAvailablePlugins();
    assertThat(updates.size(), is(0));
  }
}
