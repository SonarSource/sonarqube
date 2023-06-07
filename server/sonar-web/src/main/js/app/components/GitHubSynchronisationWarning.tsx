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
import { FormattedMessage } from 'react-intl';
import Link from '../../components/common/Link';
import CheckIcon from '../../components/icons/CheckIcon';
import { Alert } from '../../components/ui/Alert';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { useSyncStatusQuery } from '../../queries/github-sync';
import { GithubStatusEnabled } from '../../types/provisioning';
import { TaskStatuses } from '../../types/tasks';
import './SystemAnnouncement.css';

interface LastSyncProps {
  short?: boolean;
  info: GithubStatusEnabled['lastSync'];
}

interface GitHubSynchronisationWarningProps {
  short?: boolean;
}

function LastSyncAlert({ info, short }: LastSyncProps) {
  if (info === undefined) {
    return null;
  }
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

  return (
    <Alert
      variant={status === TaskStatuses.Success ? 'success' : 'error'}
      role="alert"
      aria-live="assertive"
    >
      {status === TaskStatuses.Success ? (
        <>
          {translateWithParameters(
            'settings.authentication.github.synchronization_successful',
            formattedDate
          )}
          <br />
          {summary ?? ''}
        </>
      ) : (
        <React.Fragment key={`synch-alert-${finishedAt}`}>
          <div>
            {translateWithParameters(
              'settings.authentication.github.synchronization_failed',
              formattedDate
            )}
          </div>
          <br />
          {errorMessage ?? ''}
        </React.Fragment>
      )}
    </Alert>
  );
}

function GitHubSynchronisationWarning({ short }: GitHubSynchronisationWarningProps) {
  const { data } = useSyncStatusQuery();

  if (!data) {
    return null;
  }

  return (
    <>
      <Alert
        variant="loading"
        className="spacer-bottom"
        aria-atomic={true}
        role="alert"
        aria-live="assertive"
        aria-label={
          data.nextSync === undefined
            ? translate('settings.authentication.github.synchronization_finish')
            : ''
        }
      >
        {!short &&
          data?.nextSync &&
          translate(
            data.nextSync.status === TaskStatuses.Pending
              ? 'settings.authentication.github.synchronization_pending'
              : 'settings.authentication.github.synchronization_in_progress'
          )}
      </Alert>

      <LastSyncAlert short={short} info={data.lastSync} />
    </>
  );
}

export default GitHubSynchronisationWarning;
