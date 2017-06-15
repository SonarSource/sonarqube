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
import moment from 'moment';
import React from 'react';
import PendingIcon from '../../../../components/shared/pending-icon';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

export default class ComponentNavMeta extends React.PureComponent {
  render() {
    const metaList = [];
    const canSeeBackgroundTasks = this.props.conf.showBackgroundTasks;
    const backgroundTasksUrl =
      window.baseUrl +
      `/project/background_tasks?id=${encodeURIComponent(this.props.component.key)}`;

    if (this.props.isInProgress) {
      const tooltip = canSeeBackgroundTasks
        ? translateWithParameters(
            'component_navigation.status.in_progress.admin',
            backgroundTasksUrl
          )
        : translate('component_navigation.status.in_progress');
      metaList.push(
        <li key="isInProgress" data-toggle="tooltip" title={tooltip}>
          <i className="spinner" style={{ marginTop: '-1px' }} />
          {' '}
          <span className="text-info">{translate('background_task.status.IN_PROGRESS')}</span>
        </li>
      );
    } else if (this.props.isPending) {
      const tooltip = canSeeBackgroundTasks
        ? translateWithParameters('component_navigation.status.pending.admin', backgroundTasksUrl)
        : translate('component_navigation.status.pending');
      metaList.push(
        <li key="isPending" data-toggle="tooltip" title={tooltip}>
          <PendingIcon /> <span>{translate('background_task.status.PENDING')}</span>
        </li>
      );
    } else if (this.props.isFailed) {
      const tooltip = canSeeBackgroundTasks
        ? translateWithParameters('component_navigation.status.failed.admin', backgroundTasksUrl)
        : translate('component_navigation.status.failed');
      metaList.push(
        <li key="isFailed" data-toggle="tooltip" title={tooltip}>
          <span className="badge badge-danger">{translate('background_task.status.FAILED')}</span>
        </li>
      );
    }

    if (this.props.analysisDate) {
      metaList.push(<li key="analysisDate">{moment(this.props.analysisDate).format('LLL')}</li>);
    }

    if (this.props.version) {
      metaList.push(<li key="version">Version {this.props.version}</li>);
    }

    return (
      <div className="navbar-context-meta">
        <ul className="list-inline">{metaList}</ul>
      </div>
    );
  }
}
