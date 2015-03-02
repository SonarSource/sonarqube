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

package org.sonar.core.component;

import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentDtoTest {

  @Test
  public void setters_and_getters() throws Exception {
    ComponentDto componentDto = new ComponentDto()
      .setId(1L)
      .setKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setDeprecatedKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setName("RequestContext.java")
      .setLongName("org.struts.RequestContext")
      .setQualifier("FIL")
      .setScope("FIL")
      .setLanguage("java")
      .setPath("src/org/struts/RequestContext.java")
      .setCopyResourceId(5L)
      .setParentProjectId(3L)
      .setAuthorizationUpdatedAt(123456789L);

    assertThat(componentDto.getId()).isEqualTo(1L);
    assertThat(componentDto.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(componentDto.deprecatedKey()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(componentDto.name()).isEqualTo("RequestContext.java");
    assertThat(componentDto.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(componentDto.qualifier()).isEqualTo("FIL");
    assertThat(componentDto.scope()).isEqualTo("FIL");
    assertThat(componentDto.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(componentDto.language()).isEqualTo("java");
    assertThat(componentDto.parentProjectId()).isEqualTo(3L);
    assertThat(componentDto.getCopyResourceId()).isEqualTo(5L);
    assertThat(componentDto.getAuthorizationUpdatedAt()).isEqualTo(123456789L);
  }

  @Test
  public void equals_and_hashcode() throws Exception {
    ComponentDto dto = new ComponentDto().setId(1L);
    ComponentDto dtoWithSameId = new ComponentDto().setId(1L);
    ComponentDto dtoWithDifferentId = new ComponentDto().setId(2L);

    assertThat(dto).isEqualTo(dto);
    assertThat(dto).isEqualTo(dtoWithSameId);
    assertThat(dto).isNotEqualTo(dtoWithDifferentId);

    assertThat(dto.hashCode()).isEqualTo(dto.hashCode());
    assertThat(dto.hashCode()).isEqualTo(dtoWithSameId.hashCode());
    assertThat(dto.hashCode()).isNotEqualTo(dtoWithDifferentId.hashCode());
  }

  @Test
  public void toString_does_not_fail_if_empty() throws Exception {
    ComponentDto dto = new ComponentDto();
    assertThat(dto.toString()).isNotEmpty();
  }

  @Test
  public void is_root_project() throws Exception {
    assertThat(new ComponentDto().setModuleUuid("ABCD").isRootProject()).isFalse();
    assertThat(new ComponentDto().setModuleUuid("ABCD").setScope(Scopes.DIRECTORY).isRootProject()).isFalse();
    assertThat(new ComponentDto().setModuleUuid(null).setScope(Scopes.PROJECT).setQualifier(Qualifiers.PROJECT).isRootProject()).isTrue();
  }
}
