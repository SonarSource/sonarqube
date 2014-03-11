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

package org.sonar.server.permission;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.annotations.Nullable;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.util.RubyUtils;

import java.util.List;
import java.util.Map;

public class ApplyPermissionTemplateQuery {

  private static final String TEMPLATE_KEY = "template_key";
  private static final String COMPONENTS_KEY = "components";

  private final String templateKey;
  private List<String> selectedComponents;

  private ApplyPermissionTemplateQuery(@Nullable String templateKey) {
    this.templateKey = templateKey;
  }

  public static ApplyPermissionTemplateQuery buildFromParams(Map<String, Object> params) {
    ApplyPermissionTemplateQuery query = new ApplyPermissionTemplateQuery((String)params.get(TEMPLATE_KEY));
    query.setSelectedComponents(RubyUtils.toStrings(params.get(COMPONENTS_KEY)));
    return query;
  }

  public String getTemplateKey() {
    return templateKey;
  }

  public List<String> getSelectedComponents() {
    return selectedComponents;
  }

  public void validate() {
    if(StringUtils.isBlank(templateKey)) {
      throw new BadRequestException("Permission template is mandatory");
    }
    if(selectedComponents == null || selectedComponents.isEmpty()) {
      throw new BadRequestException("Please provide at least one entry to which the permission template should be applied");
    }
  }

  private void setSelectedComponents(List<String> selectedComponents) {
    this.selectedComponents = selectedComponents;
  }
}
