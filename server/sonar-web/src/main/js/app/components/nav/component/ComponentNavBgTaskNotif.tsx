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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { STATUSES } from '../../../../apps/background-tasks/constants';
import Link from '../../../../components/common/Link';
import { Location, withRouter } from '../../../../components/hoc/withRouter';
import { Alert } from '../../../../components/ui/Alert';
import { hasMessage, translate } from '../../../../helpers/l10n';
import { getComponentBackgroundTaskUrl } from '../../../../helpers/urls';
import { Task, TaskStatuses } from '../../../../types/tasks';
import { Component } from '../../../../types/types';
import ComponentNavLicenseNotif from './ComponentNavLicenseNotif';

interface Props {
  component: Component;
  currentTask?: Task;
  currentTaskOnSameBranch?: boolean;
  isInProgress?: boolean;
  isPending?: boolean;
  location: Location;
}

export class ComponentNavBgTaskNotif extends React.PureComponent<Props> {
  renderMessage(messageKey: string, status?: string, branch?: string) {
    const { component, currentTask, location } = this.props;
    const backgroundTaskUrl = getComponentBackgroundTaskUrl(component.key, status);
    const canSeeBackgroundTasks =
      component.configuration && component.configuration.showBackgroundTasks;
    const isOnBackgroundTaskPage = location.pathname === backgroundTaskUrl.pathname;

    let type;
    if (currentTask && hasMessage('background_task.type', currentTask.type)) {
      messageKey += '_X';
      type = translate('background_task.type', currentTask.type);
    }

    let url;
    let stacktrace;
    if (canSeeBackgroundTasks) {
      messageKey += '.admin';

      if (isOnBackgroundTaskPage) {
        messageKey += '.help';
        stacktrace = translate('background_tasks.show_stacktrace');
      } else {
        messageKey += '.link';
        url = <Link to={backgroundTaskUrl}>{translate('background_tasks.page')}</Link>;
      }
    }

    return (
      <FormattedMessage
        defaultMessage={translate(messageKey)}
        id={messageKey}
        values={{ branch, url, stacktrace, type }}
      />
    );
  }

  render() {
    const { currentTask, currentTaskOnSameBranch, isInProgress, isPending } = this.props;
    if (isInProgress) {
      return (
        <Alert display="banner" variant="info">
          {this.renderMessage('component_navigation.status.in_progress')}
        </Alert>
      );
    } else if (isPending) {
      return (
        <Alert display="banner" variant="info">
          {this.renderMessage('component_navigation.status.pending', STATUSES.ALL)}
        </Alert>
      );
    } else if (currentTask && currentTask.status === TaskStatuses.Failed) {
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

      return (
        <Alert className="null-spacer-bottom" display="banner" variant="error">
          {message}
        </Alert>
      );
    }
    return null;
  }
}

export default withRouter(ComponentNavBgTaskNotif);
