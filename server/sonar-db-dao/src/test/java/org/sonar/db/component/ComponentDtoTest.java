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
package org.sonar.db.component;

import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.db.organization.OrganizationTesting;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentDtoTest {

  @Test
  public void setters_and_getters() {
    ComponentDto componentDto = new ComponentDto()
      .setId(1L)
      .setKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setDeprecatedKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setName("RequestContext.java")
      .setLongName("org.struts.RequestContext")
      .setQualifier("FIL")
      .setScope("FIL")
      .setLanguage("java")
      .setDescription("desc")
      .setPath("src/org/struts/RequestContext.java")
      .setCopyComponentUuid("uuid_5")
      .setRootUuid("uuid_3")
      .setDeveloperUuid("uuid_6")
      .setAuthorizationUpdatedAt(123456789L)
      ;

    assertThat(componentDto.getId()).isEqualTo(1L);
    assertThat(componentDto.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(componentDto.deprecatedKey()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(componentDto.name()).isEqualTo("RequestContext.java");
    assertThat(componentDto.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(componentDto.qualifier()).isEqualTo("FIL");
    assertThat(componentDto.scope()).isEqualTo("FIL");
    assertThat(componentDto.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(componentDto.language()).isEqualTo("java");
    assertThat(componentDto.description()).isEqualTo("desc");
    assertThat(componentDto.getRootUuid()).isEqualTo("uuid_3");
    assertThat(componentDto.getCopyResourceUuid()).isEqualTo("uuid_5");
    assertThat(componentDto.getDeveloperUuid()).isEqualTo("uuid_6");
    assertThat(componentDto.getAuthorizationUpdatedAt()).isEqualTo(123456789L);
    assertThat(componentDto.isPrivate()).isFalse();
  }

  @Test
  public void equals_and_hashcode() {
    ComponentDto dto = new ComponentDto().setUuid("u1");
    ComponentDto dtoWithSameUuid = new ComponentDto().setUuid("u1");
    ComponentDto dtoWithDifferentUuid = new ComponentDto().setUuid("u2");

    assertThat(dto).isEqualTo(dto);
    assertThat(dto).isEqualTo(dtoWithSameUuid);
    assertThat(dto).isNotEqualTo(dtoWithDifferentUuid);

    assertThat(dto.hashCode()).isEqualTo(dto.hashCode());
    assertThat(dto.hashCode()).isEqualTo(dtoWithSameUuid.hashCode());
    assertThat(dto.hashCode()).isNotEqualTo(dtoWithDifferentUuid.hashCode());
  }

  @Test
  public void toString_does_not_fail_if_empty() {
    ComponentDto dto = new ComponentDto();
    assertThat(dto.toString()).isNotEmpty();
  }

  @Test
  public void is_root_project() {
    assertThat(new ComponentDto().setModuleUuid("ABCD").isRootProject()).isFalse();
    assertThat(new ComponentDto().setModuleUuid("ABCD").setScope(Scopes.DIRECTORY).isRootProject()).isFalse();
    assertThat(new ComponentDto().setModuleUuid(null).setScope(Scopes.PROJECT).setQualifier(Qualifiers.PROJECT).isRootProject()).isTrue();
  }

  @Test
  public void test_formatUuidPathFromParent() {
    ComponentDto parent = ComponentTesting.newPrivateProjectDto(OrganizationTesting.newOrganizationDto(), "123").setUuidPath(ComponentDto.UUID_PATH_OF_ROOT);
    assertThat(ComponentDto.formatUuidPathFromParent(parent)).isEqualTo(".123.");
  }

  @Test
  public void test_Name() {
    ComponentDto root = new ComponentDto().setUuidPath(ComponentDto.UUID_PATH_OF_ROOT);
    assertThat(root.getUuidPathAsList()).isEmpty();

    ComponentDto nonRoot = new ComponentDto().setUuidPath(".12.34.56.");
    assertThat(nonRoot.getUuidPathAsList()).containsExactly("12", "34", "56");
  }
}
