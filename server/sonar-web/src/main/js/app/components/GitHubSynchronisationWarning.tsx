/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { isEmpty } from 'lodash';
import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import { WrappedComponentProps, injectIntl } from 'react-intl';
import { getActivity } from '../../api/ce';
import { formatterOption } from '../../components/intl/DateTimeFormatter';
import { Alert, AlertVariant } from '../../components/ui/Alert';
import { translateWithParameters } from '../../helpers/l10n';
import { Feature } from '../../types/features';
import { ActivityRequestParameters, TaskStatuses, TaskTypes } from '../../types/tasks';
import './SystemAnnouncement.css';
import { AvailableFeaturesContext } from './available-features/AvailableFeaturesContext';

function GitHubSynchronisationWarning(props: WrappedComponentProps) {
  const { formatDate } = props.intl;
  const [displayMessage, setDisplayMessage] = useState(false);
  const [activityStatus, setActivityStatus] = useState<AlertVariant>('info');
  const [message, setMessage] = useState('');
  const hasGithubProvisioning = useContext(AvailableFeaturesContext).includes(
    Feature.GithubProvisioning
  );

  useEffect(() => {
    (async () => {
      const lastActivity = await getLatestGithubSynchronisationTask();

      if (lastActivity === undefined) {
        return;
      }
      const { status, errorMessage, executedAt } = lastActivity;

      if (executedAt === undefined) {
        return;
      }
      const formattedDate = formatDate(new Date(executedAt), formatterOption);

      switch (status) {
        case TaskStatuses.Failed:
          setMessage(
            translateWithParameters(
              'settings.authentication.github.background_task.synchronization_failed',
              formattedDate,
              errorMessage ?? ''
            )
          );
          setActivityStatus('error');
          break;
        case TaskStatuses.Success:
          setMessage(
            translateWithParameters(
              'settings.authentication.github.background_task.synchronization_successful',
              formattedDate
            )
          );
          setActivityStatus('success');
          break;
        case TaskStatuses.InProgress:
          setMessage(
            translateWithParameters(
              'settings.authentication.github.background_task.synchronization_in_progress',
              formattedDate
            )
          );
          setActivityStatus('loading');
          break;
        default:
          return;
      }
      setDisplayMessage(true);
    })();
  }, []);

  if (!displayMessage || !hasGithubProvisioning) {
    return null;
  }

  return (
    <Alert title={message} variant={activityStatus}>
      {message}
    </Alert>
  );
}

const getLatestGithubSynchronisationTask = async () => {
  const data: ActivityRequestParameters = {
    type: TaskTypes.GithubProvisioning,
    onlyCurrents: true,
    status: [TaskStatuses.InProgress, TaskStatuses.Success, TaskStatuses.Failed].join(','),
  };
  const activity = await getActivity(data);

  if (isEmpty(activity.tasks)) {
    return undefined;
  }

  return activity.tasks[0];
};

export default injectIntl(GitHubSynchronisationWarning);
