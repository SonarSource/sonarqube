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
package org.sonar.core.persistence;

import com.google.common.io.Files;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.utils.SonarException;
import org.sonar.core.resource.ResourceDao;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

public class DryRunDatabaseFactory implements ServerComponent {
  public static final String SONAR_DRY_RUN_CACHE_KEY_PREFIX = "sonar.dryRun.cache.";
  public static final String SONAR_DRY_RUN_CACHE_LAST_UPDATE_KEY = SONAR_DRY_RUN_CACHE_KEY_PREFIX + "lastUpdate";
  private static final Logger LOG = LoggerFactory.getLogger(DryRunDatabaseFactory.class);
  private static final String DIALECT = "h2";
  private static final String DRIVER = "org.h2.Driver";
  private static final String URL = "jdbc:h2:";
  private static final String H2_FILE_SUFFIX = ".h2.db";
  private static final String SONAR = "sonar";
  private static final String USER = SONAR;
  private static final String PASSWORD = SONAR;

  private final Database database;
  private final ServerFileSystem serverFileSystem;
  private final Settings settings;
  private final ResourceDao resourceDao;

  public static String getCacheLastUpdateKey(Long rootProjectId) {
    return SONAR_DRY_RUN_CACHE_KEY_PREFIX + rootProjectId + ".lastUpdate";
  }

  public DryRunDatabaseFactory(Database database, ServerFileSystem serverFileSystem, Settings settings, ResourceDao resourceDao) {
    this.database = database;
    this.serverFileSystem = serverFileSystem;
    this.settings = settings;
    this.resourceDao = resourceDao;
  }

  private File getRootCacheLocation() {
    return new File(serverFileSystem.getTempDir(), "dryRun");
  }

  private File getCacheLocation(@Nullable Long projectId) {
    return new File(getRootCacheLocation(), projectId != null ? projectId.toString() : "default");
  }

  private Long getLastTimestampInCache(@Nullable Long projectId) {
    File cacheLocation = getCacheLocation(projectId);
    if (!cacheLocation.exists()) {
      return null;
    }
    Collection<File> dbInCache = FileUtils.listFiles(cacheLocation, FileFileFilter.FILE, null);
    if (dbInCache.isEmpty()) {
      return null;
    }
    SortedSet<Long> timestamps = new TreeSet<Long>();
    for (File file : dbInCache) {
      if (file.getName().endsWith(H2_FILE_SUFFIX)) {
        try {
          timestamps.add(Long.valueOf(StringUtils.removeEnd(file.getName(), H2_FILE_SUFFIX)));
        } catch (NumberFormatException e) {
          LOG.warn("Unexpected file in dryrun cache folder " + file.getAbsolutePath(), e);
        }
      }
    }
    if (timestamps.isEmpty()) {
      return null;
    }
    return timestamps.last();
  }

  private boolean isValid(@Nullable Long projectId, long lastTimestampInCache) {
    long globalTimestamp = settings.getLong(SONAR_DRY_RUN_CACHE_LAST_UPDATE_KEY);
    if (globalTimestamp > lastTimestampInCache) {
      return false;
    }
    if (projectId != null) {
      // For modules look for root project last modification timestamp
      Long rootId = resourceDao.getRootProjectByComponentId(projectId).getId();
      long projectTimestamp = settings.getLong(getCacheLastUpdateKey(rootId));
      if (projectTimestamp > lastTimestampInCache) {
        return false;
      }
    }
    return true;
  }

  public byte[] createDatabaseForDryRun(@Nullable Long projectId) {
    long startup = System.currentTimeMillis();

    Long lastTimestampInCache = getLastTimestampInCache(projectId);
    if (lastTimestampInCache == null || !isValid(projectId, lastTimestampInCache)) {
      lastTimestampInCache = System.nanoTime();
      cleanCache(projectId);
      createNewDatabaseForDryRun(projectId, startup, lastTimestampInCache);
    }
    return dbFileContent(projectId, lastTimestampInCache);
  }

  private void cleanCache(@Nullable Long projectId) {
    FileUtils.deleteQuietly(getCacheLocation(projectId));
  }

  public String getH2DBName(File location, long timestamp) {
    return location.getAbsolutePath() + File.separator + timestamp;
  }

  public String getTemporaryH2DBName(File location, long timestamp) {
    return location.getAbsolutePath() + File.separator + ".tmp" + timestamp;
  }

  private void createNewDatabaseForDryRun(Long projectId, long startup, Long lastTimestampInCache) {
    String tmpName = getTemporaryH2DBName(getCacheLocation(projectId), lastTimestampInCache);
    String finalName = getH2DBName(getCacheLocation(projectId), lastTimestampInCache);

    try {
      DataSource source = database.getDataSource();
      BasicDataSource destination = create(DIALECT, DRIVER, USER, PASSWORD, URL + tmpName);

      copy(source, destination, projectId);
      close(destination);

      File tempDbFile = new File(tmpName + H2_FILE_SUFFIX);
      File dbFile = new File(finalName + H2_FILE_SUFFIX);
      Files.move(tempDbFile, dbFile);
      if (LOG.isDebugEnabled()) {
        long size = dbFile.length();
        long duration = System.currentTimeMillis() - startup;
        if (projectId == null) {
          LOG.debug("Dry Run Database created in " + duration + " ms, size is " + size + " bytes");
        } else {
          LOG.debug("Dry Run Database for project " + projectId + " created in " + duration + " ms, size is " + size + " bytes");
        }
      }

    } catch (SQLException e) {
      throw new SonarException("Unable to create database for DryRun", e);
    } catch (IOException e) {
      throw new SonarException("Unable to cache database for DryRun", e);
    }

  }

  private void copy(DataSource source, DataSource dest, @Nullable Long projectId) {
    DbTemplate template = new DbTemplate();
    template
      .copyTable(source, dest, "active_rules")
      .copyTable(source, dest, "active_rule_parameters")
      .copyTable(source, dest, "characteristics")
      .copyTable(source, dest, "characteristic_edges")
      .copyTable(source, dest, "characteristic_properties")
      .copyTable(source, dest, "metrics")
      .copyTable(source, dest, "permission_templates")
      .copyTable(source, dest, "perm_templates_users")
      .copyTable(source, dest, "perm_templates_groups")
      .copyTable(source, dest, "quality_models")
      .copyTable(source, dest, "rules")
      .copyTable(source, dest, "rules_parameters")
      .copyTable(source, dest, "rules_profiles")
      .copyTable(source, dest, "alerts");
    if (projectId != null) {
      template.copyTable(source, dest, "projects", projectQuery(projectId, false));

      template.copyTable(source, dest, "snapshots", "SELECT * FROM snapshots WHERE project_id=" + projectId);
      template.copyTable(source, dest, "project_measures", "SELECT m.* FROM project_measures m INNER JOIN snapshots s on m.snapshot_id=s.id WHERE s.project_id=" + projectId);

      StringBuilder issueQuery = new StringBuilder()
        .append("SELECT issues.* FROM issues")
        .append(" INNER JOIN (")
        .append(projectQuery(projectId, true))
        .append(") resources")
        .append(" ON issues.component_id=resources.id")
        .append(" AND status <> '").append(Issue.STATUS_CLOSED).append("'");
      template.copyTable(source, dest, "issues", issueQuery.toString());
    }
  }

  private String projectQuery(Long projectId, boolean returnOnlyIds) {
    return new StringBuilder()
      .append("SELECT p.").append(returnOnlyIds ? "id" : "*")
      .append(" FROM projects p INNER JOIN snapshots s ON p.id = s.project_id")
      .append(" WHERE s.islast=").append(database.getDialect().getTrueSqlValue())
      .append(" AND s.root_project_id=").append(projectId)
      .append(" UNION")
      .append(" SELECT p.").append(returnOnlyIds ? "id" : "*")
      .append(" FROM projects p")
      .append(" WHERE p.id=").append(projectId)
      .append(" OR p.root_id=").append(projectId).toString();
  }

  private BasicDataSource create(String dialect, String driver, String user, String password, String url) {
    BasicDataSource dataSource = new DbTemplate().dataSource(driver, user, password, url);
    new DbTemplate().createSchema(dataSource, dialect);
    return dataSource;
  }

  private void close(BasicDataSource destination) throws SQLException {
    destination.close();
  }

  private byte[] dbFileContent(@Nullable Long projectId, long timestamp) {
    File cacheLocation = getCacheLocation(projectId);
    try {
      FileUtils.forceMkdir(cacheLocation);
    } catch (IOException e) {
      throw new SonarException("Unable to create cache directory " + cacheLocation, e);
    }
    String name = getH2DBName(cacheLocation, timestamp);
    try {
      File dbFile = new File(name + H2_FILE_SUFFIX);
      return Files.toByteArray(dbFile);
    } catch (IOException e) {
      throw new SonarException("Unable to read h2 database file", e);
    }
  }

}
