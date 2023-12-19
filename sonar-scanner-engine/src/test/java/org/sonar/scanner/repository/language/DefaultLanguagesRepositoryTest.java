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
package org.sonar.scanner.repository.language;

import java.io.InputStreamReader;
import java.io.StringReader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.config.Configuration;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.bootstrap.DefaultScannerWsClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultLanguagesRepositoryTest {

  @Rule
  public LogTester logTester = new LogTester();

  private final DefaultScannerWsClient wsClient = mock(DefaultScannerWsClient.class);
  private final Configuration properties = mock(Configuration.class);
  private final LanguagesLoader languagesLoader = new DefaultLanguagesLoader(wsClient, properties);


  private final DefaultLanguagesRepository underTest = new DefaultLanguagesRepository(languagesLoader);

  private static final String[] JAVA_SUFFIXES = new String[] { ".java", ".jav" };
  private static final String[] XOO_SUFFIXES = new String[] { ".xoo" };
  private static final String[] XOO_PATTERNS = new String[] { "Xoofile" };
  private static final String[] PYTHON_SUFFIXES = new String[] { ".py" };

  @Before
  public void setup() {
    logTester.setLevel(Level.DEBUG);
  }

  @Test
  public void should_load_languages_from_ws() {
    WsTestUtil.mockReader(wsClient, "/api/languages/list",
      new InputStreamReader(getClass().getResourceAsStream("DefaultLanguageRepositoryTest/languages-ws.json")));

    when(properties.getStringArray("sonar.java.file.suffixes")).thenReturn(JAVA_SUFFIXES);
    when(properties.getStringArray("sonar.xoo.file.suffixes")).thenReturn(XOO_SUFFIXES);
    when(properties.getStringArray("sonar.python.file.suffixes")).thenReturn(PYTHON_SUFFIXES);

    underTest.start();

    assertThat(underTest.all()).hasSize(3);
    assertThat(underTest.get("java")).isNotNull();
    assertThat(underTest.get("java").fileSuffixes()).containsExactlyInAnyOrder(JAVA_SUFFIXES);
    assertThat(underTest.get("java").isPublishAllFiles()).isTrue();
    assertThat(underTest.get("xoo")).isNotNull();
    assertThat(underTest.get("xoo").fileSuffixes()).containsExactlyInAnyOrder(XOO_SUFFIXES);
    assertThat(underTest.get("xoo").isPublishAllFiles()).isTrue();
    assertThat(underTest.get("python")).isNotNull();
    assertThat(underTest.get("python").fileSuffixes()).containsExactlyInAnyOrder(PYTHON_SUFFIXES);
    assertThat(underTest.get("python").isPublishAllFiles()).isTrue();
  }

  @Test
  public void should_throw_error_on_invalid_ws_response() {
    WsTestUtil.mockReader(wsClient, "api/languages/list", new StringReader("not json"));

    IllegalStateException e = catchThrowableOfType(underTest::start, IllegalStateException.class);

    assertThat(e).hasMessage("Fail to parse response of /api/languages/list");
  }

  @Test
  public void should_return_null_when_language_not_found() {
    WsTestUtil.mockReader(wsClient, "/api/languages/list",
      new InputStreamReader(getClass().getResourceAsStream("DefaultLanguageRepositoryTest/languages-ws.json")));

    when(properties.getStringArray("sonar.java.file.suffixes")).thenReturn(JAVA_SUFFIXES);
    when(properties.getStringArray("sonar.xoo.file.suffixes")).thenReturn(XOO_SUFFIXES);
    when(properties.getStringArray("sonar.python.file.suffixes")).thenReturn(PYTHON_SUFFIXES);

    assertThat(underTest.get("k1")).isNull();
  }

  @Test
  public void publishAllFiles_by_default() {
    WsTestUtil.mockReader(wsClient, "/api/languages/list",
      new InputStreamReader(getClass().getResourceAsStream("DefaultLanguageRepositoryTest/languages-ws.json")));

    underTest.start();

    assertThat(underTest.get("java").isPublishAllFiles()).isTrue();
    assertThat(underTest.get("xoo").isPublishAllFiles()).isTrue();
    assertThat(underTest.get("python").isPublishAllFiles()).isTrue();
  }

  @Test
  public void get_find_language_by_key() {
    WsTestUtil.mockReader(wsClient, "/api/languages/list",
      new InputStreamReader(getClass().getResourceAsStream("DefaultLanguageRepositoryTest/languages-ws.json")));

    when(properties.getStringArray("sonar.java.file.suffixes")).thenReturn(JAVA_SUFFIXES);

    underTest.start();

    assertThat(underTest.get("java"))
      .extracting("key", "name", "fileSuffixes", "publishAllFiles")
      .containsOnly("java", "Java", JAVA_SUFFIXES, true);
  }


  @Test
  public void should_log_if_language_has_no_suffixes_or_patterns() {
    WsTestUtil.mockReader(wsClient, "/api/languages/list",
      new InputStreamReader(getClass().getResourceAsStream("DefaultLanguageRepositoryTest/languages-ws.json")));

    when(properties.getStringArray("sonar.java.file.suffixes")).thenReturn(JAVA_SUFFIXES);
    when(properties.getStringArray("sonar.xoo.file.patterns")).thenReturn(XOO_PATTERNS);

    underTest.start();

    assertThat(logTester.logs(Level.DEBUG)).contains("Language 'Python' cannot be detected as it has neither suffixes nor patterns.");
  }

}
