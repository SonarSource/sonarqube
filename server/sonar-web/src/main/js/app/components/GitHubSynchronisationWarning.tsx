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
import { formatDistance } from 'date-fns';
import * as React from 'react';
import { useContext } from 'react';
import { FormattedMessage } from 'react-intl';
import Link from '../../components/common/Link';
import CheckIcon from '../../components/icons/CheckIcon';
import { Alert } from '../../components/ui/Alert';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { useSyncStatusQuery } from '../../queries/github-sync';
import { Feature } from '../../types/features';
import { GithubStatusEnabled } from '../../types/provisioning';
import { TaskStatuses } from '../../types/tasks';
import './SystemAnnouncement.css';
import { AvailableFeaturesContext } from './available-features/AvailableFeaturesContext';

interface LastSyncProps {
  short?: boolean;
  info: Required<GithubStatusEnabled>['lastSync'];
}

interface GitHubSynchronisationWarningProps {
  short?: boolean;
}

function LastSyncAlert({ info, short }: LastSyncProps) {
  const { finishedAt, errorMessage, status, summary } = info;

  const formattedDate = finishedAt ? formatDistance(new Date(finishedAt), new Date()) : '';

  if (short) {
    return status === TaskStatuses.Success ? (
      <div>
        <span className="authentication-enabled spacer-left">
          <CheckIcon className="spacer-right" />
        </span>
        <i>
          {translateWithParameters(
            'settings.authentication.github.synchronization_successful',
            formattedDate
          )}
        </i>
      </div>
    ) : (
      <Alert variant="error">
        <FormattedMessage
          id="settings.authentication.github.synchronization_failed_short"
          defaultMessage={translate('settings.authentication.github.synchronization_failed_short')}
          values={{
            details: (
              <Link to="../settings?category=authentication&tab=github">
                {translate('settings.authentication.github.synchronization_failed_link')}
              </Link>
            ),
          }}
        />
      </Alert>
    );
  }

  return status === TaskStatuses.Success ? (
    <Alert variant="success">
      {translateWithParameters(
        'settings.authentication.github.synchronization_successful',
        formattedDate
      )}
      <br />
      {summary ?? ''}
    </Alert>
  ) : (
    <Alert variant="error">
      <div>
        {translateWithParameters(
          'settings.authentication.github.synchronization_failed',
          formattedDate
        )}
      </div>
      <br />
      {errorMessage ?? ''}
    </Alert>
  );
}

function GitHubSynchronisationWarning({ short }: GitHubSynchronisationWarningProps) {
  const hasGithubProvisioning = useContext(AvailableFeaturesContext).includes(
    Feature.GithubProvisioning
  );
  const { data } = useSyncStatusQuery({ enabled: hasGithubProvisioning });

  if (!data) {
    return null;
  }

  return (
    <>
      {!short && data?.nextSync && (
        <>
          <Alert variant="loading">
            {translate(
              data.nextSync.status === TaskStatuses.Pending
                ? 'settings.authentication.github.synchronization_pending'
                : 'settings.authentication.github.synchronization_in_progress'
            )}
          </Alert>
          <br />
        </>
      )}
      {data?.lastSync && <LastSyncAlert short={short} info={data.lastSync} />}
    </>
  );
}

export default GitHubSynchronisationWarning;
