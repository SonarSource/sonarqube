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
package org.sonar.server.v2.api.dop.jfrog.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * JFrog evidence response in the in-toto Statement format.
 * See: https://in-toto.io/Statement/v1
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SonarQubeStatement(
  @JsonProperty("_type") String type,
  @JsonProperty("predicateType") String predicateType,
  @JsonProperty("predicate") SonarQubePredicate predicate,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC") @JsonProperty("createdAt") Instant createdAt,
  @JsonProperty("createdBy") String createdBy,
  @JsonProperty("markdown") String markdown) {

  public static final String STATEMENT_TYPE = "https://in-toto.io/Statement/v1";
  public static final String PREDICATE_TYPE = "https://sonar.com/evidence/sonarqube/v1";
  public static final String CREATED_BY = "SonarQube";

  public static SonarQubeStatement create(SonarQubePredicate predicate, String markdown) {
    return new SonarQubeStatement(
      STATEMENT_TYPE,
      PREDICATE_TYPE,
      predicate,
      Instant.now(),
      CREATED_BY,
      markdown);
  }

}
