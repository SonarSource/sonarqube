/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.almsettings.ws;

import java.util.Optional;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.server.almsettings.ws.GithubManifestStateStore.PendingManifest;

import static org.assertj.core.api.Assertions.assertThat;

public class GithubManifestStateStoreTest {

  private final TestSystem2 system2 = new TestSystem2().setNow(1_000_000L);
  private final GithubManifestStateStore underTest = new GithubManifestStateStore(system2);

  @Test
  public void create_then_consume_returns_pending_manifest() {
    String state = underTest.create("my-key", "my-org", "user-uuid", true, false);

    Optional<PendingManifest> consumed = underTest.consume(state);

    assertThat(consumed).isPresent();
    PendingManifest pending = consumed.get();
    assertThat(pending.settingKey()).isEqualTo("my-key");
    assertThat(pending.organization()).isEqualTo("my-org");
    assertThat(pending.userUuid()).isEqualTo("user-uuid");
    assertThat(pending.setupDevops()).isTrue();
    assertThat(pending.setupAuth()).isFalse();
  }

  @Test
  public void consume_is_single_use() {
    String state = underTest.create("my-key", null, "user-uuid", true, false);

    assertThat(underTest.consume(state)).isPresent();
    assertThat(underTest.consume(state)).isEmpty();
  }

  @Test
  public void consume_unknownState_returnsEmpty() {
    assertThat(underTest.consume("does-not-exist")).isEmpty();
  }

  @Test
  public void consume_expiredState_returnsEmpty() {
    String state = underTest.create("my-key", null, "user-uuid", true, false);

    system2.setNow(system2.now() + GithubManifestStateStore.TTL_MS + 1);

    assertThat(underTest.consume(state)).isEmpty();
  }
}
