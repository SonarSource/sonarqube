/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.StringJoiner;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static org.sonar.api.utils.Preconditions.checkArgument;

public class RuleDescriptionSectionDto {
  public static final String DEFAULT_KEY = "default";

  private String uuid;
  private String key;
  private String content;
  private RuleDescriptionSectionContextDto context;

  private RuleDescriptionSectionDto() {
  }

  private RuleDescriptionSectionDto(String uuid, String key, String content, @Nullable RuleDescriptionSectionContextDto context) {
    this.uuid = uuid;
    this.key = key;
    this.content = content;
    this.context = context;
  }

  public String getUuid() {
    return uuid;
  }

  public String getKey() {
    return key;
  }

  public String getContent() {
    return content;
  }

  @CheckForNull
  public RuleDescriptionSectionContextDto getContext() {
    return context;
  }

  public static RuleDescriptionSectionDto createDefaultRuleDescriptionSection(String uuid, String description) {
    return RuleDescriptionSectionDto.builder()
      .setDefault()
      .uuid(uuid)
      .content(description)
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
      .add("content='" + content + "'")
      .add("context='" + context + "'")
      .toString();
  }



  public static final class RuleDescriptionSectionDtoBuilder {
    private String uuid;
    private String key = null;
    private String content;
    private RuleDescriptionSectionContextDto context;

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

    public RuleDescriptionSectionDtoBuilder content(String content) {
      this.content = content;
      return this;
    }

    public RuleDescriptionSectionDtoBuilder context(@Nullable RuleDescriptionSectionContextDto context) {
      this.context = context;
      return this;
    }

    public RuleDescriptionSectionDto build() {
      return new RuleDescriptionSectionDto(uuid, key, content, context);
    }
  }

}
