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
import Event from './Event';
import ChangeCustomEventForm from './forms/ChangeCustomEventForm';
import RemoveCustomEventForm from './forms/RemoveCustomEventForm';
import type { Event as EventType } from '../../../store/projectActivity/duck';

type Props = {
  analysis: string,
  event: EventType,
  project: string
};

type State = {
  changing: boolean,
  deleting: boolean
};

export default class EditableEvent extends React.Component {
  mounted: boolean;
  props: Props;

  state: State = {
    changing: false,
    deleting: false
  };

  componentDidMount () {
    this.mounted = true;
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  startChanging = () => {
    this.setState({ changing: true });
  };

  stopChanging = () => {
    if (this.mounted) {
      this.setState({ changing: false });
    }
  };

  startDeleting = () => {
    this.setState({ deleting: true });
  };

  stopDeleting = () => {
    if (this.mounted) {
      this.setState({ deleting: false });
    }
  };

  render () {
    if (this.props.event.category !== 'OTHER' && this.props.event.category !== 'VERSION') {
      return <Event event={this.props.event}/>;
    }

    if (this.state.changing) {
      return (
          <ChangeCustomEventForm
              analysis={this.props.analysis}
              event={this.props.event}
              project={this.props.project}
              onClose={this.stopChanging}/>
      );
    }

    if (this.state.deleting) {
      return (
          <RemoveCustomEventForm
              analysis={this.props.analysis}
              event={this.props.event}
              project={this.props.project}
              onClose={this.stopDeleting}/>
      );
    }

    return (
        <div className="project-activity-editable-event">
          <div className="project-activity-editable-event-actions">
            <button className="button-small" onClick={this.startChanging}>
              Change
            </button>

            <button className="spacer-left button-small button-red" onClick={this.startDeleting}>
              Delete
            </button>
          </div>

          <Event event={this.props.event}/>
        </div>
    );
  }
}
