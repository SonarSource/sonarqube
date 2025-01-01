/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentDtoTest {

  @Test
  void setters_and_getters() {
    ComponentDto componentDto = new ComponentDto()
      .setKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setName("RequestContext.java")
      .setLongName("org.struts.RequestContext")
      .setQualifier("FIL")
      .setScope("FIL")
      .setLanguage("java")
      .setDescription("desc")
      .setPath("src/org/struts/RequestContext.java")
      .setCopyComponentUuid("uuid_5");

    assertThat(componentDto.getKey()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(componentDto.name()).isEqualTo("RequestContext.java");
    assertThat(componentDto.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(componentDto.qualifier()).isEqualTo("FIL");
    assertThat(componentDto.scope()).isEqualTo("FIL");
    assertThat(componentDto.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(componentDto.language()).isEqualTo("java");
    assertThat(componentDto.description()).isEqualTo("desc");
    assertThat(componentDto.getCopyComponentUuid()).isEqualTo("uuid_5");
    assertThat(componentDto.isPrivate()).isFalse();
  }

  @Test
  void equals_and_hashcode() {
    ComponentDto dto = new ComponentDto().setUuid("u1");
    ComponentDto dtoWithSameUuid = new ComponentDto().setUuid("u1");
    ComponentDto dtoWithDifferentUuid = new ComponentDto().setUuid("u2");

    assertThat(dto)
      .isEqualTo(dto)
      .isEqualTo(dtoWithSameUuid)
      .isNotEqualTo(dtoWithDifferentUuid)
      .hasSameHashCodeAs(dto)
      .hasSameHashCodeAs(dtoWithSameUuid);
    assertThat(dto.hashCode()).isNotEqualTo(dtoWithDifferentUuid.hashCode());
  }

  @Test
  void toString_does_not_fail_if_empty() {
    ComponentDto dto = new ComponentDto();
    assertThat(dto.toString()).isNotEmpty();
  }

  @Test
  void is_root_project() {
    assertThat(new ComponentDto().setUuid("uuid").setBranchUuid("branch").isRootProject()).isFalse();
    assertThat(new ComponentDto().setScope(ComponentScopes.DIRECTORY).setUuid("uuid").setBranchUuid("uuid").isRootProject()).isFalse();
    assertThat(new ComponentDto().setScope(ComponentScopes.PROJECT).setUuid("uuid").setBranchUuid("uuid").isRootProject()).isTrue();
  }

  @Test
  void formatUuidPathFromParent() {
    ComponentDto parent = ComponentTesting.newPrivateProjectDto("123").setUuidPath(ComponentDto.UUID_PATH_OF_ROOT);
    assertThat(ComponentDto.formatUuidPathFromParent(parent)).isEqualTo(".123.");
  }

  @Test
  void getUuidPathLikeIncludingSelf() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto().setUuidPath(ComponentDto.UUID_PATH_OF_ROOT);
    assertThat(project.getUuidPathLikeIncludingSelf()).isEqualTo("." + project.uuid() + ".%");

    ComponentDto dir = ComponentTesting.newDirectory(project, "path");
    assertThat(dir.getUuidPathLikeIncludingSelf()).isEqualTo("." + project.uuid() + "." + dir.uuid() + ".%");

    ComponentDto file = ComponentTesting.newFileDto(project, dir);
    assertThat(file.getUuidPathLikeIncludingSelf()).isEqualTo("." + project.uuid() + "." + dir.uuid() + "." + file.uuid() + ".%");
  }

  @Test
  void getUuidPathAsList() {
    ComponentDto root = new ComponentDto().setUuidPath(ComponentDto.UUID_PATH_OF_ROOT);
    assertThat(root.getUuidPathAsList()).isEmpty();

    ComponentDto nonRoot = new ComponentDto().setUuidPath(".12.34.56.");
    assertThat(nonRoot.getUuidPathAsList()).containsExactly("12", "34", "56");
  }
}
