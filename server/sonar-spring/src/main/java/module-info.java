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
/**
 * SonarQube Spring shared configuration and utilities module.
 * 
 * <p>
 * This module provides Spring configuration, exception handling,
 * and common utilities for SonarQube server components.
 * 
 * <h2>Public API:</h2>
 * <ul>
 * <li>{@code org.sonar.server.exceptions} - Custom exception classes</li>
 * <li>{@code org.sonar.server.v2.api.model} - REST API models</li>
 * <li>{@code org.sonar.server.v2.common} - Common utilities and handlers</li>
 * <li>{@code org.sonar.server.v2.config} - Spring configuration classes</li>
 * </ul>
 */
module org.sonar.server.spring {
    requires com.fasterxml.jackson.annotation;
    requires org.hibernate.validator;
    requires spring.context;
    requires spring.web;
    requires spring.webmvc;
    requires jakarta.validation;

    requires transitive com.google.common;
    requires transitive org.slf4j;

    requires static jakarta.servlet;

    exports org.sonar.server.exceptions;
    exports org.sonar.server.v2.api.model;
    exports org.sonar.server.v2.common;
    exports org.sonar.server.v2.config;
}
