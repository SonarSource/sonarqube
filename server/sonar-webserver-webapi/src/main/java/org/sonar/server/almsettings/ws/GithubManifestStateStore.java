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

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;

/**
 * Holds the short-lived, single-use {@code state} tokens generated when an administrator starts the
 * GitHub App Manifest flow, together with the configuration they intend to create. The token is
 * validated when GitHub redirects back to SonarQube (see {@code GithubManifestCallbackFilter}).
 * <p>
 * The store is in-memory: the manifest flow is expected to complete within minutes and on the same
 * web node. In a clustered deployment the redirect must land on the node that started the flow
 * (sticky sessions). GitHub also enforces a one-hour limit and the temporary code is single-use, so
 * a lost state simply requires restarting the flow.
 */
@ServerSide
public class GithubManifestStateStore {

  static final long TTL_MS = Duration.ofHours(1).toMillis();

  private final System2 system2;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Map<String, PendingManifest> pendingByState = new ConcurrentHashMap<>();

  public GithubManifestStateStore(System2 system2) {
    this.system2 = system2;
  }

  public String create(@Nullable String settingKey, @Nullable String organization, String userUuid, boolean setupDevops, boolean setupAuth) {
    purgeExpired();
    String state = new BigInteger(160, secureRandom).toString(32);
    pendingByState.put(state, new PendingManifest(settingKey, organization, userUuid, setupDevops, setupAuth, system2.now() + TTL_MS));
    return state;
  }

  /**
   * Returns and removes the pending manifest for the given state, or empty if the state is unknown or
   * expired. A state can only be consumed once.
   */
  public Optional<PendingManifest> consume(String state) {
    purgeExpired();
    PendingManifest pending = pendingByState.remove(state);
    if (pending == null || pending.expiresAtMs() < system2.now()) {
      return Optional.empty();
    }
    return Optional.of(pending);
  }

  private void purgeExpired() {
    long now = system2.now();
    pendingByState.entrySet().removeIf(entry -> entry.getValue().expiresAtMs() < now);
  }

  public record PendingManifest(@Nullable String settingKey, @Nullable String organization, String userUuid,
    boolean setupDevops, boolean setupAuth, long expiresAtMs) {
  }
}
