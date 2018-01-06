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
package org.sonar.server.setting;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import javax.annotation.Nullable;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.PropertyDefinitions;

import static java.util.Collections.unmodifiableMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class ThreadLocalSettingsTest {

  private static final String A_KEY = "a_key";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MapSettingLoader dbSettingLoader = new MapSettingLoader();
  private ThreadLocalSettings underTest = null;

  @After
  public void tearDown() {
    if (underTest != null) {
      underTest.unload();
    }
  }

  @Test
  public void can_not_add_property_if_no_cache() {
    underTest = create(Collections.emptyMap());

    underTest.set("foo", "wiz");

    assertThat(underTest.get("foo")).isNotPresent();
  }

  @Test
  public void can_not_remove_system_property_if_no_cache() {
    underTest = create(ImmutableMap.of("foo", "bar"));

    underTest.remove("foo");

    assertThat(underTest.get("foo").get()).isEqualTo("bar");
  }

  @Test
  public void add_property_to_cache() {
    underTest = create(Collections.emptyMap());

    underTest.load();
    underTest.set("foo", "bar");
    assertThat(underTest.get("foo").get()).isEqualTo("bar");
    underTest.unload();

    // no more cache
    assertThat(underTest.get("foo")).isNotPresent();
  }

  @Test
  public void remove_property_from_cache() {
    underTest = create(Collections.emptyMap());

    underTest.load();
    underTest.set("foo", "bar");
    assertThat(underTest.get("foo").get()).isEqualTo("bar");
    underTest.remove("foo");
    assertThat(underTest.get("foo")).isNotPresent();
    underTest.unload();

    // no more cache
    assertThat(underTest.get("foo")).isNotPresent();
  }

  /**
   * SONAR-8216 System info page fails when a setting is defined both in sonar.properties and in DB
   */
  @Test
  public void getProperties_does_not_fail_on_duplicated_key() {
    insertPropertyIntoDb("foo", "from_db");
    underTest = create(ImmutableMap.of("foo", "from_system"));

    assertThat(underTest.get("foo")).hasValue("from_system");
    assertThat(underTest.getProperties().get("foo")).isEqualTo("from_system");
  }

  @Test
  public void load_encryption_secret_key_from_system_properties() throws Exception {
    File secretKey = temp.newFile();

    underTest = create(ImmutableMap.of("foo", "bar", "sonar.secretKeyPath", secretKey.getAbsolutePath()));

    assertThat(underTest.getEncryption().hasSecretKey()).isTrue();
  }

  @Test
  public void encryption_secret_key_is_undefined_by_default() {
    underTest = create(ImmutableMap.of("foo", "bar", "sonar.secretKeyPath", "unknown/path/to/sonar-secret.txt"));

    assertThat(underTest.getEncryption().hasSecretKey()).isFalse();
  }

  private ThreadLocalSettings create(Map<String, String> systemProps) {
    Properties p = new Properties();
    p.putAll(systemProps);
    return new ThreadLocalSettings(new PropertyDefinitions(), p, dbSettingLoader);
  }

  @Test
  public void load_system_properties() {
    underTest = create(ImmutableMap.of("foo", "1", "bar", "2"));

    assertThat(underTest.get("foo").get()).isEqualTo("1");
    assertThat(underTest.get("missing")).isNotPresent();
    assertThat(underTest.getProperties()).containsOnly(entry("foo", "1"), entry("bar", "2"));
  }

  @Test
  public void database_properties_are_not_cached_by_default() {
    insertPropertyIntoDb("foo", "from db");
    underTest = create(Collections.emptyMap());

    assertThat(underTest.get("foo").get()).isEqualTo("from db");

    deletePropertyFromDb("foo");
    // no cache, change is visible immediately
    assertThat(underTest.get("foo")).isNotPresent();
  }

  @Test
  public void system_settings_have_precedence_over_database() {
    insertPropertyIntoDb("foo", "from db");
    underTest = create(ImmutableMap.of("foo", "from system"));

    assertThat(underTest.get("foo").get()).isEqualTo("from system");
  }

  @Test
  public void getProperties_are_all_properties_with_value() {
    insertPropertyIntoDb("db", "from db");
    insertPropertyIntoDb("empty", "");
    underTest = create(ImmutableMap.of("system", "from system"));

    assertThat(underTest.getProperties()).containsOnly(entry("system", "from system"), entry("db", "from db"), entry("empty", ""));
  }

  @Test
  public void getProperties_is_not_cached_in_thread_cache() {
    insertPropertyIntoDb("foo", "bar");
    underTest = create(Collections.emptyMap());
    underTest.load();

    assertThat(underTest.getProperties())
      .containsOnly(entry("foo", "bar"));

    insertPropertyIntoDb("foo2", "bar2");
    assertThat(underTest.getProperties())
      .containsOnly(entry("foo", "bar"), entry("foo2", "bar2"));

    underTest.unload();

    assertThat(underTest.getProperties())
      .containsOnly(entry("foo", "bar"), entry("foo2", "bar2"));
  }

  @Test
  public void load_creates_a_thread_specific_cache() throws InterruptedException {
    insertPropertyIntoDb(A_KEY, "v1");

    underTest = create(Collections.emptyMap());
    underTest.load();

    assertThat(underTest.get(A_KEY).get()).isEqualTo("v1");

    deletePropertyFromDb(A_KEY);
    // the main thread still has "v1" in cache, but not new thread
    assertThat(underTest.get(A_KEY).get()).isEqualTo("v1");
    verifyValueInNewThread(underTest, null);

    insertPropertyIntoDb(A_KEY, "v2");
    // the main thread still has the old value "v1" in cache, but new thread loads "v2"
    assertThat(underTest.get(A_KEY).get()).isEqualTo("v1");
    verifyValueInNewThread(underTest, "v2");

    underTest.unload();
  }

  @Test
  public void load_throws_ISE_if_load_called_twice_without_unload_in_between() {
    underTest = create(Collections.emptyMap());
    underTest.load();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("load called twice for thread '" + Thread.currentThread().getName()
      + "' or state wasn't cleared last time it was used");

    underTest.load();
  }

  @Test
  public void keep_in_thread_cache_the_fact_that_a_property_is_not_in_db() {
    underTest = create(Collections.emptyMap());
    underTest.load();
    assertThat(underTest.get(A_KEY)).isNotPresent();

    insertPropertyIntoDb(A_KEY, "bar");
    // do not execute new SQL request, cache contains the information of missing property
    assertThat(underTest.get(A_KEY)).isNotPresent();

    underTest.unload();
  }

  @Test
  public void change_setting_loader() {
    underTest = new ThreadLocalSettings(new PropertyDefinitions(), new Properties());

    assertThat(underTest.getSettingLoader()).isNotNull();

    SettingLoader newLoader = mock(SettingLoader.class);
    underTest.setSettingLoader(newLoader);
    assertThat(underTest.getSettingLoader()).isSameAs(newLoader);
  }

  @Test
  public void cache_db_calls_if_property_is_not_persisted() {
    underTest = create(Collections.emptyMap());
    underTest.load();
    assertThat(underTest.get(A_KEY)).isNotPresent();
    assertThat(underTest.get(A_KEY)).isNotPresent();
    underTest.unload();
  }

  @Test
  public void getProperties_return_empty_if_DB_error_on_first_call_ever_out_of_thread_cache() {
    SettingLoader settingLoaderMock = mock(SettingLoader.class);
    PersistenceException toBeThrown = new PersistenceException("Faking an error connecting to DB");
    doThrow(toBeThrown).when(settingLoaderMock).loadAll();
    underTest = new ThreadLocalSettings(new PropertyDefinitions(), new Properties(), settingLoaderMock);

    assertThat(underTest.getProperties())
      .isEmpty();
  }

  @Test
  public void getProperties_returns_empty_if_DB_error_on_first_call_ever_in_thread_cache() {
    SettingLoader settingLoaderMock = mock(SettingLoader.class);
    PersistenceException toBeThrown = new PersistenceException("Faking an error connecting to DB");
    doThrow(toBeThrown).when(settingLoaderMock).loadAll();
    underTest = new ThreadLocalSettings(new PropertyDefinitions(), new Properties(), settingLoaderMock);
    underTest.load();

    assertThat(underTest.getProperties())
      .isEmpty();
  }

  @Test
  public void getProperties_return_properties_from_previous_thread_cache_if_DB_error_on_not_first_call() {
    String key = randomAlphanumeric(3);
    String value1 = randomAlphanumeric(4);
    String value2 = randomAlphanumeric(5);
    SettingLoader settingLoaderMock = mock(SettingLoader.class);
    PersistenceException toBeThrown = new PersistenceException("Faking an error connecting to DB");
    doAnswer(invocationOnMock -> ImmutableMap.of(key, value1))
      .doThrow(toBeThrown)
      .doAnswer(invocationOnMock -> ImmutableMap.of(key, value2))
      .when(settingLoaderMock)
      .loadAll();
    underTest = new ThreadLocalSettings(new PropertyDefinitions(), new Properties(), settingLoaderMock);

    underTest.load();
    assertThat(underTest.getProperties())
      .containsOnly(entry(key, value1));
    underTest.unload();

    underTest.load();
    assertThat(underTest.getProperties())
      .containsOnly(entry(key, value1));
    underTest.unload();

    underTest.load();
    assertThat(underTest.getProperties())
      .containsOnly(entry(key, value2));
    underTest.unload();
  }

  @Test
  public void get_returns_empty_if_DB_error_on_first_call_ever_out_of_thread_cache() {
    SettingLoader settingLoaderMock = mock(SettingLoader.class);
    PersistenceException toBeThrown = new PersistenceException("Faking an error connecting to DB");
    String key = randomAlphanumeric(3);
    doThrow(toBeThrown).when(settingLoaderMock).load(key);
    underTest = new ThreadLocalSettings(new PropertyDefinitions(), new Properties(), settingLoaderMock);

    assertThat(underTest.get(key)).isEmpty();
  }

  @Test
  public void get_returns_empty_if_DB_error_on_first_call_ever_in_thread_cache() {
    SettingLoader settingLoaderMock = mock(SettingLoader.class);
    PersistenceException toBeThrown = new PersistenceException("Faking an error connecting to DB");
    String key = randomAlphanumeric(3);
    doThrow(toBeThrown).when(settingLoaderMock).load(key);
    underTest = new ThreadLocalSettings(new PropertyDefinitions(), new Properties(), settingLoaderMock);
    underTest.load();

    assertThat(underTest.get(key)).isEmpty();
  }

  private void insertPropertyIntoDb(String key, @Nullable String value) {
    dbSettingLoader.put(key, value);
  }

  private void deletePropertyFromDb(String key) {
    dbSettingLoader.remove(key);
  }

  private void verifyValueInNewThread(ThreadLocalSettings settings, @Nullable String expectedValue) throws InterruptedException {
    CacheCaptorThread captor = new CacheCaptorThread();
    captor.verifyValue(settings, expectedValue);
  }

  private class CacheCaptorThread extends Thread {
    private final CountDownLatch latch = new CountDownLatch(1);
    private ThreadLocalSettings settings;
    private String value;

    void verifyValue(ThreadLocalSettings settings, @Nullable String expectedValue) throws InterruptedException {
      this.settings = settings;
      this.start();
      this.latch.await(5, SECONDS);
      assertThat(value).isEqualTo(expectedValue);
    }

    @Override
    public void run() {
      try {
        settings.load();
        value = settings.get(A_KEY).orElse(null);
        latch.countDown();
      } finally {
        settings.unload();
      }
    }
  }

  private static class MapSettingLoader implements SettingLoader {
    private final Map<String, String> map = new HashMap<>();

    public MapSettingLoader put(String key, String value) {
      map.put(key, value);
      return this;
    }

    public MapSettingLoader remove(String key) {
      map.remove(key);
      return this;
    }

    @Override
    public String load(String key) {
      return map.get(key);
    }

    @Override
    public Map<String, String> loadAll() {
      return unmodifiableMap(map);
    }
  }
}
