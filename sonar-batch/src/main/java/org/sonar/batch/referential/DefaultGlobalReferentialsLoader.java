/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.referential;

import com.google.common.base.Charsets;
import com.google.common.io.InputSupplier;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.protocol.input.GlobalReferentials;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DefaultGlobalReferentialsLoader implements GlobalReferentialsLoader {

  private static final String BATCH_GLOBAL_URL = "/batch/global";

  private final ServerClient serverClient;

  public DefaultGlobalReferentialsLoader(ServerClient serverClient) {
    this.serverClient = serverClient;
  }

  @Override
  public GlobalReferentials load() {
    InputSupplier<InputStream> jsonStream = serverClient.doRequest(BATCH_GLOBAL_URL, null);
    try {
      return GlobalReferentials.fromJson(new InputStreamReader(jsonStream.getInput(), Charsets.UTF_8));
    } catch (IOException e) {
      throw new IllegalStateException("Unable to load global referentials", e);
    }
  }

}
