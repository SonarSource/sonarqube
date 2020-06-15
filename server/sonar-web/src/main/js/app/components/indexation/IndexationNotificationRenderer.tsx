/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { Link } from 'react-router';
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { BackgroundTaskTypes } from '../../../apps/background-tasks/constants';
import { IndexationProgression } from './IndexationNotification';

export interface IndexationNotificationRendererProps {
  progression: IndexationProgression;
  percentCompleted: number;
  onDismissCompletedNotification: VoidFunction;
  displayBackgroundTaskLink?: boolean;
}

export default function IndexationNotificationRenderer(props: IndexationNotificationRendererProps) {
  const { progression, percentCompleted, displayBackgroundTaskLink } = props;

  const inProgress = progression === IndexationProgression.InProgress;

  return (
    <div className="indexation-notification-wrapper">
      <Alert
        className="indexation-notification-banner"
        display="banner"
        variant={inProgress ? 'warning' : 'success'}>
        <div className="display-flex-center">
          {inProgress ? (
            <>
              <span>{translate('indexation.in_progress')}</span>
              <i className="spinner spacer-left" />
              <span className="spacer-left">
                {translateWithParameters('indexation.in_progress.details', percentCompleted)}
              </span>
              {displayBackgroundTaskLink && (
                <span className="spacer-left">
                  <FormattedMessage
                    id="indexation.in_progress.admin_details"
                    defaultMessage={translate('indexation.in_progress.admin_details')}
                    values={{
                      link: (
                        <Link
                          to={{
                            pathname: '/admin/background_tasks',
                            query: { taskType: BackgroundTaskTypes.IssueSync }
                          }}>
                          {translate('background_tasks.page')}
                        </Link>
                      )
                    }}
                  />
                </span>
              )}
            </>
          ) : (
            <>
              <span>{translate('indexation.completed')}</span>
              <ButtonLink className="spacer-left" onClick={props.onDismissCompletedNotification}>
                <strong>{translate('dismiss')}</strong>
              </ButtonLink>
            </>
          )}
        </div>
      </Alert>
    </div>
  );
}
