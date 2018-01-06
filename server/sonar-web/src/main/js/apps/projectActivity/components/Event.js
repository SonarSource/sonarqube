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
import EventInner from './EventInner';
import ChangeEventForm from './forms/ChangeEventForm';
import RemoveEventForm from './forms/RemoveEventForm';
import { DeleteButton, EditButton } from '../../../components/ui/buttons';
/*:: import type { Event as EventType } from '../types'; */

/*::
type Props = {
  analysis: string,
  canAdmin: boolean,
  changeEvent: (event: string, name: string) => Promise<*>,
  deleteEvent: (analysis: string, event: string) => Promise<*>,
  event: EventType,
  isFirst: boolean
};
*/

/*::
type State = {
  changing: boolean,
  deleting: boolean
};
*/

export default class Event extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    changing: false,
    deleting: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
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

  render() {
    const { event, canAdmin } = this.props;
    const isOther = event.category === 'OTHER';
    const isVersion = !isOther && event.category === 'VERSION';
    const canChange = isOther || isVersion;
    const canDelete = isOther || (isVersion && !this.props.isFirst);
    const showActions = canAdmin && (canChange || canDelete);

    return (
      <div className="project-activity-event">
        <EventInner event={this.props.event} />

        {showActions && (
          <div className="project-activity-event-actions spacer-left">
            {canChange && (
              <EditButton className="js-change-event button-small" onClick={this.startChanging} />
            )}
            {canDelete && (
              <DeleteButton className="js-delete-event button-small" onClick={this.startDeleting} />
            )}
          </div>
        )}

        {this.state.changing && (
          <ChangeEventForm
            changeEventButtonText={
              'project_activity.' + (isVersion ? 'change_version' : 'change_custom_event')
            }
            changeEvent={this.props.changeEvent}
            event={this.props.event}
            onClose={this.stopChanging}
          />
        )}

        {this.state.deleting && (
          <RemoveEventForm
            analysis={this.props.analysis}
            deleteEvent={this.props.deleteEvent}
            event={this.props.event}
            onClose={this.stopDeleting}
            removeEventButtonText={
              'project_activity.' + (isVersion ? 'remove_version' : 'remove_custom_event')
            }
            removeEventQuestion={`project_activity.${
              isVersion ? 'remove_version' : 'remove_custom_event'
            }.question`}
          />
        )}
      </div>
    );
  }
}
