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
import {
  IconCheckCircle,
  IconError,
  IconWarning,
  IconX,
  Link,
  LinkHighlight,
  Spinner,
} from '@sonarsource/echoes-react';
import { FormattedMessage, useIntl } from 'react-intl';
import { InteractiveIconBase, ThemeColors, themeBorder, themeColor } from '~design-system';
import { queryToSearchString } from '~sonar-aligned/helpers/urls';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import { IndexationNotificationType } from '../../../types/indexation';
import { TaskStatuses, TaskTypes } from '../../../types/tasks';

interface IndexationNotificationRendererProps {
  completedCount?: number;
  onDismissBanner: () => void;
  shouldDisplaySurveyLink: boolean;
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

const SPRIG_SURVEY_LINK =
  'https://a.sprig.com/U1h4UFpySUNwN2ZtfnNpZDowNWUyNmRkZC01MmUyLTQ4OGItOTA3ZC05M2VjYjQxZTYzN2Y=';

export default function IndexationNotificationRenderer(
  props: Readonly<IndexationNotificationRendererProps>,
) {
  const { type } = props;

  return (
    <div className={type === undefined ? 'sw-hidden' : ''}>
      <StyledBanner
        className="sw-typo-default sw-py-3 sw-px-4 sw-gap-4"
        type={type ?? IndexationNotificationType.Completed}
        aria-live="assertive"
        role="alert"
      >
        <IndexationStatusIcon type={type} />
        <IndexationBanner {...props} />
      </StyledBanner>
    </div>
  );
}

function IndexationStatusIcon(props: Readonly<{ type?: IndexationNotificationType }>) {
  const { type } = props;

  switch (type) {
    case IndexationNotificationType.Completed:
      return <IconCheckCircle color="echoes-color-icon-success" />;
    case IndexationNotificationType.CompletedWithFailure:
    case IndexationNotificationType.InProgressWithFailure:
      return <IconError color="echoes-color-icon-danger" />;
    case IndexationNotificationType.InProgress:
      return <IconWarning color="echoes-color-icon-warning" />;
    default:
      return null;
  }
}

function IndexationBanner(props: Readonly<IndexationNotificationRendererProps>) {
  const { completedCount, onDismissBanner, shouldDisplaySurveyLink, total, type } = props;

  switch (type) {
    case IndexationNotificationType.Completed:
      return (
        <CompletedBanner
          onDismissBanner={onDismissBanner}
          shouldDisplaySurveyLink={shouldDisplaySurveyLink}
        />
      );
    case IndexationNotificationType.CompletedWithFailure:
      return <CompletedWithFailureBanner shouldDisplaySurveyLink={shouldDisplaySurveyLink} />;
    case IndexationNotificationType.InProgress:
      return <InProgressBanner completedCount={completedCount as number} total={total as number} />;
    case IndexationNotificationType.InProgressWithFailure:
      return (
        <InProgressWithFailureBanner
          completedCount={completedCount as number}
          total={total as number}
        />
      );
    default:
      return null;
  }
}

function SurveyLink() {
  return (
    <span className="sw-ml-2">
      <FormattedMessage
        id="indexation.upgrade_survey_link"
        values={{
          link: (text) => (
            <Link highlight={LinkHighlight.Default} shouldOpenInNewTab to={SPRIG_SURVEY_LINK}>
              {text}
            </Link>
          ),
        }}
      />
    </span>
  );
}

function CompletedBanner(
  props: Readonly<{ onDismissBanner: () => void; shouldDisplaySurveyLink: boolean }>,
) {
  const { onDismissBanner, shouldDisplaySurveyLink } = props;

  const intl = useIntl();

  return (
    <div className="sw-flex sw-flex-1 sw-items-center">
      <FormattedMessage id="indexation.completed" />
      {shouldDisplaySurveyLink && <SurveyLink />}
      <div className="sw-flex sw-flex-1 sw-justify-end">
        <BannerDismissIcon
          className="sw-ml-2 sw-px-1/2"
          Icon={IconX}
          aria-label={intl.formatMessage({ id: 'dismiss' })}
          onClick={onDismissBanner}
          size="small"
        />
      </div>
    </div>
  );
}

function CompletedWithFailureBanner(props: Readonly<{ shouldDisplaySurveyLink: boolean }>) {
  const { shouldDisplaySurveyLink } = props;

  const { formatMessage } = useIntl();

  return (
    <span>
      <FormattedMessage
        defaultMessage={translate('indexation.completed_with_error')}
        id="indexation.completed_with_error"
        values={{
          link: (
            <BackgroundTasksPageLink
              hasError
              text={formatMessage({ id: 'indexation.completed_with_error.link' })}
            />
          ),
        }}
      />
      {shouldDisplaySurveyLink && <SurveyLink />}
    </span>
  );
}

function InProgressBanner(props: Readonly<{ completedCount: number; total: number }>) {
  const { completedCount, total } = props;

  const { formatMessage } = useIntl();

  return (
    <>
      <span>
        <FormattedMessage id="indexation.in_progress" />{' '}
        <FormattedMessage
          id="indexation.features_partly_available"
          values={{
            link: <IndexationDocPageLink />,
          }}
        />
      </span>

      <span className="sw-flex sw-items-center">
        <Spinner className="sw-mr-1 -sw-mb-1/2" />
        <FormattedMessage
          id="indexation.progression"
          values={{
            count: completedCount,
            total,
          }}
        />
      </span>

      <span>
        <FormattedMessage
          id="indexation.admin_link"
          values={{
            link: (
              <BackgroundTasksPageLink
                hasError={false}
                text={formatMessage({ id: 'background_tasks.page' })}
              />
            ),
          }}
        />
      </span>
    </>
  );
}

function InProgressWithFailureBanner(props: Readonly<{ completedCount: number; total: number }>) {
  const { completedCount, total } = props;

  const { formatMessage } = useIntl();

  return (
    <>
      <span>
        <FormattedMessage id="indexation.in_progress" />{' '}
        <FormattedMessage
          id="indexation.features_partly_available"
          values={{
            link: <IndexationDocPageLink />,
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
            link: (
              <BackgroundTasksPageLink
                hasError
                text={formatMessage({ id: 'indexation.progression_with_error.link' })}
              />
            ),
          }}
        />
      </span>
    </>
  );
}

function BackgroundTasksPageLink(props: Readonly<{ hasError: boolean; text: string }>) {
  const { hasError, text } = props;

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

function IndexationDocPageLink() {
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

// There's currently no banner in Echoes so let's use the legacy design-system components for now
const BannerDismissIcon = styled(InteractiveIconBase)`
  --background: ${themeColor('successBackground')};
  --backgroundHover: ${themeColor('successText', 0.1)};
  --color: ${themeColor('successText')};
  --colorHover: ${themeColor('successText')};
  --focus: ${themeColor('bannerIconFocus', 0.2)};
`;
