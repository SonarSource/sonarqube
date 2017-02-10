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
import EventInner from './EventInner';
import ChangeCustomEventForm from './forms/ChangeCustomEventForm';
import RemoveCustomEventForm from './forms/RemoveCustomEventForm';
import DeleteIcon from './DeleteIcon';
import ChangeIcon from './ChangeIcon';
import type { Event as EventType } from '../../../store/projectActivity/duck';

type Props = {
  analysis: string,
  event: EventType,
  isFirst: boolean,
  canAdmin: boolean
};

type State = {
  changing: boolean,
  deleting: boolean
};

export default class Event extends React.Component {
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
    const { event, canAdmin } = this.props;
    const canChange = ['OTHER', 'VERSION'].includes(event.category);
    const canDelete = event.category === 'OTHER' || (event.category === 'VERSION' && !this.props.isFirst);
    const showActions = canAdmin && (canChange || canDelete);

    return (
        <div className="project-activity-event">
          <EventInner event={this.props.event}/>

          {showActions && (
              <div className="project-activity-event-actions">
                {canChange && (
                    <button className="js-change-event button-clean" onClick={this.startChanging}>
                      <ChangeIcon/>
                    </button>
                )}
                {canDelete && (
                    <button className="js-delete-event button-clean" onClick={this.startDeleting}>
                      <DeleteIcon/>
                    </button>
                )}
              </div>
          )}

          {this.state.changing && (
              <ChangeCustomEventForm
                  event={this.props.event}
                  onClose={this.stopChanging}/>
          )}

          {this.state.deleting && (
              <RemoveCustomEventForm
                  analysis={this.props.analysis}
                  event={this.props.event}
                  onClose={this.stopDeleting}/>
          )}
        </div>
    );
  }
}
