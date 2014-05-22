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

package org.sonar.wsclient.duplication.internal;

import org.json.simple.JSONValue;
import org.sonar.wsclient.duplication.*;
import org.sonar.wsclient.internal.EncodingUtils;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.unmarshallers.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultDuplicationClient implements DuplicationClient {

  private final HttpRequestFactory requestFactory;

  public DefaultDuplicationClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  @Override
  public Duplications show(String componentKey) {
    Map<String, Object> params = EncodingUtils.toMap("key", componentKey);
    String jsonResult = requestFactory.get("/api/duplications/show", params);

    DefaultDuplications duplications = new DefaultDuplications();
    Map jRoot = (Map) JSONValue.parse(jsonResult);
    parseDuplications(duplications, jRoot);
    parseFiles(duplications, jRoot);
    return duplications;
  }

  private void parseDuplications(DefaultDuplications duplications, Map jRoot) {
    List<Map> jsonDuplications = (List<Map>) jRoot.get("duplications");
    if (jsonDuplications != null) {
      for (Map jsonDuplication : jsonDuplications) {
        final List<Block> blockList = new ArrayList<Block>();

        List<Map> blocks = (List<Map>) jsonDuplication.get("blocks");
        if (blocks != null) {
          for (final Map block : blocks) {

            blockList.add((new Block() {
                @Override
                public String fileRef() {
                return JsonUtils.getString(block, "_ref");
              }

                @Override
                public Integer from() {
                return JsonUtils.getInteger(block, "from");
              }

                @Override
                public Integer size() {
                return JsonUtils.getInteger(block, "size");
              }
            }));
          }
        }

        Duplication duplication = new Duplication() {
          @Override
          public List<Block> blocks() {
            return blockList;
          }
        };

        duplications.addDuplication(duplication);
      }
    }
  }

  private void parseFiles(DefaultDuplications duplications, Map jRoot) {
    Map<String, Map> jsonFiles = (Map) jRoot.get("files");
    if (jsonFiles != null) {
      for (Map.Entry<String, Map> entry : jsonFiles.entrySet()) {
        String ref = entry.getKey();
        final Map file = entry.getValue();
        if (ref != null && file != null) {

          duplications.addFile(ref, new File() {
            @Override
            public String key () {
              return JsonUtils.getString(file, "key");
            }

            @Override
            public String name () {
              return JsonUtils.getString(file, "name");
            }

            @Override
            public String projectName () {
              return JsonUtils.getString(file, "projectName");
            }
          });

        }
      }
    }
  }
}
