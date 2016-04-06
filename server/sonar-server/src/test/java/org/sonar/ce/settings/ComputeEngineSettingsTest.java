/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.ce.settings;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Encryption;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.db.DbClient;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComputeEngineSettingsTest {
  private static final String PROPERTY_KEY_1 = "property key 1";
  private static final String PROPERTY_VALUE_1 = "property value 1";
  private static final String PROPERTY_KEY_2 = "property key 2";
  private static final String PROPERTY_VALUE_2 = "property value 2";
  private static final String OTHER_PROP_1 = "otherProp1";
  private static final String OTHER_PROP_1_VALUE = "otherProp1Value";
  private static final String OTHER_PROP_2 = "otherProp2";
  private static final String OTHER_PROP_2_VALUE = "otherProp2Value";
  private static final String PROPERTY_3 = "property 3";
  private static final String PROPERTY_VALUE_3 = "property value 3";
  private static final String THREAD_PROPERTY = "thread";
  private static final String MAIN = "main";
  private static final String CAPTOR = "captor";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PropertyDefinitions propertyDefinitions = new PropertyDefinitions();
  private Properties rootProperties = new Properties();
  private ComponentContainer container = new ComponentContainer();
  private DbClient dbClient = mock(DbClient.class);
  private PropertiesDao propertiesDao = mock(PropertiesDao.class);
  private ComputeEngineSettings underTest = new ComputeEngineSettings(propertyDefinitions, rootProperties, container);

  @Before
  public void setUp() throws Exception {
    rootProperties.setProperty(PROPERTY_KEY_1, PROPERTY_VALUE_1);
    rootProperties.setProperty(PROPERTY_KEY_2, PROPERTY_VALUE_2);
    container.add(dbClient);
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(propertiesDao.selectGlobalProperties()).thenReturn(ImmutableList.of(
      new PropertyDto().setKey(PROPERTY_KEY_1).setValue(PROPERTY_VALUE_1),
      new PropertyDto().setKey(PROPERTY_KEY_2).setValue(PROPERTY_VALUE_2)));
  }

  @After
  public void tearDown() throws Exception {
    // prevent ThreadLocal leak
    underTest.unload();
  }

  @Test
  public void activateDatabaseSettings_populates_default_settings_with_content_of_root_properties_plus_map_argument_content() {
    assertThat(underTest.getProperties()).isEmpty();

    ImmutableMap<String, String> properties = ImmutableMap.of(OTHER_PROP_1, OTHER_PROP_1_VALUE, OTHER_PROP_2, OTHER_PROP_2_VALUE);
    underTest.activateDatabaseSettings(properties);

    assertThat(underTest.getProperties()).containsOnly(
      entry(PROPERTY_KEY_1, PROPERTY_VALUE_1),
      entry(PROPERTY_KEY_2, PROPERTY_VALUE_2),
      entry(OTHER_PROP_1, OTHER_PROP_1_VALUE),
      entry(OTHER_PROP_2, OTHER_PROP_2_VALUE));

  }

  @Test
  public void load_creates_a_thread_specific_properties_map() throws InterruptedException {
    Map<String, String> defaultProperties = underTest.getProperties();
    assertThat(defaultProperties).isEmpty();

    rootProperties.setProperty(THREAD_PROPERTY, MAIN);
    underTest.load();

    assertThat(underTest.getProperties()).contains(entry(THREAD_PROPERTY, MAIN));

    PropertiesCaptorThread loadPropertiesCaptor = new PropertiesCaptorThread(true);

    loadPropertiesCaptor.blockingExecute();
    assertThat(loadPropertiesCaptor.properties).contains(entry(THREAD_PROPERTY, CAPTOR));

    PropertiesCaptorThread noLoadPropertiesCaptor = new PropertiesCaptorThread(false);

    noLoadPropertiesCaptor.blockingExecute();
    assertThat(noLoadPropertiesCaptor.properties).isEmpty();

    underTest.unload();

    assertThat(underTest.getProperties()).isEmpty();
  }

  @Test
  public void load_throws_ISE_if_load_called_twice_without_unload_in_between() {
    underTest.load();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("loadLocal called twice for Thread '" + Thread.currentThread().getName()
      + "' or state wasn't cleared last time it was used");

    underTest.load();
  }

  @Test
  public void getEncryption_returns_object_specific_to_current_thread_after_load() throws InterruptedException {
    Encryption defaultEncryption = underTest.getEncryption();

    underTest.load();

    Encryption mainEncryption = underTest.getEncryption();
    assertThat(mainEncryption).isNotSameAs(defaultEncryption);

    EncryptionCaptorThread loadEncryptionCaptor = new EncryptionCaptorThread(true);
    loadEncryptionCaptor.blockingExecute();

    assertThat(loadEncryptionCaptor.encryption).isNotSameAs(defaultEncryption);
    assertThat(loadEncryptionCaptor.encryption).isNotSameAs(mainEncryption);

    EncryptionCaptorThread noLoadEncryptionCaptor = new EncryptionCaptorThread(false);
    noLoadEncryptionCaptor.blockingExecute();

    assertThat(noLoadEncryptionCaptor.encryption).isSameAs(defaultEncryption);

    underTest.unload();

    assertThat(underTest.getEncryption()).isSameAs(defaultEncryption);
  }

  @Test
  public void getSettings_always_returns_current_ComputeEngineSettings_object() {
    assertThat(underTest.getSettings()).isSameAs(underTest);

    underTest.load();

    assertThat(underTest.getSettings()).isSameAs(underTest);

    underTest.unload();

    assertThat(underTest.getSettings()).isSameAs(underTest);
  }

  @Test
  public void clear_clears_settings_specific_to_current_thread_if_any() {
    underTest.activateDatabaseSettings(Collections.<String, String>emptyMap());
    assertThat(underTest.getProperties()).isNotEmpty();

    underTest.load();

    assertThat(underTest.getProperties()).isNotEmpty();

    underTest.clear();

    assertThat(underTest.getProperties()).isEmpty();

    underTest.unload();

    assertThat(underTest.getProperties()).isNotEmpty();

    underTest.clear();

    assertThat(underTest.getProperties()).isEmpty();
  }

  @Test
  public void removeProperty_removes_property_in_settings_specific_to_current_thread_if_any() {
    underTest.activateDatabaseSettings(Collections.<String, String>emptyMap());
    assertThat(underTest.getProperties()).containsKey(PROPERTY_KEY_2);

    underTest.load();

    assertThat(underTest.getProperties()).containsKey(PROPERTY_KEY_2);

    underTest.removeProperty(PROPERTY_KEY_2);

    assertThat(underTest.getProperties()).doesNotContainKey(PROPERTY_KEY_2);

    underTest.unload();

    assertThat(underTest.getProperties()).containsKey(PROPERTY_KEY_2);

    underTest.clear();

    assertThat(underTest.getProperties()).doesNotContainKey(PROPERTY_KEY_2);
  }

  @Test
  public void setProperty_sets_property_in_settings_specific_to_current_thread_if_any() {
    underTest.activateDatabaseSettings(Collections.<String, String>emptyMap());
    assertThat(underTest.getProperties()).doesNotContainKey(PROPERTY_3);

    underTest.load();

    assertThat(underTest.getProperties()).doesNotContainKey(PROPERTY_3);

    underTest.setProperty(PROPERTY_3, PROPERTY_VALUE_3);

    assertThat(underTest.getProperties()).contains(entry(PROPERTY_3, PROPERTY_VALUE_3));

    underTest.unload();

    assertThat(underTest.getProperties()).doesNotContainKey(PROPERTY_3);

    underTest.setProperty(PROPERTY_3, PROPERTY_VALUE_3);

    assertThat(underTest.getProperties()).contains(entry(PROPERTY_3, PROPERTY_VALUE_3));
  }

  @Test
  public void setProperties_sets_property_in_settings_specific_to_current_thread_if_any() {
    underTest.activateDatabaseSettings(Collections.<String, String>emptyMap());
    assertThat(underTest.getProperties()).doesNotContainKey(PROPERTY_3);

    underTest.load();

    assertThat(underTest.getProperties()).doesNotContainKey(PROPERTY_3);

    underTest.setProperties(ImmutableMap.of(PROPERTY_3, PROPERTY_VALUE_3));

    assertThat(underTest.getProperties()).contains(entry(PROPERTY_3, PROPERTY_VALUE_3));

    underTest.unload();

    assertThat(underTest.getProperties()).doesNotContainKey(PROPERTY_3);

    underTest.setProperties(ImmutableMap.of(PROPERTY_3, PROPERTY_VALUE_3));

    assertThat(underTest.getProperties()).contains(entry(PROPERTY_3, PROPERTY_VALUE_3));
  }

  @Test
  public void getString_gets_property_value_in_settings_specific_to_current_thread_if_any() {
    underTest.activateDatabaseSettings(Collections.<String, String>emptyMap());
    assertThat(underTest.getString(THREAD_PROPERTY)).isNull();

    rootProperties.setProperty(THREAD_PROPERTY, MAIN);
    underTest.load();

    assertThat(underTest.getString(THREAD_PROPERTY)).isEqualTo(MAIN);

    underTest.unload();

    assertThat(underTest.getString(THREAD_PROPERTY)).isNull();
  }

  @Test
  public void hasKey_checks_property_key_in_settings_specific_to_current_thread_if_any() {
    underTest.activateDatabaseSettings(Collections.<String, String>emptyMap());
    assertThat(underTest.hasKey(THREAD_PROPERTY)).isFalse();

    rootProperties.setProperty(THREAD_PROPERTY, MAIN);
    underTest.load();

    assertThat(underTest.hasKey(THREAD_PROPERTY)).isTrue();

    underTest.unload();

    assertThat(underTest.hasKey(THREAD_PROPERTY)).isFalse();
  }

  private abstract class CaptorThread extends Thread {
    private final boolean callLoad;
    private final CountDownLatch latch;

    public CaptorThread(boolean callLoad) {
      this.callLoad = callLoad;
      this.latch = new CountDownLatch(1);
    }

    public void blockingExecute() throws InterruptedException {
      this.start();
      this.latch.await(5, SECONDS);
    }

    @Override
    public void run() {
      if (callLoad) {
        try {
          rootProperties.setProperty(THREAD_PROPERTY, CAPTOR);
          underTest.load();
          doCapture();
          latch.countDown();
        } finally {
          underTest.unload();
        }
      } else {
        doCapture();
        latch.countDown();
      }
    }

    protected abstract void doCapture();
  }

  private class PropertiesCaptorThread extends CaptorThread {
    private Map<String, String> properties;

    public PropertiesCaptorThread(boolean callLoad) {
      super(callLoad);
    }

    protected void doCapture() {
      this.properties = underTest.getProperties();
    }
  }

  private class EncryptionCaptorThread extends CaptorThread {
    private Encryption encryption;

    public EncryptionCaptorThread(boolean callLoad) {
      super(callLoad);
    }

    protected void doCapture() {
      this.encryption = underTest.getEncryption();
    }
  }
}
