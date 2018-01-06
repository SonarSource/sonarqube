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
import { sortBy } from 'lodash';
import Event from './Event';
import DateTooltipFormatter from '../../../components/intl/DateTooltipFormatter';
import { translate } from '../../../helpers/l10n';
/*:: import type { Analysis as AnalysisType, Event as EventType } from '../../projectActivity/types'; */

/*::
type Props = {
  analysis: AnalysisType,
  qualifier: string
};
*/

export default function Analysis(props /*: Props */) {
  const { analysis } = props;
  const sortedEvents /*: Array<EventType> */ = sortBy(
    analysis.events,
    // versions first
    (event /*: EventType */) => (event.category === 'VERSION' ? 0 : 1),
    // then the rest sorted by category
    'category'
  );

  // use `TRK` for all components but applications
  const qualifier = props.qualifier === 'APP' ? 'APP' : 'TRK';

  return (
    <li className="overview-analysis">
      <div className="small little-spacer-bottom">
        <strong>
          <DateTooltipFormatter date={analysis.date} placement="right" />
        </strong>
      </div>

      {sortedEvents.length > 0 ? (
        <div className="project-activity-events">
          {sortedEvents.map(event => <Event event={event} key={event.key} />)}
        </div>
      ) : (
        <span className="note">{translate('project_activity.analyzed', qualifier)}</span>
      )}
    </li>
  );
}
