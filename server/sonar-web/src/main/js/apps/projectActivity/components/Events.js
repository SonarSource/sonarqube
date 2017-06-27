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
import Event from './Event';
import type { Event as EventType } from '../types';

type Props = {
  analysis?: string,
  canAdmin?: boolean,
  changeEvent?: (event: string, name: string) => Promise<*>,
  deleteEvent?: (analysis: string, event: string) => Promise<*>,
  events: Array<EventType>,
  isFirst?: boolean
};

export default function Events(props: Props) {
  return (
    <div className="project-activity-events">
      {props.events.map(event => (
        <Event
          analysis={props.analysis}
          canAdmin={props.canAdmin}
          changeEvent={props.changeEvent}
          deleteEvent={props.deleteEvent}
          event={event}
          isFirst={props.isFirst}
          key={event.key}
        />
      ))}
    </div>
  );
}
