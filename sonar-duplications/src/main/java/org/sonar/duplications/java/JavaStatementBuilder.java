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
package org.sonar.duplications.java;

import static org.sonar.duplications.statement.TokenMatcherFactory.*;

import org.sonar.duplications.statement.StatementChunker;

public final class JavaStatementBuilder {

  private JavaStatementBuilder() {
  }

  public static StatementChunker build() {
    return StatementChunker.builder()
        .ignore(from("import"), to(";"))
        .ignore(from("package"), to(";"))
        .statement(new BridgeWithExceptionTokenMatcher("{", "}", ";"))
        .ignore(token("}"))
        .ignore(token("{"))
        .ignore(token(";"))
        .statement(from("@"), anyToken(), opt(bridge("(", ")")))
        .statement(from("do"))
        .statement(from("if"), bridge("(", ")"))
        .statement(from("else"), token("if"), bridge("(", ")"))
        .statement(from("else"))
        .statement(from("for"), bridge("(", ")"))
        .statement(from("while"), bridge("(", ")"))
        .statement(from("try"), bridge("(", ")"))
        .statement(from("case"), to(";", "{", "}"), forgetLastToken())
        .statement(from("default"), to(";", "{", "}"), forgetLastToken())
        .statement(to(";", "{", "}"), forgetLastToken())
        .build();
  }

}
