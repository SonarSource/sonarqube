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
// @flow
import React from 'react';
import ProjectEventIcon from '../../../components/icons-components/ProjectEventIcon';
import { translate } from '../../../helpers/l10n';
import type { Event } from '../types';

type Props = {
  events: Array<Event>
};

export default function GraphsTooltipsContentEvents({ events }: Props) {
  return (
    <tbody>
      <tr><td className="project-activity-graph-tooltip-separator" colSpan="3"><hr /></td></tr>
      {events.map(event => (
        <tr key={event.key} className="project-activity-graph-tooltip-line">
          <td className="spacer-right thin">
            <ProjectEventIcon className={'project-activity-event-icon ' + event.category} />
          </td>
          <td colSpan="2">
            <span>{translate('event.category', event.category)}:</span>
            {' '} {event.name}
          </td>
        </tr>
      ))}
    </tbody>
  );
}
