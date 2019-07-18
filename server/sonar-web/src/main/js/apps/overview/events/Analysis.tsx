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
import { sortBy } from 'lodash';
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import DateTooltipFormatter from '../../../components/intl/DateTooltipFormatter';
import Event from './Event';

interface Props {
  analysis: T.Analysis;
  qualifier: string;
}

export default function Analysis({ analysis, ...props }: Props) {
  const sortedEvents = sortBy(
    analysis.events,
    // versions first
    event => (event.category === 'VERSION' ? 0 : 1),
    // then the rest sorted by category
    'category'
  );

  // use `TRK` for all components but applications
  const qualifier = props.qualifier === 'APP' ? 'APP' : 'TRK';

  return (
    <li className="overview-analysis">
      <div className="small little-spacer-bottom">
        <strong>
          <DateTooltipFormatter date={analysis.date} />
        </strong>
      </div>

      {sortedEvents.length > 0 ? (
        <div className="overview-activity-events">
          {sortedEvents.map(event => (
            <Event event={event} key={event.key} />
          ))}
        </div>
      ) : (
        <span className="note">{translate('project_activity.analyzed', qualifier)}</span>
      )}
    </li>
  );
}
