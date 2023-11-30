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
import WarningIcon from '../../components/icons/WarningIcon';
import { Alert } from '../../components/ui/Alert';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { AlmSyncStatus } from '../../types/provisioning';
import { TaskStatuses } from '../../types/tasks';
import './SystemAnnouncement.css';

interface SynchronisationWarningProps {
  short?: boolean;
  data: AlmSyncStatus;
}

interface LastSyncProps {
  short?: boolean;
  info: AlmSyncStatus['lastSync'];
}

function LastSyncAlert({ info, short }: Readonly<LastSyncProps>) {
  if (info === undefined) {
    return null;
  }
  const { finishedAt, errorMessage, status, summary, warningMessage } = info;

  const formattedDate = finishedAt ? formatDistance(new Date(finishedAt), new Date()) : '';

  if (short) {
    return status === TaskStatuses.Success ? (
      <div>
        <span className="authentication-enabled spacer-left">
          {warningMessage ? (
            <WarningIcon className="spacer-right" />
          ) : (
            <CheckIcon className="spacer-right" />
          )}
        </span>
        <i>
          {warningMessage ? (
            <FormattedMessage
              id="settings.authentication.github.synchronization_successful.with_warning"
              defaultMessage={translate(
                'settings.authentication.github.synchronization_successful.with_warning',
              )}
              values={{
                date: formattedDate,
                details: (
                  <Link to="/admin/settings?category=authentication&tab=github">
                    {translate('settings.authentication.github.synchronization_details_link')}
                  </Link>
                ),
              }}
            />
          ) : (
            translateWithParameters(
              'settings.authentication.github.synchronization_successful',
              formattedDate,
            )
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
              <Link to="/admin/settings?category=authentication&tab=github">
                {translate('settings.authentication.github.synchronization_details_link')}
              </Link>
            ),
          }}
        />
      </Alert>
    );
  }

  return (
    <>
      <Alert
        variant={status === TaskStatuses.Success ? 'success' : 'error'}
        role="alert"
        aria-live="assertive"
      >
        {status === TaskStatuses.Success ? (
          <>
            {translateWithParameters(
              'settings.authentication.github.synchronization_successful',
              formattedDate,
            )}
            <br />
            {summary ?? ''}
          </>
        ) : (
          <React.Fragment key={`synch-alert-${finishedAt}`}>
            <div>
              {translateWithParameters(
                'settings.authentication.github.synchronization_failed',
                formattedDate,
              )}
            </div>
            <br />
            {errorMessage ?? ''}
          </React.Fragment>
        )}
      </Alert>
      <Alert variant="warning" role="alert" aria-live="assertive">
        {warningMessage}
      </Alert>
    </>
  );
}

export default function AlmSynchronisationWarning({
  short,
  data,
}: Readonly<SynchronisationWarningProps>) {
  return (
    <>
      <Alert
        variant="loading"
        className="spacer-bottom"
        aria-atomic
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
              : 'settings.authentication.github.synchronization_in_progress',
          )}
      </Alert>

      <LastSyncAlert short={short} info={data.lastSync} />
    </>
  );
}
