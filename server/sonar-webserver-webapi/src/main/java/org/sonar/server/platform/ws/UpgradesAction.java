/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.io.Resources;
import java.util.List;
import java.util.Optional;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.ui.VersionFormatter;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.SonarUpdate;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonar.server.plugins.edition.EditionBundledPlugins.isEditionBundled;

/**
 * Implementation of the {@code upgrades} action for the System WebService.
 */
public class UpgradesAction implements SystemWsAction {

  private static final boolean DO_NOT_FORCE_REFRESH = false;

  private static final String ARRAY_UPGRADES = "upgrades";
  private static final String PROPERTY_UPDATE_CENTER_LTS = "latestLTS";
  private static final String PROPERTY_UPDATE_CENTER_LTA = "latestLTA";
  private static final String PROPERTY_UPDATE_CENTER_REFRESH = "updateCenterRefresh";
  private static final String PROPERTY_VERSION = "version";
  private static final String PROPERTY_DESCRIPTION = "description";
  private static final String PROPERTY_RELEASE_DATE = "releaseDate";
  private static final String PROPERTY_CHANGE_LOG_URL = "changeLogUrl";
  private static final String PROPERTY_COMMUNITY_DOWNLOAD_URL = "downloadUrl";
  private static final String PROPERTY_DEVELOPER_DOWNLOAD_URL = "downloadDeveloperUrl";
  private static final String PROPERTY_ENTERPRISE_DOWNLOAD_URL = "downloadEnterpriseUrl";
  private static final String PROPERTY_DATACENTER_DOWNLOAD_URL = "downloadDatacenterUrl";
  private static final String PROPERTY_PRODUCT = "product";
  private static final String OBJECT_PLUGINS = "plugins";
  private static final String ARRAY_REQUIRE_UPDATE = "requireUpdate";
  private static final String ARRAY_INCOMPATIBLE = "incompatible";
  private static final String PROPERTY_KEY = "key";
  private static final String PROPERTY_NAME = "name";
  private static final String PROPERTY_LICENSE = "license";
  private static final String PROPERTY_CATEGORY = "category";
  private static final String PROPERTY_ORGANIZATION_NAME = "organizationName";
  private static final String PROPERTY_ORGANIZATION_URL = "organizationUrl";
  private static final String PROPERTY_HOMEPAGE_URL = "homepageUrl";
  private static final String PROPERTY_ISSUE_TRACKER_URL = "issueTrackerUrl";
  private static final String PROPERTY_EDITION_BUNDLED = "editionBundled";
  private static final String PROPERTY_TERMS_AND_CONDITIONS_URL = "termsAndConditionsUrl";
  public static final String INSTALLED_VERSION_ACTIVE = "installedVersionActive";

  private final UpdateCenterMatrixFactory updateCenterFactory;
  private final ActiveVersionEvaluator activeVersionEvaluator;

  public UpgradesAction(UpdateCenterMatrixFactory updateCenterFactory, ActiveVersionEvaluator activeVersionEvaluator) {
    this.updateCenterFactory = updateCenterFactory;
    this.activeVersionEvaluator = activeVersionEvaluator;
  }

  private static void writeMetadata(JsonWriter jsonWriter, Release release) {
    jsonWriter.prop(PROPERTY_VERSION, VersionFormatter.format(release.getVersion().getName()));
    jsonWriter.prop(PROPERTY_DESCRIPTION, release.getDescription());
    jsonWriter.propDate(PROPERTY_RELEASE_DATE, release.getDate());
    jsonWriter.prop(PROPERTY_CHANGE_LOG_URL, release.getChangelogUrl());
    jsonWriter.prop(PROPERTY_COMMUNITY_DOWNLOAD_URL, release.getDownloadUrl(Release.Edition.COMMUNITY));
    jsonWriter.prop(PROPERTY_DEVELOPER_DOWNLOAD_URL, release.getDownloadUrl(Release.Edition.DEVELOPER));
    jsonWriter.prop(PROPERTY_ENTERPRISE_DOWNLOAD_URL, release.getDownloadUrl(Release.Edition.ENTERPRISE));
    jsonWriter.prop(PROPERTY_DATACENTER_DOWNLOAD_URL, release.getDownloadUrl(Release.Edition.DATACENTER));
    jsonWriter.prop(PROPERTY_PRODUCT, release.getProduct().name());
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("upgrades")
      .setDescription("Lists available upgrades for the SonarQube instance (if any) and for each one, " +
        "lists incompatible plugins and plugins requiring upgrade." +
        "<br/>" +
        "Plugin information is retrieved from Update Center. Date and time at which Update Center was last refreshed " +
        "is provided in the response.")
      .setSince("5.2")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-upgrades_plugins.json"))
      .setChangelog(new Change("10.5", "The field 'ltsVersion' is deprecated from the response"))
      .setChangelog(new Change("10.5", "The field 'ltaVersion' is added to indicate the Long-Term Active Version"))
      .setChangelog(new Change("10.5", "The field 'installedVersionActive' is added to indicate if the installed version is an active version"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (JsonWriter jsonWriter = response.newJsonWriter()) {
      jsonWriter.setSerializeEmptys(false);
      writeResponse(jsonWriter);
    }
  }

  private void writeResponse(JsonWriter jsonWriter) {
    jsonWriter.beginObject();
    try {
      Optional<UpdateCenter> updateCenterOpt = updateCenterFactory.getUpdateCenter(DO_NOT_FORCE_REFRESH);
      writeUpgrades(jsonWriter, updateCenterOpt);
      if (updateCenterOpt.isPresent()) {
        UpdateCenter updateCenter = updateCenterOpt.get();
        writeLatestLtsVersion(jsonWriter, updateCenter);
        writeLatestLtaVersion(jsonWriter, updateCenter);
        jsonWriter.propDateTime(PROPERTY_UPDATE_CENTER_REFRESH, updateCenter.getDate());
        jsonWriter.prop(INSTALLED_VERSION_ACTIVE, activeVersionEvaluator.evaluateIfActiveVersion(updateCenter));
      }
    } finally {
      jsonWriter.endObject();
    }
  }

  private static void writeLatestLtsVersion(JsonWriter jsonWriter, UpdateCenter updateCenter) {
    Release ltsRelease = updateCenter.getSonar().getLtsRelease();
    if (ltsRelease != null) {
      Version ltsVersion = ltsRelease.getVersion();
      String latestLTS = String.format("%s.%s", ltsVersion.getMajor(), ltsVersion.getMinor());
      jsonWriter.prop(PROPERTY_UPDATE_CENTER_LTS, latestLTS);
    }
  }

  private static void writeLatestLtaVersion(JsonWriter jsonWriter, UpdateCenter updateCenter) {
    Release ltaRelease = updateCenter.getSonar().getLtaVersion();
    if (ltaRelease != null) {
      Version ltaVersion = ltaRelease.getVersion();
      String latestLTA = String.format("%s.%s", ltaVersion.getMajor(), ltaVersion.getMinor());
      jsonWriter.prop(PROPERTY_UPDATE_CENTER_LTA, latestLTA);
    }
  }

  private static void writeUpgrades(JsonWriter jsonWriter, Optional<UpdateCenter> updateCenter) {
    jsonWriter.name(ARRAY_UPGRADES).beginArray();

    if (updateCenter.isPresent()) {
      for (SonarUpdate sonarUpdate : updateCenter.get().findSonarUpdates()) {
        writeUpgrade(jsonWriter, sonarUpdate);
      }
    }

    jsonWriter.endArray();
  }

  private static void writeUpgrade(JsonWriter jsonWriter, SonarUpdate sonarUpdate) {
    jsonWriter.beginObject();

    writeMetadata(jsonWriter, sonarUpdate.getRelease());

    writePlugins(jsonWriter, sonarUpdate);

    jsonWriter.endObject();
  }

  private static void writePlugins(JsonWriter jsonWriter, SonarUpdate sonarUpdate) {
    jsonWriter.name(OBJECT_PLUGINS).beginObject();

    writePluginsToUpdate(jsonWriter, sonarUpdate.getPluginsToUpgrade());

    writeIncompatiblePlugins(jsonWriter, sonarUpdate.getIncompatiblePlugins());

    jsonWriter.endObject();
  }

  private static void writePluginsToUpdate(JsonWriter jsonWriter, List<Release> pluginsToUpgrade) {
    jsonWriter.name(ARRAY_REQUIRE_UPDATE).beginArray();
    for (Release release : pluginsToUpgrade) {
      jsonWriter.beginObject();

      writePlugin(jsonWriter, (Plugin) release.getArtifact());
      String version = isNotBlank(release.getDisplayVersion()) ? release.getDisplayVersion() : release.getVersion().toString();
      jsonWriter.prop(PROPERTY_VERSION, version);

      jsonWriter.endObject();
    }

    jsonWriter.endArray();
  }

  private static void writeIncompatiblePlugins(JsonWriter jsonWriter, List<Plugin> incompatiblePlugins) {
    jsonWriter.name(ARRAY_INCOMPATIBLE).beginArray();

    for (Plugin incompatiblePlugin : incompatiblePlugins) {
      jsonWriter.beginObject();
      writePlugin(jsonWriter, incompatiblePlugin);
      jsonWriter.endObject();
    }

    jsonWriter.endArray();
  }

  public static void writePlugin(JsonWriter jsonWriter, Plugin plugin) {
    jsonWriter.prop(PROPERTY_KEY, plugin.getKey());
    jsonWriter.prop(PROPERTY_NAME, plugin.getName());
    jsonWriter.prop(PROPERTY_CATEGORY, plugin.getCategory());
    jsonWriter.prop(PROPERTY_DESCRIPTION, plugin.getDescription());
    jsonWriter.prop(PROPERTY_LICENSE, plugin.getLicense());
    jsonWriter.prop(PROPERTY_TERMS_AND_CONDITIONS_URL, plugin.getTermsConditionsUrl());
    jsonWriter.prop(PROPERTY_ORGANIZATION_NAME, plugin.getOrganization());
    jsonWriter.prop(PROPERTY_ORGANIZATION_URL, plugin.getOrganizationUrl());
    jsonWriter.prop(PROPERTY_HOMEPAGE_URL, plugin.getHomepageUrl());
    jsonWriter.prop(PROPERTY_ISSUE_TRACKER_URL, plugin.getIssueTrackerUrl());
    jsonWriter.prop(PROPERTY_EDITION_BUNDLED, isEditionBundled(plugin));
  }
}
