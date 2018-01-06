/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import ProjectEventIcon from '../../../components/icons-components/ProjectEventIcon';
import { translate } from '../../../helpers/l10n';
/*:: import type { Event as EventType } from '../types'; */

export default function EventInner(props /*: { event: EventType } */) {
  const { event } = props;

  return (
    <div className="project-activity-event-inner">
      <div className="project-activity-event-inner-icon little-spacer-right">
        <ProjectEventIcon
          className={'project-activity-event-icon margin-align ' + event.category}
        />
      </div>
      <Tooltip mouseEnterDelay={0.5} overlay={event.name}>
        <span className="project-activity-event-inner-text">
          <span className="note">{translate('event.category', event.category)}:</span>{' '}
          <strong title={event.description}>{event.name}</strong>
        </span>
      </Tooltip>
    </div>
  );
}
