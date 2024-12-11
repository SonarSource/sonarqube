/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.ws;

import com.google.common.collect.Lists;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.updatecenter.common.Product;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

public class ActiveVersionEvaluator {

  private static final Comparator<Version> COMPARATOR = Comparator.comparingInt((Version v) -> Integer.parseInt(v.getMajor()))
    .thenComparingInt((Version v) -> Integer.parseInt(v.getMinor()));
  private final SonarQubeVersion sonarQubeVersion;
  private final System2 system2;

  public ActiveVersionEvaluator(SonarQubeVersion sonarQubeVersion, System2 system2) {
    this.sonarQubeVersion = sonarQubeVersion;
    this.system2 = system2;
  }

  public boolean evaluateIfActiveVersion(UpdateCenter updateCenter) {
    Version installedVersion = Version.create(sonarQubeVersion.get().toString());
    if (updateCenter.getInstalledSonarProduct() == Product.SONARQUBE_COMMUNITY_BUILD) {
      return true;
    }

    if (compareWithoutPatchVersion(installedVersion, updateCenter.getSonar().getLtaVersion().getVersion()) == 0) {
      return true;
    }
    SortedSet<Release> allReleases = updateCenter.getSonar().getAllReleases(updateCenter.getInstalledSonarProduct());
    if (compareWithoutPatchVersion(installedVersion, updateCenter.getSonar().getPastLtaVersion().getVersion()) == 0) {
      Release initialLtaRelease = findInitialVersionOfMajorRelease(allReleases, updateCenter.getSonar().getLtaVersion().getVersion());
      Date initialLtaReleaseDate = initialLtaRelease.getDate();
      if (initialLtaReleaseDate == null) {
        throw new IllegalStateException("Initial Major release date is missing in releases");
      }
      // date of the latest major release should be within 6 months
      Calendar c = Calendar.getInstance();
      c.setTime(new Date(system2.now()));
      c.add(Calendar.MONTH, -6);

      return initialLtaReleaseDate.after(c.getTime());
    } else {
      return compareWithoutPatchVersion(installedVersion, findPreviousReleaseIgnoringPatch(allReleases).getVersion()) >= 0;
    }
  }


  private static int compareWithoutPatchVersion(Version v1, Version v2) {
    return COMPARATOR.compare(v1, v2);
  }

  private static Release findInitialVersionOfMajorRelease(SortedSet<Release> releases, Version referenceVersion) {
    return releases.stream()
      .filter(release -> release.getVersion().getMajor().equals(referenceVersion.getMajor())
        && release.getVersion().getMinor().equals(referenceVersion.getMinor()))
      .min(Comparator.comparing(r -> Integer.parseInt(r.getVersion().getPatch())))
      .orElseThrow(() -> new IllegalStateException("Unable to find initial major release for version " + referenceVersion + " in releases"
      ));
  }

  private static Release findPreviousReleaseIgnoringPatch(SortedSet<Release> releases) {
    if (!releases.isEmpty()) {
      Release refRelease = releases.last();
      int patchesOfRefRelease = 0;
      List<Release> sublist = Lists.reverse(releases.stream().toList());
      for (Release release : sublist) {
        int versionComparison = compareWithoutPatchVersion(release.getVersion(), refRelease.getVersion());
        if (versionComparison < 0) {
          return release;
        } else if (versionComparison == 0) {
          patchesOfRefRelease++;
        }
      }
      // if all releases have the same version, return the last one
      if (patchesOfRefRelease == releases.size()) {
        return refRelease;
      }
    }
    throw new IllegalStateException("Unable to find previous release in releases");
  }
}
