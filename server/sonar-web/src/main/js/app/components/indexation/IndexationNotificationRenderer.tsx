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
import styled from '@emotion/styled';
import { FormattedMessage } from 'react-intl';
import {
  FlagErrorIcon,
  FlagSuccessIcon,
  FlagWarningIcon,
  Link,
  Spinner,
  ThemeColors,
  themeBorder,
  themeColor,
} from '~design-system';
import { queryToSearchString } from '~sonar-aligned/helpers/urls';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { IndexationNotificationType } from '../../../types/indexation';
import { TaskStatuses, TaskTypes } from '../../../types/tasks';

export interface IndexationNotificationRendererProps {
  completedCount?: number;
  total?: number;
  type?: IndexationNotificationType;
}

const NOTIFICATION_COLORS: {
  [key in IndexationNotificationType]: { background: ThemeColors; border: ThemeColors };
} = {
  [IndexationNotificationType.InProgress]: {
    background: 'warningBackground',
    border: 'warningBorder',
  },
  [IndexationNotificationType.InProgressWithFailure]: {
    background: 'errorBackground',
    border: 'errorBorder',
  },
  [IndexationNotificationType.Completed]: {
    background: 'successBackground',
    border: 'successBorder',
  },
  [IndexationNotificationType.CompletedWithFailure]: {
    background: 'errorBackground',
    border: 'errorBorder',
  },
};

export default function IndexationNotificationRenderer(props: IndexationNotificationRendererProps) {
  const { completedCount, total, type } = props;

  return (
    <div className={type === undefined ? 'sw-hidden' : ''}>
      <StyledBanner
        className="sw-typo-default sw-py-3 sw-px-4 sw-gap-4"
        type={type ?? IndexationNotificationType.Completed}
        aria-live="assertive"
        role="alert"
      >
        {type !== undefined && (
          <>
            {renderIcon(type)}
            {type === IndexationNotificationType.Completed && renderCompletedBanner()}

            {type === IndexationNotificationType.CompletedWithFailure &&
              renderCompletedWithFailureBanner()}

            {type === IndexationNotificationType.InProgress &&
              renderInProgressBanner(completedCount as number, total as number)}

            {type === IndexationNotificationType.InProgressWithFailure &&
              renderInProgressWithFailureBanner(completedCount as number, total as number)}
          </>
        )}
      </StyledBanner>
    </div>
  );
}

function renderIcon(type: IndexationNotificationType) {
  switch (type) {
    case IndexationNotificationType.Completed:
      return <FlagSuccessIcon />;
    case IndexationNotificationType.CompletedWithFailure:
    case IndexationNotificationType.InProgressWithFailure:
      return <FlagErrorIcon />;
    case IndexationNotificationType.InProgress:
      return <FlagWarningIcon />;
    default:
      return null;
  }
}

function renderCompletedBanner() {
  return <span>{translate('indexation.completed')}</span>;
}

function renderCompletedWithFailureBanner() {
  return (
    <span>
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
      <span>
        <FormattedMessage id="indexation.in_progress" />{' '}
        <FormattedMessage
          id="indexation.features_partly_available"
          values={{
            link: renderIndexationDocPageLink(),
          }}
        />
      </span>

      <span className="sw-flex sw-items-center">
        <Spinner className="sw-mr-1 -sw-mb-1/2" />
        {translateWithParameters(
          'indexation.progression',
          completedCount.toString(),
          total.toString(),
        )}
      </span>

      <span>
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
      <span>
        <FormattedMessage id="indexation.in_progress" />{' '}
        <FormattedMessage
          id="indexation.features_partly_available"
          values={{
            link: renderIndexationDocPageLink(),
          }}
        />
      </span>

      <span className="sw-flex sw-items-center">
        <Spinner className="sw-mr-1 -sw-mb-1/2" />
        <FormattedMessage
          tagName="span"
          id="indexation.progression_with_error"
          values={{
            count: completedCount,
            total,
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
        search: queryToSearchString({
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
    <DocumentationLink className="sw-whitespace-nowrap" to={DocLink.InstanceAdminReindexation}>
      <FormattedMessage id="indexation.features_partly_available.link" />
    </DocumentationLink>
  );
}

const StyledBanner = styled.div<{ type: IndexationNotificationType }>`
  display: flex;
  align-items: center;
  box-sizing: border-box;
  width: 100%;

  background-color: ${({ type }) => themeColor(NOTIFICATION_COLORS[type].background)};
  border-bottom: ${({ type }) => themeBorder('default', NOTIFICATION_COLORS[type].border)};
`;
