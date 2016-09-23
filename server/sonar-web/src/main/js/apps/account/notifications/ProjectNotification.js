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
import classNames from 'classnames';
import React, { Component } from 'react';

import NotificationsList from './NotificationsList';
import { translate } from '../../../helpers/l10n';

export default class ProjectNotification extends Component {
  state = {
    toDelete: false
  };

  handleRemoveProject (e) {
    e.preventDefault();
    if (this.state.toDelete) {
      const { data, onRemoveProject } = this.props;
      onRemoveProject(data.project);
    } else {
      this.setState({ toDelete: true });
    }
  }

  render () {
    const { data, channels } = this.props;
    const buttonClassName = classNames('big-spacer-left', 'button-red', {
      'active': this.state.toDelete
    });

    return (
        <table key={data.project.internalId} className="form big-spacer-bottom">
          <thead>
            <tr>
              <th>
                <h4 className="display-inline-block">{data.project.name}</h4>
                <button
                    onClick={this.handleRemoveProject.bind(this)}
                    className={buttonClassName}>
                  {this.state.toDelete ? 'Sure?' : translate('delete')}
                </button>
              </th>
              {channels.map(channel => (
                  <th key={channel} className="text-center">
                    <h4>{translate('notification.channel', channel)}</h4>
                  </th>
              ))}
            </tr>
          </thead>
          <NotificationsList
              notifications={data.notifications}
              checkboxId={(d, c) => `project_notifs_${data.project.internalId}_${d}_${c}`}
              checkboxName={(d, c) => `project_notifs[${data.project.internalId}][${d}][${c}]`}/>
        </table>
    );
  }
}
