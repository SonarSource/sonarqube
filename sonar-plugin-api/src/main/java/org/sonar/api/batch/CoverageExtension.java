/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.batch;

import org.sonar.api.BatchExtension;

/**
 * Marker for extension. Extension which implements this interface would be active, when:
 * <ul>
 * <li>corresponding coverage engine activated (see {@link AbstractCoverageExtension#PARAM_PLUGIN}) and language is Java</li>
 * <li>type of analysis is dynamic or reuse reports</li>
 * </ul>
 * 
 * @since 2.6
 * @TODO Ability to configure coverage engine per language - http://jira.codehaus.org/browse/SONAR-1803
 */
public interface CoverageExtension extends BatchExtension {

}
