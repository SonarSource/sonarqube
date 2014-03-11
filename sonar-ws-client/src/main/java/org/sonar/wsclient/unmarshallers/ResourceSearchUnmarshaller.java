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
package org.sonar.wsclient.unmarshallers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sonar.wsclient.services.ResourceSearchResult;
import org.sonar.wsclient.services.WSUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 3.4
 */
public class ResourceSearchUnmarshaller extends AbstractUnmarshaller<ResourceSearchResult> {

  @Override
  protected ResourceSearchResult parse(Object json) {
    WSUtils utils = WSUtils.getINSTANCE();
    ResourceSearchResult result = new ResourceSearchResult();
    result.setPage(utils.getInteger(json, "page"));
    result.setPageSize(utils.getInteger(json, "page_size"));
    result.setTotal(utils.getInteger(json, "total"));

    List<ResourceSearchResult.Resource> resources = new ArrayList<ResourceSearchResult.Resource>();
    JSONArray dataJson = JsonUtils.getArray((JSONObject) json, "data");
    if (dataJson != null) {
      for (Object jsonResource : dataJson) {
        ResourceSearchResult.Resource resource = new ResourceSearchResult.Resource();
        resource.setKey(JsonUtils.getString((JSONObject) jsonResource, "key"));
        resource.setName(JsonUtils.getString((JSONObject) jsonResource, "nm"));
        resource.setQualifier(JsonUtils.getString((JSONObject) jsonResource, "q"));
        resources.add(resource);
      }
    }
    result.setResources(resources);
    return result;
  }


}
