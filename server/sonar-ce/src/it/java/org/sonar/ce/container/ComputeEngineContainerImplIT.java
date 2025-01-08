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
package org.sonar.ce.container;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.extension.ServiceLoaderWrapper;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;
import org.sonar.server.property.InternalProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_INDEX;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;
import static org.sonar.process.ProcessProperties.Property.JDBC_PASSWORD;
import static org.sonar.process.ProcessProperties.Property.JDBC_URL;
import static org.sonar.process.ProcessProperties.Property.JDBC_USERNAME;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;

public class ComputeEngineContainerImplIT {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  @Rule

  public DbTester db = DbTester.create(System2.INSTANCE);

  private final ServiceLoaderWrapper serviceLoaderWrapper = mock(ServiceLoaderWrapper.class);
  private final ProcessProperties processProperties = new ProcessProperties(serviceLoaderWrapper);
  private final ComputeEngineContainerImpl underTest = new ComputeEngineContainerImpl();

  @Before
  public void setUp() {
    when(serviceLoaderWrapper.load()).thenReturn(ImmutableSet.of());
    underTest.setComputeEngineStatus(mock(ComputeEngineStatus.class));
  }

  @Test
  public void constructor_does_not_create_container() {
    assertThat(underTest.getComponentContainer()).isNull();
  }

  @Test
  public void test_real_start() throws IOException {
    Properties properties = getProperties();

    // required persisted properties
    insertProperty(CoreProperties.SERVER_ID, "a_server_id");
    insertProperty(CoreProperties.SERVER_STARTTIME, DateUtils.formatDateTime(new Date()));
    insertInternalProperty(InternalProperties.SERVER_ID_CHECKSUM, DigestUtils.sha256Hex("a_server_id|" + cleanJdbcUrl()));

    underTest.start(new Props(properties));

    AnnotationConfigApplicationContext context = underTest.getComponentContainer().context();
    try {
      assertThat(context.getBeanDefinitionNames()).hasSizeGreaterThan(1);
      assertThat(context.getParent().getBeanDefinitionNames()).hasSizeGreaterThan(1);
      assertThat(context.getParent().getParent().getBeanDefinitionNames()).hasSizeGreaterThan(1);
      assertThat(context.getParent().getParent().getParent().getBeanDefinitionNames()).hasSizeGreaterThan(1);
      assertThat(context.getParent().getParent().getParent().getParent()).isNull();
    } finally {
      underTest.stop();
    }

    assertThat(context.isActive()).isFalse();
  }

  private String cleanJdbcUrl() {
    return StringUtils.lowerCase(StringUtils.substringBefore(db.getUrl(), "?"), Locale.ENGLISH);
  }

  private Properties getProperties() throws IOException {
    Properties properties = new Properties();
    Props props = new Props(properties);
    processProperties.completeDefaults(props);
    properties = props.rawProperties();
    File homeDir = tempFolder.newFolder();
    File dataDir = new File(homeDir, "data");
    dataDir.mkdirs();
    File tmpDir = new File(homeDir, "tmp");
    tmpDir.mkdirs();
    properties.setProperty(PATH_HOME.getKey(), homeDir.getAbsolutePath());
    properties.setProperty(PATH_DATA.getKey(), dataDir.getAbsolutePath());
    properties.setProperty(PATH_TEMP.getKey(), tmpDir.getAbsolutePath());
    properties.setProperty(PROPERTY_PROCESS_INDEX, valueOf(ProcessId.COMPUTE_ENGINE.getIpcIndex()));
    properties.setProperty(PROPERTY_SHARED_PATH, tmpDir.getAbsolutePath());
    properties.setProperty(JDBC_URL.getKey(), db.getUrl());
    properties.setProperty(JDBC_USERNAME.getKey(), "sonar");
    properties.setProperty(JDBC_PASSWORD.getKey(), "sonar");
    return properties;
  }

  private void insertProperty(String key, String value) {
    PropertyDto dto = new PropertyDto().setKey(key).setValue(value);
    db.getDbClient().propertiesDao().saveProperty(db.getSession(), dto, null, null, null, null);
    db.commit();
  }

  private void insertInternalProperty(String key, String value) {
    db.getDbClient().internalPropertiesDao().save(db.getSession(), key, value);
    db.commit();
  }
}
