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

import styled from '@emotion/styled';
import { Link, Spinner } from '@sonarsource/echoes-react';
import { formatDistance } from 'date-fns';
import { CheckIcon, FlagMessage, FlagWarningIcon, themeColor } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { AlmKeys } from '../../types/alm-settings';
import { AlmSyncStatus } from '../../types/provisioning';
import { TaskStatuses } from '../../types/tasks';

interface SynchronisationWarningProps {
  data: AlmSyncStatus;
  provisionedBy: AlmKeys.GitHub | AlmKeys.GitLab;
  short?: boolean;
}

interface LastSyncProps {
  info: AlmSyncStatus['lastSync'];
  provisionedBy: AlmKeys.GitHub | AlmKeys.GitLab;
  short?: boolean;
}

function LastSyncAlert({ info, provisionedBy, short }: Readonly<LastSyncProps>) {
  if (info === undefined) {
    return null;
  }

  const { finishedAt, errorMessage, status, summary, warningMessage } = info;

  const formattedDate = finishedAt ? formatDistance(new Date(finishedAt), new Date()) : '';

  if (short) {
    return status === TaskStatuses.Success ? (
      <div>
        <IconWrapper className="sw-ml-2">
          {warningMessage ? (
            <FlagWarningIcon className="sw-mr-2" />
          ) : (
            <CheckIcon width={32} height={32} className="sw-mr-2" />
          )}
        </IconWrapper>

        <i>
          {warningMessage ? (
            <FormattedMessage
              defaultMessage={translate(
                'settings.authentication.synchronization_successful.with_warning',
              )}
              id="settings.authentication.synchronization_successful.with_warning"
              values={{
                date: formattedDate,
                details: (
                  <Link
                    className="sw-ml-2"
                    to={`/admin/settings?category=authentication&tab=${provisionedBy}`}
                  >
                    {translate('settings.authentication.synchronization_details_link')}
                  </Link>
                ),
              }}
            />
          ) : (
            translateWithParameters(
              'settings.authentication.synchronization_successful',
              formattedDate,
            )
          )}
        </i>
      </div>
    ) : (
      <FlagMessage variant="error">
        <div>
          <FormattedMessage
            defaultMessage={translate('settings.authentication.synchronization_failed_short')}
            id="settings.authentication.synchronization_failed_short"
            values={{
              details: (
                <Link
                  className="sw-ml-2"
                  to={`/admin/settings?category=authentication&tab=${provisionedBy}`}
                >
                  {translate('settings.authentication.synchronization_details_link')}
                </Link>
              ),
            }}
          />
        </div>
      </FlagMessage>
    );
  }

  return (
    <>
      <FlagMessage
        aria-live="assertive"
        role="alert"
        variant={status === TaskStatuses.Success ? 'success' : 'error'}
      >
        <div>
          {status === TaskStatuses.Success ? (
            <>
              {translateWithParameters(
                'settings.authentication.synchronization_successful',
                formattedDate,
              )}

              <br />

              {summary ?? ''}
            </>
          ) : (
            <React.Fragment key={`synch-alert-${finishedAt}`}>
              <div>
                {translateWithParameters(
                  'settings.authentication.synchronization_failed',
                  formattedDate,
                )}
              </div>

              <br />

              {errorMessage ?? ''}
            </React.Fragment>
          )}
        </div>
      </FlagMessage>

      <FlagMessage variant="warning" role="alert" aria-live="assertive">
        {warningMessage}
      </FlagMessage>
    </>
  );
}

export default function AlmSynchronisationWarning(props: Readonly<SynchronisationWarningProps>) {
  const { data, provisionedBy, short } = props;
  const loadingLabel =
    data.nextSync &&
    translate(
      data.nextSync.status === TaskStatuses.Pending
        ? 'settings.authentication.synchronization_pending'
        : 'settings.authentication.synchronization_in_progress',
    );

  return (
    <>
      {!short && (
        <div className={data.nextSync ? 'sw-flex sw-gap-2 sw-mb-4' : ''}>
          <Spinner ariaLabel={loadingLabel} isLoading={!!data.nextSync} />

          <div>{data.nextSync && loadingLabel}</div>
        </div>
      )}

      <LastSyncAlert short={short} info={data.lastSync} provisionedBy={provisionedBy} />
    </>
  );
}

const IconWrapper = styled.span`
  color: ${themeColor('iconSuccess')};
`;
