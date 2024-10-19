/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.ws;

import java.net.HttpURLConnection;
import java.util.Date;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.util.Locale.ENGLISH;
import static java.util.Locale.PRC;
import static java.util.Locale.UK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class IndexActionTest {
  private static final String KEY_1 = "key1";
  private static final String KEY_2 = "key2";
  private static final String KEY_3 = "key3";

  private final DefaultI18n i18n = mock(DefaultI18n.class);
  private final Server server = mock(Server.class);

  private final IndexAction underTest = new IndexAction(i18n, server);

  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void execute_shouldAllowClientToCacheMessages() {
    Date now = new Date();
    Date aBitLater = new Date(now.getTime() + 1000);
    when(server.getStartedAt()).thenReturn(now);

    TestResponse result = call(null, DateUtils.formatDateTime(aBitLater));

    verifyNoInteractions(i18n);
    verify(server).getStartedAt();
    assertThat(result.getStatus()).isEqualTo(HttpURLConnection.HTTP_NOT_MODIFIED);
  }

  @Test
  public void execute_shouldReturnAllL10nMessages_whenUsingAcceptHeaderWithCacheExpired() {
    Date now = new Date();
    Date aBitEarlier = new Date(now.getTime() - 1000);
    when(server.getStartedAt()).thenReturn(now);
    when(i18n.getPropertyKeys()).thenReturn(Set.of(KEY_1, KEY_2, KEY_3));
    when(i18n.message(PRC, KEY_1, KEY_1)).thenReturn(KEY_1);
    when(i18n.message(PRC, KEY_2, KEY_2)).thenReturn(KEY_2);
    when(i18n.message(PRC, KEY_3, KEY_3)).thenReturn(KEY_3);
    when(i18n.getEffectiveLocale(PRC)).thenReturn(PRC);

    TestResponse result = call(PRC.toLanguageTag(), DateUtils.formatDateTime(aBitEarlier));

    verify(i18n).getPropertyKeys();
    verify(i18n).message(PRC, KEY_1, KEY_1);
    verify(i18n).message(PRC, KEY_2, KEY_2);
    verify(i18n).message(PRC, KEY_3, KEY_3);
    assertJson(result.getInput()).isSimilarTo("{\"effectiveLocale\":\"zh-CN\", \"messages\": {\"key1\":\"key1\",\"key2\":\"key2\",\"key3\":\"key3\"}}");
  }

  @Test
  public void execute_shouldReturnEnglishMessages_whenDefaultLocaleProvided() {
    String key1 = "key1";
    String key2 = "key2";
    String key3 = "key3";
    when(i18n.getPropertyKeys()).thenReturn(Set.of(key1, key2, key3));
    when(i18n.message(ENGLISH, key1, key1)).thenReturn(key1);
    when(i18n.message(ENGLISH, key2, key2)).thenReturn(key2);
    when(i18n.message(ENGLISH, key3, key3)).thenReturn(key3);
    when(i18n.getEffectiveLocale(ENGLISH)).thenReturn(ENGLISH);

    TestResponse result = call(null, null);

    verify(i18n).getPropertyKeys();
    verify(i18n).message(ENGLISH, key1, key1);
    verify(i18n).message(ENGLISH, key2, key2);
    verify(i18n).message(ENGLISH, key3, key3);
    assertJson(result.getInput()).isSimilarTo("{\"messages\": {\"key1\":\"key1\",\"key2\":\"key2\",\"key3\":\"key3\"}}");
  }

  @Test
  public void execute_shouldReturnMessages_whenProvidedSupportedBCP47FormattedLanguageTags() {
    String key1 = "key1";
    when(i18n.getPropertyKeys()).thenReturn(Set.of(key1));
    when(i18n.message(UK, key1, key1)).thenReturn(key1);
    when(i18n.getEffectiveLocale(UK)).thenReturn(UK);

    TestResponse result = call("en-GB", null);

    verify(i18n).getPropertyKeys();
    verify(i18n).message(UK, key1, key1);
    assertJson(result.getInput()).isSimilarTo("{\"messages\": {\"key1\":\"key1\"}}");
  }

  @Test
  public void execute_shouldFail_whenJavaFormattedLanguageTags() {
    String key1 = "key1";
    when(i18n.getPropertyKeys()).thenReturn(Set.of(key1));
    when(i18n.message(UK, key1, key1)).thenReturn(key1);
    when(i18n.getEffectiveLocale(UK)).thenReturn(UK);

    assertThatThrownBy(() -> call("en_GB", null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Locale cannot be parsed as a BCP47 language tag");
  }

  @Test
  public void execute_shouldFail_whenUnknownBCP47Tag() {
    String key1 = "key1";
    when(i18n.getPropertyKeys()).thenReturn(Set.of(key1));
    when(i18n.message(UK, key1, key1)).thenReturn(key1);
    when(i18n.getEffectiveLocale(UK)).thenReturn(UK);

    assertThatThrownBy(() -> call("ABCD", null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Locale cannot be parsed as a BCP47 language tag");
  }

  private TestResponse call(@Nullable String locale, @Nullable String timestamp) {
    TestRequest request = ws.newRequest();
    if (locale != null) {
      request.setParam("locale", locale);
    }
    if (timestamp != null) {
      request.setParam("ts", timestamp);
    }
    return request.execute();
  }

}
