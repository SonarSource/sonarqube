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
package org.sonar.core.plugins;

import org.apache.commons.io.FileUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.ZipUtils;
import org.sonar.updatecenter.common.PluginManifest;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;

public class PluginInstaller {

  public DefaultPluginMetadata install(File pluginFile, boolean isCore, List<File> deprecatedExtensions, File toDir) {
    DefaultPluginMetadata metadata = extractMetadata(pluginFile, isCore);
    metadata.setDeprecatedExtensions(deprecatedExtensions);
    return install(metadata, toDir);
  }

  public DefaultPluginMetadata install(DefaultPluginMetadata metadata, File toDir) {
    try {
      File pluginFile = metadata.getFile();
      File pluginBasedir = copyPlugin(metadata, toDir, pluginFile);
      copyDependencies(metadata, pluginFile, pluginBasedir);
      copyDeprecatedExtensions(metadata, pluginBasedir);

      return metadata;

    } catch (IOException e) {
      throw new SonarException("Fail to install plugin: " + metadata, e);
    }
  }

  private File copyPlugin(DefaultPluginMetadata metadata, File toDir, File pluginFile) throws IOException {
    File pluginBasedir = toDir;
    FileUtils.forceMkdir(pluginBasedir);
    File targetFile = new File(pluginBasedir, pluginFile.getName());
    FileUtils.copyFile(pluginFile, targetFile);
    metadata.addDeployedFile(targetFile);
    return pluginBasedir;
  }

  private void copyDependencies(DefaultPluginMetadata metadata, File pluginFile, File pluginBasedir) throws IOException {
    if (metadata.getPathsToInternalDeps().length > 0) {
      // needs to unzip the jar
      ZipUtils.unzip(pluginFile, pluginBasedir, new LibFilter());
      for (String depPath : metadata.getPathsToInternalDeps()) {
        File dependency = new File(pluginBasedir, depPath);
        if (!dependency.isFile() || !dependency.exists()) {
          throw new IllegalArgumentException("Dependency " + depPath + " can not be found in " + pluginFile.getName());
        }
        metadata.addDeployedFile(dependency);
      }
    }
  }

  private void copyDeprecatedExtensions(DefaultPluginMetadata metadata, File pluginBasedir) throws IOException {
    for (File extension : metadata.getDeprecatedExtensions()) {
      File toFile = new File(pluginBasedir, extension.getName());
      if (!toFile.equals(extension)) {
        FileUtils.copyFile(extension, toFile);
      }
      metadata.addDeployedFile(toFile);
    }
  }

  private static final class LibFilter implements ZipUtils.ZipEntryFilter {
    public boolean accept(ZipEntry entry) {
      return entry.getName().startsWith("META-INF/lib");
    }
  }

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
      metadata.setPathsToInternalDeps(manifest.getDependencies());
      metadata.setUseChildFirstClassLoader(manifest.isUseChildFirstClassLoader());
      metadata.setBasePlugin(manifest.getBasePlugin());
      metadata.setImplementationBuild(manifest.getImplementationBuild());
      metadata.setCore(isCore);
      return metadata;

    } catch (IOException e) {
      throw new IllegalStateException("Fail to extract plugin metadata from file: " + file, e);
    }
  }
}
