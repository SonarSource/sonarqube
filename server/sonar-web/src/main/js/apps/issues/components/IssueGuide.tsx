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

import React, { useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { CallBackProps } from 'react-joyride';
import { SpotlightTour, SpotlightTourStep } from '~design-system';
import { dismissNotice } from '../../../api/users';
import { CurrentUserContext } from '../../../app/components/current-user/CurrentUserContext';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { SCREEN_POSITION_COMPUTE_DELAY } from '../../../components/common/ScreenPositionHelper';
import { DocLink } from '../../../helpers/doc-links';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { NoticeType } from '../../../types/users';

interface Props {
  run: boolean;
}

const PLACEMENT_RIGHT = 'right';
const SESSION_STORAGE_KEY = 'issueCleanCodeGuideStep';

const EXTRA_DELAY = 50;

export default function IssueGuide({ run }: Props) {
  const { currentUser, updateDismissedNotices } = React.useContext(CurrentUserContext);
  const [step, setStep] = useState(+(sessionStorage.getItem(SESSION_STORAGE_KEY) ?? 0));
  const canRun = currentUser.isLoggedIn && !currentUser.dismissedNotices[NoticeType.ISSUE_GUIDE];

  // IssueGuide can be called within context of a ScreenPositionHelper. When this happens,
  // React Floater (a lib used by React Joyride, which in turn is what powers SpotlightTour)
  // gets confused and cannot correctly position the first step. The only way around this is
  // to delay the rendering of the SpotlightTour until *after* ScreenPositionHelper has
  // recomputed its positioning. That's what this state + effect is about: if `run` is false,
  // it means we are not in a state to start running. This could either be because we really don't
  // want to start the tour at all, in which case `run` will remain false. OR, it means we are
  // waiting on something else (like ScreenPositionHelper), in which case `run` will turn true
  // later. We wait on the delay of ScreenPositionHelper + 50ms, and try again. If `run` is still
  // false, we don't start the tour. If `run` is now true, we start the tour.
  const [start, setStart] = React.useState(run);
  React.useEffect(() => {
    // Only trigger the timeout if start is false.
    if (!start && canRun) {
      setTimeout(() => {
        setStart(run);
      }, SCREEN_POSITION_COMPUTE_DELAY + EXTRA_DELAY);
    }
  }, [canRun, run, start]);

  React.useEffect(() => {
    if (start && canRun) {
      sessionStorage.setItem(SESSION_STORAGE_KEY, step.toString());
    }
  }, [step, start, canRun]);

  if (!start || !canRun) {
    return null;
  }

  const onToggle = (props: CallBackProps) => {
    switch (props.action) {
      case 'close':
      case 'skip':
      case 'reset':
        sessionStorage.removeItem(SESSION_STORAGE_KEY);
        dismissNotice(NoticeType.ISSUE_GUIDE)
          .then(() => {
            updateDismissedNotices(NoticeType.ISSUE_GUIDE, true);
          })
          .catch(() => {
            /* noop */
          });
        break;
      case 'next':
        if (props.lifecycle === 'complete') {
          setStep(step + 1);
        }
        break;
      case 'prev':
        if (props.lifecycle === 'complete') {
          setStep(step - 1);
        }
        break;
      default:
        break;
    }
  };

  const constructContent = (
    first: string,
    second: string,
    extraContent?: string | React.ReactNode,
  ) => (
    <>
      <span>{translate(first)}</span>
      <br />
      <br />
      <span>{translate(second)}</span>
      {extraContent ?? null}
    </>
  );

  const steps: SpotlightTourStep[] = [
    {
      target: '[data-guiding-id="issue-1"]',
      content: constructContent('guiding.issue_list.1.content.1', 'guiding.issue_list.1.content.2'),
      title: translate('guiding.issue_list.1.title'),
      placement: PLACEMENT_RIGHT,
    },
    {
      target: '[data-guiding-id="issue-2"]',
      content: constructContent('guiding.issue_list.2.content.1', 'guiding.issue_list.2.content.2'),
      title: translate('guiding.issue_list.2.title'),
    },
    {
      target: '[data-guiding-id="issue-3"]',
      content: constructContent('guiding.issue_list.3.content.1', 'guiding.issue_list.3.content.2'),
      title: translate('guiding.issue_list.3.title'),
    },
    {
      target: '[data-guiding-id="issue-4"]',
      content: constructContent(
        'guiding.issue_list.4.content.1',
        'guiding.issue_list.4.content.2',
        <ul className="sw-mt-2 sw-pl-5 sw-list-disc">
          <li>{translate('guiding.issue_list.4.content.list.1')}</li>
          <li>{translate('guiding.issue_list.4.content.list.2')}</li>
          <li>{translate('guiding.issue_list.4.content.list.3')}</li>
        </ul>,
      ),
      title: translate('guiding.issue_list.4.title'),
    },
    {
      target: '[data-guiding-id="issue-5"]',
      content: (
        <FormattedMessage
          id="guiding.issue_list.5.content"
          defaultMessage={translate('guiding.issue_list.5.content')}
          values={{
            link: (
              <DocumentationLink to={DocLink.CleanCodeIntroduction} className="sw-capitalize">
                {translate('documentation')}
              </DocumentationLink>
            ),
          }}
        />
      ),
      title: translate('guiding.issue_list.5.title'),
    },
  ];

  return (
    <SpotlightTour
      callback={onToggle}
      steps={steps}
      run={run}
      continuous
      stepIndex={step}
      skipLabel={translate('skip')}
      backLabel={translate('go_back')}
      closeLabel={translate('close')}
      nextLabel={translate('next')}
      stepXofYLabel={(x: number, y: number) => translateWithParameters('guiding.step_x_of_y', x, y)}
    />
  );
}
