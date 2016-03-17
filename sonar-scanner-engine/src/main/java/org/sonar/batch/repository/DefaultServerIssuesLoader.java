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
package org.sonar.batch.repository;

import com.google.common.base.Function;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.sonar.batch.cache.WSLoader;
import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.batch.util.BatchUtils;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;

public class DefaultServerIssuesLoader implements ServerIssuesLoader {

  private final WSLoader wsLoader;

  public DefaultServerIssuesLoader(WSLoader wsLoader) {
    this.wsLoader = wsLoader;
  }

  @Override
  public boolean load(String componentKey, Function<ServerIssue, Void> consumer) {
    WSLoaderResult<InputStream> result = wsLoader.loadStream("/batch/issues.protobuf?key=" + BatchUtils.encodeForUrl(componentKey));
    parseIssues(result.get(), consumer);
    return result.isFromCache();
  }

  private static void parseIssues(InputStream is, Function<ServerIssue, Void> consumer) {
    try {
      ServerIssue previousIssue = ServerIssue.parseDelimitedFrom(is);
      while (previousIssue != null) {
        consumer.apply(previousIssue);
        previousIssue = ServerIssue.parseDelimitedFrom(is);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to get previous issues", e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }
}
