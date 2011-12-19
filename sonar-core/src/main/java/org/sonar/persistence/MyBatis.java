/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.persistence;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.persistence.dashboard.*;
import org.sonar.persistence.duplication.DuplicationMapper;
import org.sonar.persistence.duplication.DuplicationUnitDto;
import org.sonar.persistence.resource.ResourceDto;
import org.sonar.persistence.resource.ResourceIndexDto;
import org.sonar.persistence.resource.ResourceIndexerMapper;
import org.sonar.persistence.review.ReviewDto;
import org.sonar.persistence.review.ReviewMapper;
import org.sonar.persistence.rule.RuleDto;
import org.sonar.persistence.rule.RuleMapper;
import org.sonar.persistence.template.LoadedTemplateDto;
import org.sonar.persistence.template.LoadedTemplateMapper;

import java.io.IOException;
import java.io.InputStream;

public class MyBatis implements BatchComponent, ServerComponent {

  private Database database;
  private SqlSessionFactory sessionFactory;

  public MyBatis(Database database) {
    this.database = database;
  }

  public MyBatis start() throws IOException {
    Configuration conf = new Configuration();
    conf.setEnvironment(new Environment("production", createTransactionFactory(), database.getDataSource()));
    conf.setUseGeneratedKeys(true);
    conf.setLazyLoadingEnabled(false);

    loadAlias(conf, "ActiveDashboard", ActiveDashboardDto.class);
    loadAlias(conf, "Dashboard", DashboardDto.class);
    loadAlias(conf, "DuplicationUnit", DuplicationUnitDto.class);
    loadAlias(conf, "LoadedTemplate", LoadedTemplateDto.class);
    loadAlias(conf, "Review", ReviewDto.class);
    loadAlias(conf, "Resource", ResourceDto.class);
    loadAlias(conf, "ResourceIndex", ResourceIndexDto.class);
    loadAlias(conf, "Rule", RuleDto.class);
    loadAlias(conf, "Widget", WidgetDto.class);
    loadAlias(conf, "WidgetProperty", WidgetPropertyDto.class);

    loadMapper(conf, ActiveDashboardMapper.class);
    loadMapper(conf, DashboardMapper.class);
    loadMapper(conf, DuplicationMapper.class);
    loadMapper(conf, LoadedTemplateMapper.class);
    loadMapper(conf, ReviewMapper.class);
    loadMapper(conf, ResourceIndexerMapper.class);
    loadMapper(conf, RuleMapper.class);
    loadMapper(conf, WidgetMapper.class);
    loadMapper(conf, WidgetPropertyMapper.class);

    sessionFactory = new SqlSessionFactoryBuilder().build(conf);
    return this;
  }

  public SqlSessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public SqlSession openSession() {
    return sessionFactory.openSession();
  }

  public SqlSession openSession(ExecutorType type) {
    return sessionFactory.openSession(type);
  }

  private void loadMapper(Configuration conf, Class mapperClass) throws IOException {
    // trick to use database-specific XML files for a single Mapper Java interface
    InputStream input = getPathToMapper(mapperClass);
    try {
      XMLMapperBuilder mapperParser = new XMLMapperBuilder(input, conf, mapperClass.getName(), conf.getSqlFragments());
      mapperParser.parse();
      conf.addLoadedResource(mapperClass.getName());

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  private InputStream getPathToMapper(Class mapperClass) {
    InputStream input = getClass().getResourceAsStream(
      "/" + StringUtils.replace(mapperClass.getName(), ".", "/") + "-" + database.getDialect().getId() + ".xml");
    if (input == null) {
      input = getClass().getResourceAsStream("/" + StringUtils.replace(mapperClass.getName(), ".", "/") + ".xml");
    }
    return input;
  }

  private void loadAlias(Configuration conf, String alias, Class dtoClass) {
    conf.getTypeAliasRegistry().registerAlias(alias, dtoClass);
  }

  private static JdbcTransactionFactory createTransactionFactory() {
    return new JdbcTransactionFactory();
  }

}
