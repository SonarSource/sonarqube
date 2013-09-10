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

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerExtension;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.utils.SonarException;
import org.sonar.core.persistence.DryRunDatabaseFactory;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @since 3.7.1
 */
public class DryRunCache implements ServerExtension {

  private static final Logger LOG = LoggerFactory.getLogger(DryRunCache.class);

  public static final String SONAR_DRY_RUN_CACHE_LAST_UPDATE_KEY = "sonar.dryRun.cache.lastUpdate";

  private ServerFileSystem serverFileSystem;
  private PropertiesDao propertiesDao;
  private ResourceDao resourceDao;

  private Map<Long, ReadWriteLock> lockPerProject = new HashMap<Long, ReadWriteLock>();
  private Map<Long, Long> lastTimestampPerProject = new HashMap<Long, Long>();

  private DryRunDatabaseFactory dryRunDatabaseFactory;

  public DryRunCache(ServerFileSystem serverFileSystem, PropertiesDao propertiesDao, ResourceDao resourceDao, DryRunDatabaseFactory dryRunDatabaseFactory) {
    this.serverFileSystem = serverFileSystem;
    this.propertiesDao = propertiesDao;
    this.resourceDao = resourceDao;
    this.dryRunDatabaseFactory = dryRunDatabaseFactory;
  }

  public byte[] getDatabaseForDryRun(@Nullable Long projectId) {
    long notNullProjectId = projectId != null ? projectId.longValue() : 0L;
    ReadWriteLock rwl = getLock(notNullProjectId);
    try {
      rwl.readLock().lock();
      if (!isCacheValid(projectId)) {
        // upgrade lock manually
        // must unlock first to obtain writelock
        rwl.readLock().unlock();
        rwl.writeLock().lock();
        // recheck
        if (!isCacheValid(projectId)) {
          generateNewDB(projectId);
        }
        // downgrade lock
        // reacquire read without giving up write lock
        rwl.readLock().lock();
        // unlock write, still hold read
        rwl.writeLock().unlock();
      }
      File dbFile = new File(getCacheLocation(projectId), lastTimestampPerProject.get(notNullProjectId) + DryRunDatabaseFactory.H2_FILE_SUFFIX);
      return fileToByte(dbFile);
    } finally {
      rwl.readLock().unlock();
    }
  }

  private boolean isCacheValid(@Nullable Long projectId) {
    long notNullProjectId = projectId != null ? projectId.longValue() : 0L;
    Long lastTimestampInCache = lastTimestampPerProject.get(notNullProjectId);
    LOG.debug("Timestamp of last cached DB is {}", lastTimestampInCache);
    if (lastTimestampInCache != null && isValid(projectId, lastTimestampInCache.longValue())) {
      File dbFile = new File(getCacheLocation(projectId), lastTimestampInCache + DryRunDatabaseFactory.H2_FILE_SUFFIX);
      LOG.debug("Look for existence of cached DB at {}", dbFile);
      if (dbFile.exists()) {
        LOG.debug("Found cached DB at {}", dbFile);
        return true;
      }
    }
    return false;
  }

  private void generateNewDB(@Nullable Long projectId) {
    if (projectId != null) {
      LOG.info("Generate new dryRun database for project [id={}]", projectId);
    } else {
      LOG.info("Generate new dryRun database for new project");
    }
    long notNullProjectId = projectId != null ? projectId.longValue() : 0L;
    long newTimestamp = System.currentTimeMillis();
    File cacheLocation = getCacheLocation(projectId);
    FileUtils.deleteQuietly(cacheLocation);
    File dbFile = dryRunDatabaseFactory.createNewDatabaseForDryRun(projectId, cacheLocation, String.valueOf(newTimestamp));
    LOG.info("Cached DB at {}", dbFile);
    lastTimestampPerProject.put(notNullProjectId, newTimestamp);
  }

  private byte[] fileToByte(File dbFile) {
    try {
      return Files.toByteArray(dbFile);
    } catch (IOException e) {
      throw new SonarException("Unable to create h2 database file", e);
    }
  }

  private synchronized ReadWriteLock getLock(long notNullProjectId) {
    if (!lockPerProject.containsKey(notNullProjectId)) {
      lockPerProject.put(notNullProjectId, new ReentrantReadWriteLock(true));
    }
    return lockPerProject.get(notNullProjectId);
  }

  private File getRootCacheLocation() {
    return new File(serverFileSystem.getTempDir(), "dryRun");
  }

  public File getCacheLocation(@Nullable Long projectId) {
    return new File(getRootCacheLocation(), projectId != null ? projectId.toString() : "default");
  }

  private boolean isValid(@Nullable Long projectId, long lastTimestampInCache) {
    long globalTimestamp = getModificationTimestamp(null);
    if (globalTimestamp > lastTimestampInCache) {
      return false;
    }
    if (projectId != null) {
      long projectTimestamp = getModificationTimestamp(projectId);
      if (projectTimestamp > lastTimestampInCache) {
        return false;
      }
    }
    return true;
  }

  private long getModificationTimestamp(@Nullable Long projectId) {
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

  public void reportGlobalModification() {
    propertiesDao.setProperty(new PropertyDto().setKey(SONAR_DRY_RUN_CACHE_LAST_UPDATE_KEY).setValue(String.valueOf(System.currentTimeMillis())));
  }

  public void reportResourceModification(String resourceKey) {
    ResourceDto rootProject = resourceDao.getRootProjectByComponentKey(resourceKey);
    if (rootProject == null) {
      throw new SonarException("Unable to find root project for component with [key=" + resourceKey + "]");
    }
    propertiesDao.setProperty(new PropertyDto().setKey(SONAR_DRY_RUN_CACHE_LAST_UPDATE_KEY).setResourceId(rootProject.getId())
      .setValue(String.valueOf(System.currentTimeMillis())));
  }
}
