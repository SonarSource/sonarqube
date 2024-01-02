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
import { AnalysisEvent } from '../../../types/project-activity';
import Event from './Event';

export interface EventsProps {
  analysisKey: string;
  canAdmin?: boolean;
  events: AnalysisEvent[];
  isFirst?: boolean;
  onChange?: (event: string, name: string) => Promise<void>;
  onDelete?: (analysis: string, event: string) => Promise<void>;
}

export function Events(props: EventsProps) {
  const { analysisKey, canAdmin, events, isFirst } = props;

  const sortedEvents = sortBy(
    events,
    // versions last
    (event) => (event.category === 'VERSION' ? 1 : 0),
    // then the rest sorted by category
    'category'
  );

  return (
    <div className="big-spacer-top">
      {sortedEvents.map((event) => (
        <Event
          analysisKey={analysisKey}
          canAdmin={canAdmin}
          event={event}
          isFirst={isFirst}
          key={event.key}
          onChange={props.onChange}
          onDelete={props.onDelete}
        />
      ))}
    </div>
  );
}

export default React.memo(Events);
