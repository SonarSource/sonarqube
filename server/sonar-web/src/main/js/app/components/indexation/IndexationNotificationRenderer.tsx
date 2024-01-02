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
/* eslint-disable react/no-unused-prop-types */

import classNames from 'classnames';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocLink from '../../../components/common/DocLink';
import Link from '../../../components/common/Link';
import { Alert, AlertProps } from '../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { queryToSearch } from '../../../helpers/urls';
import { IndexationNotificationType } from '../../../types/indexation';
import { TaskStatuses, TaskTypes } from '../../../types/tasks';

export interface IndexationNotificationRendererProps {
  completedCount?: number;
  total?: number;
  type?: IndexationNotificationType;
}

const NOTIFICATION_VARIANTS: { [key in IndexationNotificationType]: AlertProps['variant'] } = {
  [IndexationNotificationType.InProgress]: 'warning',
  [IndexationNotificationType.InProgressWithFailure]: 'error',
  [IndexationNotificationType.Completed]: 'success',
  [IndexationNotificationType.CompletedWithFailure]: 'error',
};

export default function IndexationNotificationRenderer(props: IndexationNotificationRendererProps) {
  const { completedCount, total, type } = props;

  return (
    <div className={classNames({ 'indexation-notification-wrapper': type })}>
      <Alert
        className="indexation-notification-banner"
        display="banner"
        variant={type ? NOTIFICATION_VARIANTS[type] : 'success'}
        aria-live="assertive"
      >
        {type !== undefined && (
          <div className="display-flex-center">
            {type === IndexationNotificationType.Completed && renderCompletedBanner()}

            {type === IndexationNotificationType.CompletedWithFailure &&
              renderCompletedWithFailureBanner()}

            {type === IndexationNotificationType.InProgress &&
              renderInProgressBanner(completedCount as number, total as number)}

            {type === IndexationNotificationType.InProgressWithFailure &&
              renderInProgressWithFailureBanner(completedCount as number, total as number)}
          </div>
        )}
      </Alert>
    </div>
  );
}

function renderCompletedBanner() {
  return <span className="spacer-right">{translate('indexation.completed')}</span>;
}

function renderCompletedWithFailureBanner() {
  return (
    <span className="spacer-right">
      <FormattedMessage
        defaultMessage={translate('indexation.completed_with_error')}
        id="indexation.completed_with_error"
        values={{
          link: renderBackgroundTasksPageLink(
            true,
            translate('indexation.completed_with_error.link'),
          ),
        }}
      />
    </span>
  );
}

function renderInProgressBanner(completedCount: number, total: number) {
  return (
    <>
      <span className="spacer-right">
        <FormattedMessage id="indexation.in_progress" />{' '}
        <FormattedMessage
          id="indexation.features_partly_available"
          values={{
            link: renderIndexationDocPageLink(),
          }}
        />
      </span>
      <i className="spinner spacer-right" />

      <span className="spacer-right">
        {translateWithParameters(
          'indexation.progression',
          completedCount.toString(),
          total.toString(),
        )}
      </span>

      <span className="spacer-right">
        <FormattedMessage
          id="indexation.admin_link"
          defaultMessage={translate('indexation.admin_link')}
          values={{
            link: renderBackgroundTasksPageLink(false, translate('background_tasks.page')),
          }}
        />
      </span>
    </>
  );
}

function renderInProgressWithFailureBanner(completedCount: number, total: number) {
  return (
    <>
      <span className="spacer-right">
        <FormattedMessage id="indexation.in_progress" />{' '}
        <FormattedMessage
          id="indexation.features_partly_available"
          values={{
            link: renderIndexationDocPageLink(),
          }}
        />
      </span>
      <i className="spinner spacer-right" />

      <span className="spacer-right">
        <FormattedMessage
          id="indexation.progression_with_error"
          defaultMessage={translateWithParameters(
            'indexation.progression_with_error',
            completedCount.toString(),
            total.toString(),
          )}
          values={{
            link: renderBackgroundTasksPageLink(
              true,
              translate('indexation.progression_with_error.link'),
            ),
          }}
        />
      </span>
    </>
  );
}

function renderBackgroundTasksPageLink(hasError: boolean, text: string) {
  return (
    <Link
      to={{
        pathname: '/admin/background_tasks',
        search: queryToSearch({
          taskType: TaskTypes.IssueSync,
          status: hasError ? TaskStatuses.Failed : undefined,
        }),
      }}
    >
      {text}
    </Link>
  );
}

function renderIndexationDocPageLink() {
  return (
    <DocLink to="/instance-administration/reindexing/">
      <FormattedMessage id="indexation.features_partly_available.link" />
    </DocLink>
  );
}
