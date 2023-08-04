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
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { dismissNotice } from '../../../api/users';
import { CurrentUserContext } from '../../../app/components/current-user/CurrentUserContext';
import DocLink from '../../../components/common/DocLink';
import { Guide } from '../../../components/common/Guide';
import { translate } from '../../../helpers/l10n';
import { NoticeType } from '../../../types/users';

interface Props {
  run?: boolean;
}

export default function IssueListGuide({ run }: Props) {
  const { currentUser, updateDismissedNotices } = React.useContext(CurrentUserContext);

  if (!currentUser.isLoggedIn || currentUser.dismissedNotices[NoticeType.ISSUE_GUIDE]) {
    return null;
  }

  const onToggle = (props: { action: string }) => {
    if (props.action === 'reset') {
      dismissNotice(NoticeType.ISSUE_GUIDE)
        .then(() => {
          updateDismissedNotices(NoticeType.ISSUE_GUIDE, true);
        })
        .catch(() => {
          /* noop */
        });
    }
  };

  const constructContent = (
    first: string,
    second: string,
    extraContent?: string | React.ReactNode
  ) => (
    <>
      <span>{translate(first)}</span>
      <br />
      <br />
      <span>{translate(second)}</span>
      {extraContent ?? null}
    </>
  );

  const commonStepProps = {
    disableScrolling: true,
    disableBeacon: true,
    floaterProps: {
      disableAnimation: true,
    },
  };

  const steps = [
    {
      target: '[data-guiding-id="issuelist-1"]',
      content: constructContent('guiding.issue_list.1.content.1', 'guiding.issue_list.1.content.2'),
      title: translate('guiding.issue_list.1.title'),
      placement: 'right' as const,
      ...commonStepProps,
    },
    {
      target: '[data-guiding-id="issuelist-2"]',
      content: constructContent('guiding.issue_list.2.content.1', 'guiding.issue_list.2.content.2'),
      title: translate('guiding.issue_list.2.title'),
      placement: 'right' as const,
      ...commonStepProps,
    },
    {
      target: '[data-guiding-id="issuelist-3"]',
      content: constructContent('guiding.issue_list.3.content.1', 'guiding.issue_list.3.content.2'),
      title: translate('guiding.issue_list.3.title'),
      ...commonStepProps,
    },
    {
      target: '[data-guiding-id="issuelist-4"]',
      content: constructContent(
        'guiding.issue_list.4.content.1',
        'guiding.issue_list.4.content.2',
        <ul className="sw-mt-2 sw-pl-5 sw-list-disc">
          <li>{translate('guiding.issue_list.4.content.list.1')}</li>
          <li>{translate('guiding.issue_list.4.content.list.2')}</li>
          <li>{translate('guiding.issue_list.4.content.list.3')}</li>
        </ul>
      ),
      title: translate('guiding.issue_list.4.title'),
      ...commonStepProps,
    },
    {
      target: 'body',
      content: (
        <FormattedMessage
          id="guiding.issue_list.5.content"
          defaultMessage={translate('guiding.issue_list.5.content')}
          values={{
            link: (
              <DocLink to="/user-guide/clean-code" className="sw-capitalize">
                {translate('documentation')}
              </DocLink>
            ),
          }}
        />
      ),
      title: translate('guiding.issue_list.5.title'),
      placement: 'center' as const,
      ...commonStepProps,
    },
  ];

  return <Guide callback={onToggle} steps={steps} run={run} continuous />;
}
