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
package org.sonar.scanner.repository.featureflags;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sonar.scanner.http.ScannerWsClient;
import org.sonarqube.ws.client.GetRequest;

public class DefaultFeatureFlagsLoader implements FeatureFlagsLoader {

  private static final String FEATURE_FLAGS_WS_URL = "/api/features/list";

  private final ScannerWsClient wsClient;

  public DefaultFeatureFlagsLoader(ScannerWsClient wsClient) {
    this.wsClient = wsClient;
  }

  @Override
  public Set<String> load() {
    GetRequest getRequest = new GetRequest(FEATURE_FLAGS_WS_URL);
    List<String> jsonResponse;
    try (Reader reader = wsClient.call(getRequest).contentReader()) {
      jsonResponse = new Gson().fromJson(reader, new TypeToken<ArrayList<String>>() {
      }.getType());
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load feature flags", e);
    }
    return Set.copyOf(jsonResponse);
  }

}
