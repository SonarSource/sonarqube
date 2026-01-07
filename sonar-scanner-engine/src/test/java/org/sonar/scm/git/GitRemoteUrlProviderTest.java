/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.scm.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.repository.TelemetryCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.scm.git.GitUtils.createRepository;

class GitRemoteUrlProviderTest {

  @TempDir
  Path tempDir;

  @ParameterizedTest(name = "{0}")
  @MethodSource("sanitizeUrlTestCases")
  void sanitizeUrl(String testName, String input, String expected) {
    GitRemoteUrlProvider provider = createProvider();

    String sanitized = provider.sanitizeUrl(input);

    assertThat(sanitized).isEqualTo(expected);
  }

  private static Stream<Arguments> sanitizeUrlTestCases() {
    return Stream.of(
      Arguments.of("should remove credentials from HTTPS URL", "https://user:pass@github.com/org/repo.git", "https://github.com/org/repo.git"),
      Arguments.of("should remove credentials from HTTP URL", "http://user:pass@gitlab.com/org/repo.git", "http://gitlab.com/org/repo.git"),
      Arguments.of("should remove username from HTTPS URL", "https://user@github.com/org/repo.git", "https://github.com/org/repo.git"),
      Arguments.of("should keep HTTPS URL without credentials", "https://github.com/org/repo.git", "https://github.com/org/repo.git"),
      Arguments.of("should keep SSH URL as is", "git@github.com:org/repo.git", "git@github.com:org/repo.git"),
      Arguments.of("should remove credentials from SSH protocol URL", "ssh://git@github.com/org/repo.git", "ssh://github.com/org/repo.git"),
      Arguments.of("should keep git protocol URL as is", "git://github.com/org/repo.git", "git://github.com/org/repo.git"),
      Arguments.of("should return UNDETECTED as is", "UNDETECTED", "UNDETECTED"),
      Arguments.of("should handle invalid URI gracefully", "not a valid uri ://]", "not a valid uri ://]")
    );
  }

  @Test
  void start_shouldStoreOriginRemoteUrlInTelemetry() throws Exception {
    Git git = createRepository(tempDir);
    git.remoteAdd().setName("origin").setUri(new org.eclipse.jgit.transport.URIish("https://github.com/org/repo.git")).call();

    TelemetryCache telemetryCache = new TelemetryCache();
    InputModuleHierarchy moduleHierarchy = createModuleHierarchy(tempDir);
    GitRemoteUrlProvider provider = new GitRemoteUrlProvider(telemetryCache, moduleHierarchy);

    provider.start();

    assertThat(telemetryCache.getAll()).containsEntry("scanner.git_remote_url", "https://github.com/org/repo.git");
  }

  @Test
  void start_shouldSanitizeCredentialsFromRemoteUrl() throws Exception {
    Git git = createRepository(tempDir);
    git.remoteAdd().setName("origin").setUri(new org.eclipse.jgit.transport.URIish("https://user:pass@github.com/org/repo.git")).call();

    TelemetryCache telemetryCache = new TelemetryCache();
    InputModuleHierarchy moduleHierarchy = createModuleHierarchy(tempDir);
    GitRemoteUrlProvider provider = new GitRemoteUrlProvider(telemetryCache, moduleHierarchy);

    provider.start();

    assertThat(telemetryCache.getAll()).containsEntry("scanner.git_remote_url", "https://github.com/org/repo.git");
  }

  @Test
  void start_shouldStoreUndetectedWhenNotGitRepository() {
    TelemetryCache telemetryCache = new TelemetryCache();
    InputModuleHierarchy moduleHierarchy = createModuleHierarchy(tempDir);
    GitRemoteUrlProvider provider = new GitRemoteUrlProvider(telemetryCache, moduleHierarchy);

    provider.start();

    assertThat(telemetryCache.getAll()).containsEntry("scanner.git_remote_url", "UNDETECTED");
  }

  @Test
  void start_shouldStoreUndetectedWhenNoOriginRemote() throws IOException {
    createRepository(tempDir);

    TelemetryCache telemetryCache = new TelemetryCache();
    InputModuleHierarchy moduleHierarchy = createModuleHierarchy(tempDir);
    GitRemoteUrlProvider provider = new GitRemoteUrlProvider(telemetryCache, moduleHierarchy);

    provider.start();

    assertThat(telemetryCache.getAll()).containsEntry("scanner.git_remote_url", "UNDETECTED");
  }

  @Test
  void start_shouldStoreUndetectedWhenExceptionOccurs() {
    TelemetryCache telemetryCache = mock(TelemetryCache.class);
    InputModuleHierarchy moduleHierarchy = mock(InputModuleHierarchy.class);
    when(moduleHierarchy.root()).thenThrow(new RuntimeException("Test exception"));

    GitRemoteUrlProvider provider = new GitRemoteUrlProvider(telemetryCache, moduleHierarchy);

    provider.start();

    verify(telemetryCache).put("scanner.git_remote_url", "UNDETECTED");
  }

  private GitRemoteUrlProvider createProvider() {
    TelemetryCache telemetryCache = mock(TelemetryCache.class);
    InputModuleHierarchy moduleHierarchy = mock(InputModuleHierarchy.class);
    return new GitRemoteUrlProvider(telemetryCache, moduleHierarchy);
  }

  private InputModuleHierarchy createModuleHierarchy(Path baseDir) {
    DefaultInputModule module = mock(DefaultInputModule.class);
    when(module.getBaseDir()).thenReturn(baseDir);

    InputModuleHierarchy hierarchy = mock(InputModuleHierarchy.class);
    when(hierarchy.root()).thenReturn(module);
    return hierarchy;
  }
}
