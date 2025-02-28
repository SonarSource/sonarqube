/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.v2.api.rule.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.sonar.core.rule.RuleType;
import org.sonar.server.v2.api.rule.enums.CleanCodeAttributeRestEnum;
import org.sonar.server.v2.api.rule.enums.RuleStatusRestEnum;
import org.sonar.server.v2.api.rule.resource.Impact;
import org.sonar.server.v2.api.rule.resource.Parameter;

public record RuleCreateRestRequest(

  @NotNull
  @Size(max = 200)
  @Schema(description = "Key of the custom rule to create, must include the repository")
  String key,

  @NotNull
  @Size(max = 200)
  @Schema(description = "Key of the rule template to be used to create the custom rule")
  String templateKey,

  @NotNull
  @Size(max = 200)
  @Schema(description = "Rule name")
  String name,

  @NotNull
  @Schema(description = "Rule description in markdown format")
  String markdownDescription,

  @Nullable
  @Schema(description = "Rule status", defaultValue = "READY")
  RuleStatusRestEnum status,

  @Nullable
  @Schema(description = "Custom rule parameters")
  List<Parameter> parameters,

  @Nullable
  @Schema(description = "Clean code attribute")
  CleanCodeAttributeRestEnum cleanCodeAttribute,

  @Valid
  @NotNull
  @Schema(description = "Impacts")
  List<Impact> impacts,

  @Nullable
  @Schema(description = "Severity")
  String severity,

  @Nullable
  @Schema(description = "Rule type")
  RuleType type
) {
}
