/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.sources;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * Get details on source files. See also api/tests.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/sources">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class SourcesService extends BaseService {

  public SourcesService(WsConnector wsConnector) {
    super(wsConnector, "api/sources");
  }

  /**
   * Show line line hashes for a given file. Require See Source Code permission on file's project<br/>
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/sources/hash">Further information about this action online (including a response example)</a>
   * @since 5.0
   */
  public String hash(HashRequest request) {
    return call(
      new GetRequest(path("hash"))
        .setParam("key", request.getKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   * Get source code as line number / text pairs. Require See Source Code permission on file
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/sources/index">Further information about this action online (including a response example)</a>
   * @since 5.0
   */
  public String index(IndexRequest request) {
    return call(
      new GetRequest(path("index"))
        .setParam("from", request.getFrom())
        .setParam("resource", request.getResource())
        .setParam("to", request.getTo())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   * Show source code with line oriented info. Require See Source Code permission on file's project<br/>Each element of the result array is an object which contains:<ol><li>Line number</li><li>Content of the line</li><li>Author of the line (from SCM information)</li><li>Revision of the line (from SCM information)</li><li>Last commit date of the line (from SCM information)</li><li>Line hits from coverage</li><li>Number of conditions to cover in tests</li><li>Number of conditions covered by tests</li></ol>
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/sources/lines">Further information about this action online (including a response example)</a>
   * @since 5.0
   */
  public String lines(LinesRequest request) {
    return call(
      new GetRequest(path("lines"))
        .setParam("branch", request.getBranch())
        .setParam("from", request.getFrom())
        .setParam("key", request.getKey())
        .setParam("to", request.getTo())
        .setParam("uuid", request.getUuid())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   * Get source code as raw text. Require 'See Source Code' permission on file
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/sources/raw">Further information about this action online (including a response example)</a>
   * @since 5.0
   */
  public String raw(RawRequest request) {
    return call(
      new GetRequest(path("raw"))
        .setParam("branch", request.getBranch())
        .setParam("key", request.getKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   * Get SCM information of source files. Require See Source Code permission on file's project<br/>Each element of the result array is composed of:<ol><li>Line number</li><li>Author of the commit</li><li>Datetime of the commit (before 5.2 it was only the Date)</li><li>Revision of the commit (added in 5.2)</li></ol>
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/sources/scm">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public String scm(ScmRequest request) {
    return call(
      new GetRequest(path("scm"))
        .setParam("commits_by_line", request.getCommitsByLine())
        .setParam("from", request.getFrom())
        .setParam("key", request.getKey())
        .setParam("to", request.getTo())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   * Get source code. Require See Source Code permission on file's project<br/>Each element of the result array is composed of:<ol><li>Line number</li><li>Content of the line</li></ol>
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/sources/show">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public String show(ShowRequest request) {
    return call(
      new GetRequest(path("show"))
        .setParam("from", request.getFrom())
        .setParam("key", request.getKey())
        .setParam("to", request.getTo())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
