/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import sortBy from 'lodash/sortBy';
import Event from './Event';
import EditableEvent from './EditableEvent';
import type { Event as EventType } from '../../../store/projectActivity/duck';

export default class Events extends React.Component {
  props: {
    analysis: string,
    events: Array<EventType>,
    project: string,
    canAdmin: boolean
  };

  renderEvent = (event: EventType) => {
    if (this.props.canAdmin) {
      return (
          <EditableEvent
              key={event.key}
              analysis={this.props.analysis}
              event={event}
              project={this.props.project}/>
      );
    } else {
      return (
          <Event key={event.key} event={event}/>
      );
    }
  };

  render () {
    const sortedEvents: Array<EventType> = sortBy(
        this.props.events,
        // versions first
        (event: EventType) => event.category === 'VERSION' ? 0 : 1,
        // then the rest sorted by category
        'category'
    );

    return (
        <div>
          {sortedEvents.map(this.renderEvent)}
        </div>
    );
  }
}
