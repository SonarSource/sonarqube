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
import * as React from 'react';
import EventInner from './EventInner';
import ChangeEventForm from './forms/ChangeEventForm';
import RemoveEventForm from './forms/RemoveEventForm';
import Tooltip from '../../../components/controls/Tooltip';
import { DeleteButton, EditButton } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

interface Props {
  analysis: string;
  canAdmin?: boolean;
  changeEvent: (event: string, name: string) => Promise<void>;
  deleteEvent: (analysis: string, event: string) => Promise<void>;
  event: T.AnalysisEvent;
  isFirst?: boolean;
}

interface State {
  changing: boolean;
  deleting: boolean;
}

export default class Event extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { changing: false, deleting: false };

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
              <Tooltip overlay={translate('project_activity.events.tooltip.edit')}>
                <EditButton className="js-change-event button-small" onClick={this.startChanging} />
              </Tooltip>
            )}
            {canDelete && (
              <Tooltip overlay={translate('project_activity.events.tooltip.delete')}>
                <DeleteButton
                  className="js-delete-event button-small"
                  onClick={this.startDeleting}
                />
              </Tooltip>
            )}
          </div>
        )}

        {this.state.changing && (
          <ChangeEventForm
            changeEvent={this.props.changeEvent}
            event={this.props.event}
            header={
              isVersion
                ? translate('project_activity.change_version')
                : translate('project_activity.change_custom_event')
            }
            onClose={this.stopChanging}
          />
        )}

        {this.state.deleting && (
          <RemoveEventForm
            analysis={this.props.analysis}
            deleteEvent={this.props.deleteEvent}
            event={this.props.event}
            header={
              isVersion
                ? translate('project_activity.remove_version')
                : translate('project_activity.remove_custom_event')
            }
            onClose={this.stopDeleting}
            removeEventQuestion={`project_activity.${
              isVersion ? 'remove_version' : 'remove_custom_event'
            }.question`}
          />
        )}
      </div>
    );
  }
}
