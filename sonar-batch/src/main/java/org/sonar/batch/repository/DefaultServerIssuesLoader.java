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
package org.sonar.batch.repository;

import org.sonar.batch.cache.WSLoaderResult;

import org.sonar.batch.cache.WSLoader;
import org.sonar.batch.util.BatchUtils;
import com.google.common.io.ByteSource;
import com.google.common.base.Function;
import org.sonar.batch.protocol.input.BatchInput.ServerIssue;

import java.io.IOException;
import java.io.InputStream;

public class DefaultServerIssuesLoader implements ServerIssuesLoader {

  private final WSLoader wsLoader;

  public DefaultServerIssuesLoader(WSLoader wsLoader) {
    this.wsLoader = wsLoader;
  }

  @Override
  public boolean load(String componentKey, Function<ServerIssue, Void> consumer) {
    WSLoaderResult<ByteSource> result = wsLoader.loadSource("/scanner/issues?key=" + BatchUtils.encodeForUrl(componentKey));
    parseIssues(result.get(), consumer);
    return result.isFromCache();
  }

  private static void parseIssues(ByteSource input, Function<ServerIssue, Void> consumer) {
    try (InputStream is = input.openBufferedStream()) {
      ServerIssue previousIssue = ServerIssue.parseDelimitedFrom(is);
      while (previousIssue != null) {
        consumer.apply(previousIssue);
        previousIssue = ServerIssue.parseDelimitedFrom(is);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to get previous issues", e);
    }
  }
}
