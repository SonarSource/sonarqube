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
import * as React from 'react';
import { Link } from 'react-router';
import { FormattedMessage } from 'react-intl';
import ComponentNavLicenseNotif from './ComponentNavLicenseNotif';
import NavBarNotif from '../../../../components/nav/NavBarNotif';
import PendingIcon from '../../../../components/icons-components/PendingIcon';
import { Component, Task } from '../../../types';
import { STATUSES } from '../../../../apps/background-tasks/constants';
import { getComponentBackgroundTaskUrl } from '../../../../helpers/urls';
import { hasMessage, translate } from '../../../../helpers/l10n';

interface Props {
  component: Component;
  currentTask?: Task;
  currentTaskOnSameBranch?: boolean;
  isInProgress?: boolean;
  isPending?: boolean;
}

export default class ComponentNavBgTaskNotif extends React.PureComponent<Props> {
  renderMessage(messageKey: string, status?: string, branch?: string) {
    const { component } = this.props;
    const canSeeBackgroundTasks =
      component.configuration && component.configuration.showBackgroundTasks;

    let url;
    if (canSeeBackgroundTasks) {
      messageKey += '.admin';
      url = (
        <Link to={getComponentBackgroundTaskUrl(component.key, status)}>
          {translate('background_tasks.page')}
        </Link>
      );
    }

    return (
      <FormattedMessage
        defaultMessage={translate(messageKey)}
        id={messageKey}
        values={{ branch, url }}
      />
    );
  }

  render() {
    const { currentTask, currentTaskOnSameBranch, isInProgress, isPending } = this.props;
    if (isInProgress) {
      return (
        <NavBarNotif variant="info">
          <i className="spinner spacer-right text-bottom" />
          {this.renderMessage('component_navigation.status.in_progress')}
        </NavBarNotif>
      );
    } else if (isPending) {
      return (
        <NavBarNotif variant="info">
          <PendingIcon className="spacer-right" />
          {this.renderMessage('component_navigation.status.pending', STATUSES.ALL)}
        </NavBarNotif>
      );
    } else if (currentTask && currentTask.status === STATUSES.FAILED) {
      if (
        currentTask.errorType &&
        hasMessage('license.component_navigation.button', currentTask.errorType)
      ) {
        return <ComponentNavLicenseNotif currentTask={currentTask} />;
      }
      const branch =
        currentTask.branch ||
        `${currentTask.pullRequest}${
          currentTask.pullRequestTitle ? ' - ' + currentTask.pullRequestTitle : ''
        }`;
      let message;
      if (currentTaskOnSameBranch === false && branch) {
        message = this.renderMessage(
          'component_navigation.status.failed_branch',
          undefined,
          branch
        );
      } else {
        message = this.renderMessage('component_navigation.status.failed');
      }

      return <NavBarNotif variant="error">{message}</NavBarNotif>;
    }
    return null;
  }
}
