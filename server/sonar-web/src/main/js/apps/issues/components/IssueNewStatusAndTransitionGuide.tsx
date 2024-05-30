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

import { SpotlightTour, SpotlightTourStep } from 'design-system';
import React, { useState } from 'react';
import { useIntl } from 'react-intl';
import { CallBackProps } from 'react-joyride';
import { useCurrentUser } from '../../../app/components/current-user/CurrentUserContext';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { SCREEN_POSITION_COMPUTE_DELAY } from '../../../components/common/ScreenPositionHelper';
import { DocLink } from '../../../helpers/doc-links';
import { useDismissNoticeMutation } from '../../../queries/users';
import { IssueTransition } from '../../../types/issues';
import { Issue } from '../../../types/types';
import { NoticeType } from '../../../types/users';

interface Props {
  run?: boolean;
  togglePopup: (issue: string, popup: string, show?: boolean) => void;
  issues: Issue[];
}

const PLACEMENT_RIGHT = 'right';
export const SESSION_STORAGE_TRANSITION_GUIDE_KEY = 'issueNewStatusAndTransitionGuideStep';
const EXTRA_DELAY = 100;
const GUIDE_WIDTH = 360;

export default function IssueNewStatusAndTransitionGuide(props: Readonly<Props>) {
  const { run, issues, togglePopup } = props;
  const { currentUser, updateDismissedNotices } = useCurrentUser();
  const { mutateAsync: dismissNotice } = useDismissNoticeMutation();
  const intl = useIntl();
  const [step, setStep] = useState(
    +(sessionStorage.getItem(SESSION_STORAGE_TRANSITION_GUIDE_KEY) ?? 0),
  );
  const [start, setStart] = React.useState(false);

  const issueWithAcceptTransition = issues.find((issue) =>
    issue.transitions.includes(IssueTransition.Accept),
  );

  const userCompletedCCTGuide =
    currentUser.isLoggedIn && currentUser.dismissedNotices[NoticeType.ISSUE_GUIDE];
  const userCompletedStatusGuide =
    currentUser.isLoggedIn &&
    currentUser.dismissedNotices[NoticeType.ISSUE_NEW_STATUS_AND_TRANSITION_GUIDE];
  const canRun =
    userCompletedCCTGuide && !userCompletedStatusGuide && run && issueWithAcceptTransition;

  // Wait for the issue list to be rendered, then scroll to the issue, wait for an extra delay
  // to ensure proper positioning of the SpotlightTour in the context of ScreenPositionHelper,
  // then start the tour.
  React.useEffect(() => {
    // Should start the tour if it is not started yet
    if (!start && canRun) {
      setTimeout(() => {
        // Scroll to issue. This ensures proper rendering of the SpotlightTour.
        document
          .querySelector(`[data-guiding-id="issue-transition-${issueWithAcceptTransition.key}"]`)
          ?.scrollIntoView({ behavior: 'instant', block: 'center' });
        // Start the tour
        if (step !== 0) {
          togglePopup(issueWithAcceptTransition.key, 'transition', true);
          setTimeout(() => {
            setStart(run);
          }, 0);
        } else {
          setStart(run);
        }
      }, SCREEN_POSITION_COMPUTE_DELAY + EXTRA_DELAY);
    }
  }, [canRun, run, step, start, togglePopup, issueWithAcceptTransition]);

  React.useEffect(() => {
    if (start && canRun) {
      sessionStorage.setItem(SESSION_STORAGE_TRANSITION_GUIDE_KEY, step.toString());
    }
  }, [step, start, canRun]);

  if (!canRun || !start) {
    return null;
  }

  const onToggle = (props: CallBackProps) => {
    const { action, lifecycle, index } = props;
    switch (action) {
      case 'close':
      case 'skip':
      case 'reset':
        togglePopup(issueWithAcceptTransition.key, 'transition', false);
        sessionStorage.removeItem(SESSION_STORAGE_TRANSITION_GUIDE_KEY);
        dismissNotice(NoticeType.ISSUE_NEW_STATUS_AND_TRANSITION_GUIDE)
          .then(() => {
            updateDismissedNotices(NoticeType.ISSUE_NEW_STATUS_AND_TRANSITION_GUIDE, true);
          })
          .catch(() => {
            /* noop */
          });
        break;
      case 'next':
        if (lifecycle === 'complete') {
          if (index === 0) {
            togglePopup(issueWithAcceptTransition.key, 'transition', true);
            setTimeout(() => {
              setStep(step + 1);
            }, 0);
          } else {
            setStep(step + 1);
          }
        }
        break;
      case 'prev':
        if (lifecycle === 'complete') {
          if (index === 1) {
            togglePopup(issueWithAcceptTransition.key, 'transition', false);
          }
          setStep(step - 1);
        }
        break;
      default:
        break;
    }
  };

  const constructContent = (stepIndex: number) => {
    return (
      <>
        <div className="sw-flex sw-flex-col sw-gap-4">
          <span>{intl.formatMessage({ id: `guiding.issue_accept.${stepIndex}.content.1` })}</span>
          <span>{intl.formatMessage({ id: `guiding.issue_accept.${stepIndex}.content.2` })}</span>
        </div>
        <DocumentationLink to={DocLink.IssueStatuses} className="sw-mt-1 sw-inline-block">
          {intl.formatMessage({ id: `guiding.issue_accept.${stepIndex}.content.link` })}
        </DocumentationLink>
      </>
    );
  };

  const steps: SpotlightTourStep[] = [
    {
      target: `[data-guiding-id="issue-transition-${issueWithAcceptTransition.key}"]`,
      title: intl.formatMessage({ id: 'guiding.issue_accept.1.title' }),
      content: intl.formatMessage({ id: 'guiding.issue_accept.1.content.1' }),
      placement: PLACEMENT_RIGHT,
    },
    {
      target: '[data-guiding-id="issue-accept-transition"]',
      title: intl.formatMessage({ id: 'guiding.issue_accept.2.title' }),
      content: constructContent(2),
      placement: PLACEMENT_RIGHT,
    },
    {
      target: '[data-guiding-id="issue-deprecated-transitions"]',
      title: intl.formatMessage({ id: 'guiding.issue_accept.3.title' }),
      content: constructContent(3),
      placement: PLACEMENT_RIGHT,
    },
  ];

  return (
    <SpotlightTour
      width={GUIDE_WIDTH}
      callback={onToggle}
      steps={steps}
      stepIndex={step}
      run={run}
      continuous
      skipLabel={intl.formatMessage({ id: 'skip' })}
      backLabel={intl.formatMessage({ id: 'go_back' })}
      closeLabel={intl.formatMessage({ id: 'close' })}
      nextLabel={intl.formatMessage({ id: 'next' })}
      stepXofYLabel={(x: number, y: number) =>
        intl.formatMessage({ id: 'guiding.step_x_of_y' }, { '0': x, '1': y })
      }
    />
  );
}
