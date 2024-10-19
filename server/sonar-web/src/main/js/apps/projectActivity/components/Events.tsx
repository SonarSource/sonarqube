/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { AnalysisEvent, ProjectAnalysisEventCategory } from '../../../types/project-activity';
import Event from './Event';

export interface EventsProps {
  analysisKey: string;
  canAdmin?: boolean;
  events: AnalysisEvent[];
  isFirst?: boolean;
}

function Events(props: EventsProps) {
  const { analysisKey, canAdmin, events, isFirst } = props;

  const sortedEvents = sortBy(
    events,
    (event) => {
      switch (event.category) {
        case ProjectAnalysisEventCategory.SqUpgrade:
          // SQ Upgrade first
          return 0;
        case ProjectAnalysisEventCategory.Version:
          // versions last
          return 2;
        default:
          // then the rest in between, sorted by category
          return 1;
      }
    },
    'category',
  );

  return (
    <div className="sw-flex sw-flex-1 sw-flex-col sw-gap-2 sw-min-w-0">
      {sortedEvents.map((event) => (
        <Event
          analysisKey={analysisKey}
          canAdmin={canAdmin}
          event={event}
          isFirst={isFirst}
          key={event.key}
        />
      ))}
    </div>
  );
}

export default React.memo(Events);
