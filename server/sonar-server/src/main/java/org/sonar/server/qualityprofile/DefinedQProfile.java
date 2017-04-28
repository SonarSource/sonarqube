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
package org.sonar.server.qualityprofile;

import com.google.common.collect.ImmutableList;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.profiles.ProfileDefinition;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.lang.StringUtils.lowerCase;
import static org.sonar.db.loadedtemplate.LoadedTemplateDto.QUALITY_PROFILE_TYPE;

/**
 * Represent a Quality Profile as computed from {@link ProfileDefinition} provided by installed plugins.
 */
@Immutable
public final class DefinedQProfile {
  private final QProfileName qProfileName;
  private final boolean isDefault;
  private final String loadedTemplateType;
  private final List<org.sonar.api.rules.ActiveRule> activeRules;
  private final QProfileName parentQProfileName;

  private DefinedQProfile(Builder builder, MessageDigest messageDigest) {
    this.qProfileName = new QProfileName(builder.language, builder.getName());
    this.isDefault = builder.declaredDefault || builder.computedDefault;
    this.loadedTemplateType = computeLoadedTemplateType(this.qProfileName, messageDigest);
    this.activeRules = ImmutableList.copyOf(builder.activeRules);
    this.parentQProfileName = builder.parentName == null ? null : new QProfileName(builder.language, builder.parentName);
  }

  private static String computeLoadedTemplateType(QProfileName qProfileName, MessageDigest messageDigest) {
    String qpIdentifier = lowerCase(qProfileName.getLanguage()) + ":" + qProfileName.getName();
    return format("%s.%s", QUALITY_PROFILE_TYPE, encodeHexString(messageDigest.digest(qpIdentifier.getBytes(UTF_8))));
  }

  public String getName() {
    return qProfileName.getName();
  }

  public String getLanguage() {
    return qProfileName.getLanguage();
  }

  public QProfileName getQProfileName() {
    return qProfileName;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public String getLoadedTemplateType() {
    return loadedTemplateType;
  }

  public List<org.sonar.api.rules.ActiveRule> getActiveRules() {
    return activeRules;
  }

  @CheckForNull
  public QProfileName getParentQProfileName() {
    return parentQProfileName;
  }

  static final class Builder {
    private String language;
    private String name;
    private boolean declaredDefault;
    private boolean computedDefault;
    private final List<org.sonar.api.rules.ActiveRule> activeRules = new ArrayList<>();
    private String parentName;

    public Builder setLanguage(String language) {
      this.language = language;
      return this;
    }

    Builder setName(String name) {
      this.name = name;
      return this;
    }

    String getName() {
      return name;
    }

    Builder setDeclaredDefault(boolean declaredDefault) {
      this.declaredDefault = declaredDefault;
      return this;
    }

    boolean isDeclaredDefault() {
      return declaredDefault;
    }

    Builder setComputedDefault(boolean flag) {
      computedDefault = flag;
      return this;
    }

    Builder addRules(List<org.sonar.api.rules.ActiveRule> rules) {
      this.activeRules.addAll(rules);
      return this;
    }

    public Builder setParentName(@Nullable String parentName) {
      this.parentName = parentName;
      return this;
    }

    @CheckForNull
    public String getParentName() {
      return parentName;
    }

    DefinedQProfile build(MessageDigest messageDigest) {
      return new DefinedQProfile(this, messageDigest);
    }
  }
}
