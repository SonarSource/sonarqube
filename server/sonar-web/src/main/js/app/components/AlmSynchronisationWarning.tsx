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
import { formatDistance } from 'date-fns';
import { CheckIcon, FlagMessage, FlagWarningIcon, Link, Spinner, themeColor } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { AlmSyncStatus } from '../../types/provisioning';
import { TaskStatuses } from '../../types/tasks';

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
              id="settings.authentication.github.synchronization_successful.with_warning"
              defaultMessage={translate(
                'settings.authentication.github.synchronization_successful.with_warning',
              )}
              values={{
                date: formattedDate,
                details: (
                  <Link className="sw-ml-2" to="/admin/settings?category=authentication&tab=github">
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
      <FlagMessage variant="error">
        <div>
          <FormattedMessage
            id="settings.authentication.github.synchronization_failed_short"
            defaultMessage={translate(
              'settings.authentication.github.synchronization_failed_short',
            )}
            values={{
              details: (
                <Link className="sw-ml-2" to="/admin/settings?category=authentication&tab=github">
                  {translate('settings.authentication.github.synchronization_details_link')}
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
        variant={status === TaskStatuses.Success ? 'success' : 'error'}
        role="alert"
        aria-live="assertive"
      >
        <div>
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
        </div>
      </FlagMessage>
      <FlagMessage variant="warning" role="alert" aria-live="assertive">
        {warningMessage}
      </FlagMessage>
    </>
  );
}

export default function AlmSynchronisationWarning({
  short,
  data,
}: Readonly<SynchronisationWarningProps>) {
  const loadingLabel =
    data.nextSync &&
    translate(
      data.nextSync.status === TaskStatuses.Pending
        ? 'settings.authentication.github.synchronization_pending'
        : 'settings.authentication.github.synchronization_in_progress',
    );
  return (
    <>
      {!short && (
        <div className={data.nextSync ? 'sw-flex sw-gap-2 sw-mb-4' : ''}>
          <Spinner loading={!!data.nextSync} ariaLabel={loadingLabel} />
          <div>{data.nextSync && loadingLabel}</div>
        </div>
      )}

      <LastSyncAlert short={short} info={data.lastSync} />
    </>
  );
}

const IconWrapper = styled.span`
  color: ${themeColor('iconSuccess')};
`;
