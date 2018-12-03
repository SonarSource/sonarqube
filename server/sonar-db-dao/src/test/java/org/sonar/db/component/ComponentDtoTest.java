/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.organization.OrganizationDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;

public class ComponentDtoTest {

  @Test
  public void setters_and_getters() {
    ComponentDto componentDto = new ComponentDto()
      .setId(1L)
      .setDbKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setName("RequestContext.java")
      .setLongName("org.struts.RequestContext")
      .setQualifier("FIL")
      .setScope("FIL")
      .setLanguage("java")
      .setDescription("desc")
      .setPath("src/org/struts/RequestContext.java")
      .setCopyComponentUuid("uuid_5")
      .setRootUuid("uuid_3");

    assertThat(componentDto.getId()).isEqualTo(1L);
    assertThat(componentDto.getDbKey()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(componentDto.getBranch()).isNull();
    assertThat(componentDto.name()).isEqualTo("RequestContext.java");
    assertThat(componentDto.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(componentDto.qualifier()).isEqualTo("FIL");
    assertThat(componentDto.scope()).isEqualTo("FIL");
    assertThat(componentDto.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(componentDto.language()).isEqualTo("java");
    assertThat(componentDto.description()).isEqualTo("desc");
    assertThat(componentDto.getRootUuid()).isEqualTo("uuid_3");
    assertThat(componentDto.getCopyResourceUuid()).isEqualTo("uuid_5");
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
  public void formatUuidPathFromParent() {
    ComponentDto parent = ComponentTesting.newPrivateProjectDto(newOrganizationDto(), "123").setUuidPath(ComponentDto.UUID_PATH_OF_ROOT);
    assertThat(ComponentDto.formatUuidPathFromParent(parent)).isEqualTo(".123.");
  }

  @Test
  public void getUuidPathLikeIncludingSelf() {
    OrganizationDto organizationDto = newOrganizationDto();

    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto).setUuidPath(ComponentDto.UUID_PATH_OF_ROOT);
    assertThat(project.getUuidPathLikeIncludingSelf()).isEqualTo("." + project.uuid() + ".%");

    ComponentDto module = ComponentTesting.newModuleDto(project);
    assertThat(module.getUuidPathLikeIncludingSelf()).isEqualTo("." + project.uuid() + "." + module.uuid() + ".%");

    ComponentDto file = ComponentTesting.newFileDto(module);
    assertThat(file.getUuidPathLikeIncludingSelf()).isEqualTo("." + project.uuid() + "." + module.uuid() + "." + file.uuid() + ".%");
  }

  @Test
  public void getUuidPathAsList() {
    ComponentDto root = new ComponentDto().setUuidPath(ComponentDto.UUID_PATH_OF_ROOT);
    assertThat(root.getUuidPathAsList()).isEmpty();

    ComponentDto nonRoot = new ComponentDto().setUuidPath(".12.34.56.");
    assertThat(nonRoot.getUuidPathAsList()).containsExactly("12", "34", "56");
  }

  @Test
  public void getKey_and_getBranch() {
    ComponentDto underTest = new ComponentDto().setDbKey("my_key:BRANCH:my_branch");
    assertThat(underTest.getKey()).isEqualTo("my_key");
    assertThat(underTest.getBranch()).isEqualTo("my_branch");

    underTest = new ComponentDto().setDbKey("my_key");
    assertThat(underTest.getKey()).isEqualTo("my_key");
    assertThat(underTest.getBranch()).isNull();
  }

  @Test
  public void getKey_and_getPullRequest() {
    ComponentDto underTest = new ComponentDto().setDbKey("my_key:PULL_REQUEST:pr-123");
    assertThat(underTest.getKey()).isEqualTo("my_key");
    assertThat(underTest.getPullRequest()).isEqualTo("pr-123");

    underTest = new ComponentDto().setDbKey("my_key");
    assertThat(underTest.getKey()).isEqualTo("my_key");
    assertThat(underTest.getPullRequest()).isNull();
  }
}
