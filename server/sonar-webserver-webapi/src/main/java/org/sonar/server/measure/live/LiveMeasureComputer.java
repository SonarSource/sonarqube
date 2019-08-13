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
package org.sonar.server.measure.live;

import java.util.Collection;
import java.util.List;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;

/**
 * Refresh and persist the measures of some files, directories, modules
 * or projects. Measures include status of quality gate.
 *
 * Touching a file updates the related directory, module and project.
 * Status of Quality gate is refreshed but webhooks are not triggered.
 *
 * Branches are supported.
 */
@ServerSide
public interface LiveMeasureComputer {

  List<QGChangeEvent> refresh(DbSession dbSession, Collection<ComponentDto> components);

}
