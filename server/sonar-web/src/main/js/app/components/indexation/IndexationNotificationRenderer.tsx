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

import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import Link from '../../../components/common/Link';
import { Alert, AlertProps } from '../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { queryToSearch } from '../../../helpers/urls';
import { IndexationNotificationType } from '../../../types/indexation';
import { TaskStatuses, TaskTypes } from '../../../types/tasks';

export interface IndexationNotificationRendererProps {
  type: IndexationNotificationType;
  percentCompleted: number;
  isSystemAdmin: boolean;
}

const NOTIFICATION_VARIANTS: { [key in IndexationNotificationType]: AlertProps['variant'] } = {
  [IndexationNotificationType.InProgress]: 'warning',
  [IndexationNotificationType.InProgressWithFailure]: 'error',
  [IndexationNotificationType.Completed]: 'success',
  [IndexationNotificationType.CompletedWithFailure]: 'error',
};

export default function IndexationNotificationRenderer(props: IndexationNotificationRendererProps) {
  const { type } = props;

  return (
    <div className="indexation-notification-wrapper">
      <Alert
        className="indexation-notification-banner"
        display="banner"
        variant={NOTIFICATION_VARIANTS[type]}
      >
        <div className="display-flex-center">
          {type === IndexationNotificationType.Completed && renderCompletedBanner(props)}
          {type === IndexationNotificationType.CompletedWithFailure &&
            renderCompletedWithFailureBanner(props)}
          {type === IndexationNotificationType.InProgress && renderInProgressBanner(props)}
          {type === IndexationNotificationType.InProgressWithFailure &&
            renderInProgressWithFailureBanner(props)}
        </div>
      </Alert>
    </div>
  );
}

function renderCompletedBanner(_props: IndexationNotificationRendererProps) {
  return <span className="spacer-right">{translate('indexation.completed')}</span>;
}

function renderCompletedWithFailureBanner(props: IndexationNotificationRendererProps) {
  const { isSystemAdmin } = props;

  return (
    <span className="spacer-right">
      <FormattedMessage
        id="indexation.completed_with_error"
        defaultMessage={translate('indexation.completed_with_error')}
        values={{
          link: isSystemAdmin
            ? renderBackgroundTasksPageLink(true, translate('indexation.completed_with_error.link'))
            : translate('indexation.completed_with_error.link'),
        }}
      />
    </span>
  );
}

function renderInProgressBanner(props: IndexationNotificationRendererProps) {
  const { percentCompleted, isSystemAdmin } = props;

  return (
    <>
      <span className="spacer-right">{translate('indexation.in_progress')}</span>
      <i className="spinner spacer-right" />
      <span className="spacer-right">
        {translateWithParameters('indexation.progression', percentCompleted)}
      </span>
      {isSystemAdmin && (
        <span className="spacer-right">
          <FormattedMessage
            id="indexation.admin_link"
            defaultMessage={translate('indexation.admin_link')}
            values={{
              link: renderBackgroundTasksPageLink(false, translate('background_tasks.page')),
            }}
          />
        </span>
      )}
    </>
  );
}

function renderInProgressWithFailureBanner(props: IndexationNotificationRendererProps) {
  const { percentCompleted, isSystemAdmin } = props;

  return (
    <>
      <span className="spacer-right">{translate('indexation.in_progress')}</span>
      <i className="spinner spacer-right" />
      <span className="spacer-right">
        <FormattedMessage
          id="indexation.progression_with_error"
          defaultMessage={translateWithParameters(
            'indexation.progression_with_error',
            percentCompleted
          )}
          values={{
            link: isSystemAdmin
              ? renderBackgroundTasksPageLink(
                  true,
                  translate('indexation.progression_with_error.link')
                )
              : translate('indexation.progression_with_error.link'),
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
