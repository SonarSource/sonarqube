/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db;

import com.google.common.io.Closeables;
import java.io.InputStream;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.dialect.Dialect;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

class MyBatisConfBuilder {
  private final Configuration conf;

  MyBatisConfBuilder(Database database) {
    this.conf = new Configuration();
    this.conf.setEnvironment(new Environment("production", createTransactionFactory(), database.getDataSource()));
    this.conf.setUseGeneratedKeys(true);
    this.conf.setLazyLoadingEnabled(false);
    this.conf.setJdbcTypeForNull(JdbcType.NULL);
    Dialect dialect = database.getDialect();
    this.conf.setDatabaseId(dialect.getId());
    this.conf.getVariables().setProperty("_true", dialect.getTrueSqlValue());
    this.conf.getVariables().setProperty("_false", dialect.getFalseSqlValue());
    this.conf.getVariables().setProperty("_scrollFetchSize", String.valueOf(dialect.getScrollDefaultFetchSize()));
    this.conf.setLocalCacheScope(LocalCacheScope.STATEMENT);
  }

  void loadAlias(String alias, Class dtoClass) {
    conf.getTypeAliasRegistry().registerAlias(alias, dtoClass);
  }

  void loadMapper(Class mapperClass) {
    String configFile = configFilePath(mapperClass);
    InputStream input = null;
    try {
      input = mapperClass.getResourceAsStream(configFile);
      checkArgument(input != null, format("Can not find mapper XML file %s", configFile));
      new SQXMLMapperBuilder(mapperClass, input, conf, conf.getSqlFragments()).parse();
      loadAndConfigureLogger(mapperClass.getName());
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to load mapper " + mapperClass, e);
    } finally {
      Closeables.closeQuietly(input);
    }
  }

  private static String configFilePath(Class mapperClass) {
    return configFilePath(mapperClass.getName());
  }

  private static String configFilePath(String mapperName) {
    return "/" + mapperName.replace('.', '/') + ".xml";
  }

  private void loadAndConfigureLogger(String mapperName) {
    conf.addLoadedResource(mapperName);
    Loggers.get(mapperName).setLevel(LoggerLevel.INFO);
  }

  void loadMappers(Class<?>... mapperClasses) {
    for (Class mapperClass : mapperClasses) {
      loadMapper(mapperClass);
    }
  }

  public Configuration build() {
    return conf;
  }

  private static JdbcTransactionFactory createTransactionFactory() {
    return new JdbcTransactionFactory();
  }
}
