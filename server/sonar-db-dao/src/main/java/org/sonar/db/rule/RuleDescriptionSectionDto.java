/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.db.rule;

import java.util.Objects;
import java.util.StringJoiner;

import static org.sonar.api.utils.Preconditions.checkArgument;

public class RuleDescriptionSectionDto {
  public static final String DEFAULT_KEY = "default";

  private final String uuid;
  private final String key;
  private final String description;

  private RuleDescriptionSectionDto(String uuid, String key, String description) {
    this.uuid = uuid;
    this.key = key;
    this.description = description;
  }

  public String getUuid() {
    return uuid;
  }

  public String getKey() {
    return key;
  }

  public String getDescription() {
    return description;
  }

  public static RuleDescriptionSectionDto createDefaultRuleDescriptionSection(String uuid, String description) {
    return RuleDescriptionSectionDto.builder()
      .setDefault()
      .uuid(uuid)
      .description(description)
      .build();
  }

  public static RuleDescriptionSectionDtoBuilder builder() {
    return new RuleDescriptionSectionDtoBuilder();
  }

  public boolean isDefault() {
    return DEFAULT_KEY.equals(key);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", RuleDescriptionSectionDto.class.getSimpleName() + "[", "]")
      .add("uuid='" + uuid + "'")
      .add("key='" + key + "'")
      .add("description='" + description + "'")
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RuleDescriptionSectionDto that = (RuleDescriptionSectionDto) o;
    return Objects.equals(uuid, that.uuid) && Objects.equals(key, that.key) && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, key, description);
  }

  public static final class RuleDescriptionSectionDtoBuilder {
    private String uuid;
    private String key = null;
    private String description;

    private RuleDescriptionSectionDtoBuilder() {
    }

    public RuleDescriptionSectionDtoBuilder uuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public RuleDescriptionSectionDtoBuilder setDefault() {
      checkArgument(this.key == null, "Only one of setDefault and key methods can be called");
      this.key = DEFAULT_KEY;
      return this;
    }

    public RuleDescriptionSectionDtoBuilder key(String key) {
      checkArgument(this.key == null, "Only one of setDefault and key methods can be called");
      this.key = key;
      return this;
    }

    public RuleDescriptionSectionDtoBuilder description(String description) {
      this.description = description;
      return this;
    }

    public RuleDescriptionSectionDto build() {
      return new RuleDescriptionSectionDto(uuid, key, description);
    }
  }
}
