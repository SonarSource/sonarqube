/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.plugins;

import com.google.common.base.Function;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.SonarException;
import org.sonar.updatecenter.common.PluginManifest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public abstract class PluginJarInstaller implements BatchComponent, ServerComponent {

  protected static final String FAIL_TO_INSTALL_PLUGIN = "Fail to install plugin: ";

  protected void install(DefaultPluginMetadata metadata, @Nullable File pluginBasedir, File deployedPlugin) {
    try {
      metadata.addDeployedFile(deployedPlugin);
      copyDependencies(metadata, deployedPlugin, pluginBasedir);
    } catch (IOException e) {
      throw new SonarException(FAIL_TO_INSTALL_PLUGIN + metadata, e);
    }
  }

  private void copyDependencies(DefaultPluginMetadata metadata, File pluginFile, @Nullable File pluginBasedir) throws IOException {
    if (!metadata.getPathsToInternalDeps().isEmpty()) {
      // needs to unzip the jar
      File baseDir = extractPluginDependencies(pluginFile, pluginBasedir);
      for (String depPath : metadata.getPathsToInternalDeps()) {
        File dependency = new File(baseDir, depPath);
        if (!dependency.isFile() || !dependency.exists()) {
          throw new IllegalArgumentException("Dependency " + depPath + " can not be found in " + pluginFile.getName());
        }
        metadata.addDeployedFile(dependency);
      }
    }
  }

  protected abstract File extractPluginDependencies(File pluginFile, @Nullable File pluginBasedir) throws IOException;

  public DefaultPluginMetadata extractMetadata(File file, boolean isCore) {
    try {
      PluginManifest manifest = new PluginManifest(file);
      DefaultPluginMetadata metadata = DefaultPluginMetadata.create(file);
      metadata.setKey(manifest.getKey());
      metadata.setName(manifest.getName());
      metadata.setDescription(manifest.getDescription());
      metadata.setLicense(manifest.getLicense());
      metadata.setOrganization(manifest.getOrganization());
      metadata.setOrganizationUrl(manifest.getOrganizationUrl());
      metadata.setMainClass(manifest.getMainClass());
      metadata.setVersion(manifest.getVersion());
      metadata.setSonarVersion(manifest.getSonarVersion());
      metadata.setHomepage(manifest.getHomepage());
      metadata.setIssueTrackerUrl(manifest.getIssueTrackerUrl());
      metadata.setPathsToInternalDeps(Arrays.asList(manifest.getDependencies()));
      metadata.setUseChildFirstClassLoader(manifest.isUseChildFirstClassLoader());
      metadata.setBasePlugin(manifest.getBasePlugin());
      metadata.setImplementationBuild(manifest.getImplementationBuild());
      metadata.setRequiredPlugins(Arrays.asList(manifest.getRequirePlugins()));
      metadata.setCore(isCore);
      return metadata;

    } catch (IOException e) {
      throw new IllegalStateException("Fail to extract plugin metadata from file: " + file, e);
    }
  }

  public Function<File, DefaultPluginMetadata> fileToPlugin() {
    return jarFileToPlugin;
  }

  public Function<File, DefaultPluginMetadata> fileToCorePlugin() {
    return jarFileToCorePlugin;
  }

  private final Function<File, DefaultPluginMetadata> jarFileToCorePlugin = new Function<File, DefaultPluginMetadata>() {
    @Override
    public DefaultPluginMetadata apply(@Nonnull File file) {
      return extractMetadata(file, true);
    }
  };
  private final Function<File, DefaultPluginMetadata> jarFileToPlugin = new Function<File, DefaultPluginMetadata>() {
    @Override
    public DefaultPluginMetadata apply(@Nonnull File file) {
      return extractMetadata(file, false);
    }
  };
}
