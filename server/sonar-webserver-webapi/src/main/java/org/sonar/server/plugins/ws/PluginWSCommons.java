/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.plugins.ws;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.core.platform.PluginInfo;
import org.sonar.db.plugin.PluginDto;
import org.sonar.server.plugins.ServerPlugin;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.updatecenter.common.Artifact;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonarqube.ws.Plugins.PluginDetails;
import org.sonarqube.ws.Plugins.Release;
import org.sonarqube.ws.Plugins.Require;
import org.sonarqube.ws.Plugins.UpdateStatus;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonar.api.utils.DateUtils.formatDate;
import static org.sonar.server.plugins.edition.EditionBundledPlugins.isEditionBundled;
import static org.sonarqube.ws.Plugins.UpdateStatus.COMPATIBLE;
import static org.sonarqube.ws.Plugins.UpdateStatus.DEPS_REQUIRE_SYSTEM_UPGRADE;
import static org.sonarqube.ws.Plugins.UpdateStatus.INCOMPATIBLE;
import static org.sonarqube.ws.Plugins.UpdateStatus.REQUIRES_SYSTEM_UPGRADE;

public class PluginWSCommons {

  public static final Ordering<PluginInfo> NAME_KEY_PLUGIN_METADATA_COMPARATOR = Ordering.natural()
    .onResultOf(PluginInfo::getName)
    .compound(Ordering.natural().onResultOf(PluginInfo::getKey));
  public static final Comparator<ServerPlugin> NAME_KEY_COMPARATOR = Comparator
    .comparing((java.util.function.Function<ServerPlugin, String>) installedPluginFile -> installedPluginFile.getPluginInfo().getName())
    .thenComparing(f -> f.getPluginInfo().getKey());
  public static final Comparator<Plugin> NAME_KEY_PLUGIN_ORDERING = Ordering.from(CASE_INSENSITIVE_ORDER)
    .onResultOf(Plugin::getName)
    .compound(
      Ordering.from(CASE_INSENSITIVE_ORDER).onResultOf(Artifact::getKey));
  public static final Comparator<PluginUpdate> NAME_KEY_PLUGIN_UPDATE_ORDERING = Ordering.from(NAME_KEY_PLUGIN_ORDERING)
    .onResultOf(PluginUpdate::getPlugin);

  private PluginWSCommons() {
    // prevent instantiation
  }

  public static PluginDetails buildPluginDetails(@Nullable ServerPlugin installedPlugin, PluginInfo pluginInfo,
    @Nullable PluginDto pluginDto, @Nullable Plugin updateCenterPlugin) {
    PluginDetails.Builder builder = PluginDetails.newBuilder()
      .setKey(pluginInfo.getKey())
      .setName(pluginInfo.getName())
      .setEditionBundled(isEditionBundled(pluginInfo))
      .setSonarLintSupported(pluginInfo.isSonarLintSupported());

    ofNullable(installedPlugin).ifPresent(serverPlugin -> {
      builder.setFilename(installedPlugin.getJar().getFile().getName());
      builder.setHash(installedPlugin.getJar().getMd5());
      builder.setType(installedPlugin.getType().name());
    });

    ofNullable(pluginInfo.getVersion()).ifPresent(v -> builder.setVersion(isNotBlank(pluginInfo.getDisplayVersion()) ? pluginInfo.getDisplayVersion() : v.getName()));
    ofNullable(updateCenterPlugin).flatMap(p -> ofNullable(p.getCategory())).ifPresent(builder::setCategory);
    ofNullable(pluginDto).ifPresent(p -> builder.setUpdatedAt(p.getUpdatedAt()));
    ofNullable(pluginInfo.getDescription()).ifPresent(builder::setDescription);
    ofNullable(pluginInfo.getLicense()).ifPresent(builder::setLicense);
    ofNullable(pluginInfo.getOrganizationName()).ifPresent(builder::setOrganizationName);
    ofNullable(pluginInfo.getOrganizationUrl()).ifPresent(builder::setOrganizationUrl);
    ofNullable(pluginInfo.getHomepageUrl()).ifPresent(builder::setHomepageUrl);
    ofNullable(pluginInfo.getIssueTrackerUrl()).ifPresent(builder::setIssueTrackerUrl);
    ofNullable(pluginInfo.getImplementationBuild()).ifPresent(builder::setImplementationBuild);
    ofNullable(pluginInfo.getDocumentationPath()).ifPresent(builder::setDocumentationPath);
    builder.addAllRequiredForLanguages(pluginInfo.getRequiredForLanguages());

    return builder.build();
  }

  static Release buildRelease(org.sonar.updatecenter.common.Release release) {
    String version = isNotBlank(release.getDisplayVersion()) ? release.getDisplayVersion() : release.getVersion().toString();
    Release.Builder releaseBuilder = Release.newBuilder().setVersion(version);
    ofNullable(release.getDate()).ifPresent(date -> releaseBuilder.setDate(formatDate(date)));
    ofNullable(release.getChangelogUrl()).ifPresent(releaseBuilder::setChangeLogUrl);
    ofNullable(release.getDescription()).ifPresent(releaseBuilder::setDescription);
    return releaseBuilder.build();
  }

  static List<Require> buildRequires(PluginUpdate pluginUpdate) {
    return pluginUpdate.getRelease().getOutgoingDependencies().stream().map(
        org.sonar.updatecenter.common.Release::getArtifact)
      .filter(Plugin.class::isInstance)
      .map(Plugin.class::cast)
      .map(artifact -> {
        Require.Builder builder = Require.newBuilder()
          .setKey(artifact.getKey());
        ofNullable(artifact.getName()).ifPresent(builder::setName);
        ofNullable(artifact.getDescription()).ifPresent(builder::setDescription);
        return builder.build();
      })
      .toList();
  }

  static UpdateStatus convertUpdateCenterStatus(PluginUpdate.Status status) {
    switch (status) {
      case COMPATIBLE:
        return COMPATIBLE;
      case INCOMPATIBLE:
        return INCOMPATIBLE;
      case REQUIRE_SONAR_UPGRADE:
        return REQUIRES_SYSTEM_UPGRADE;
      case DEPENDENCIES_REQUIRE_SONAR_UPGRADE:
        return DEPS_REQUIRE_SYSTEM_UPGRADE;
      default:
        throw new IllegalArgumentException("Unsupported value of PluginUpdate.Status " + status);
    }
  }

  private static List<Plugin> compatiblePlugins(UpdateCenterMatrixFactory updateCenterMatrixFactory) {
    Optional<UpdateCenter> updateCenter = updateCenterMatrixFactory.getUpdateCenter(false);
    return updateCenter.isPresent() ? updateCenter.get().findAllCompatiblePlugins() : Collections.emptyList();
  }

  static ImmutableMap<String, Plugin> compatiblePluginsByKey(UpdateCenterMatrixFactory updateCenterMatrixFactory) {
    List<Plugin> compatiblePlugins = compatiblePlugins(updateCenterMatrixFactory);
    return Maps.uniqueIndex(compatiblePlugins, Artifact::getKey);
  }
}
