/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.dryrun;

import org.apache.commons.io.FileUtils;
import org.sonar.api.ServerExtension;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.utils.SonarException;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;

import javax.annotation.Nullable;

import java.io.File;

/**
 * @since 4.0
 */
public class DryRunCache implements ServerExtension {

  public static final String SONAR_DRY_RUN_CACHE_LAST_UPDATE_KEY = "sonar.dryRun.cache.lastUpdate";

  private ServerFileSystem serverFileSystem;
  private PropertiesDao propertiesDao;
  private ResourceDao resourceDao;

  public DryRunCache(ServerFileSystem serverFileSystem, PropertiesDao propertiesDao, ResourceDao resourceDao) {
    this.serverFileSystem = serverFileSystem;
    this.propertiesDao = propertiesDao;
    this.resourceDao = resourceDao;
  }

  private File getRootCacheLocation() {
    return new File(serverFileSystem.getTempDir(), "dryRun");
  }

  public File getCacheLocation(@Nullable Long projectId) {
    return new File(getRootCacheLocation(), projectId != null ? projectId.toString() : "default");
  }

  public long getModificationTimestamp(@Nullable Long projectId) {
    if (projectId == null) {
      PropertyDto dto = propertiesDao.selectGlobalProperty(SONAR_DRY_RUN_CACHE_LAST_UPDATE_KEY);
      if (dto == null) {
        return 0;
      }
      return Long.valueOf(dto.getValue());
    }
    // For modules look for root project last modification timestamp
    ResourceDto rootProject = resourceDao.getRootProjectByComponentId(projectId);
    if (rootProject == null) {
      throw new SonarException("Unable to find root project for project with [id=" + projectId + "]");
    }
    PropertyDto dto = propertiesDao.selectProjectProperty(rootProject.getId(), SONAR_DRY_RUN_CACHE_LAST_UPDATE_KEY);
    if (dto == null) {
      return 0;
    }
    return Long.valueOf(dto.getValue());
  }

  public void cleanAll() {
    // Delete folder where dryRun DB are stored
    FileUtils.deleteQuietly(getRootCacheLocation());
    // Delete all lastUpdate properties to force generation of new DB
    propertiesDao.deleteAllProperties(SONAR_DRY_RUN_CACHE_LAST_UPDATE_KEY);
  }

  public void clean(@Nullable Long resourceId) {
    // Delete folder where dryRun DB are stored
    FileUtils.deleteQuietly(getCacheLocation(resourceId));
  }

  public void reportGlobalModification() {
    // Delete folder where dryRun DB are stored
    FileUtils.deleteQuietly(getRootCacheLocation());

    propertiesDao.setProperty(new PropertyDto().setKey(SONAR_DRY_RUN_CACHE_LAST_UPDATE_KEY).setValue(String.valueOf(System.nanoTime())));
  }

  public void reportResourceModification(String resourceKey) {
    ResourceDto rootProject = resourceDao.getRootProjectByComponentKey(resourceKey);
    if (rootProject == null) {
      throw new SonarException("Unable to find root project for component with [key=" + resourceKey + "]");
    }
    propertiesDao.setProperty(new PropertyDto().setKey(SONAR_DRY_RUN_CACHE_LAST_UPDATE_KEY).setResourceId(rootProject.getId())
      .setValue(String.valueOf(System.nanoTime())));
  }
}
