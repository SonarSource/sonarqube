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
import { SpotlightTour, SpotlightTourStep } from 'design-system';
import React from 'react';
import { useIntl } from 'react-intl';
import { CallBackProps } from 'react-joyride';
import { createSharedStoreHook } from 'shared-store-hook';
import { useCurrentUser } from '../../../app/components/current-user/CurrentUserContext';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { SCREEN_POSITION_COMPUTE_DELAY } from '../../../components/common/ScreenPositionHelper';
import { useDismissNoticeMutation } from '../../../queries/users';
import { IssueTransition } from '../../../types/issues';
import { Issue } from '../../../types/types';
import { NoticeType } from '../../../types/users';

export const useAcceptGuideState = createSharedStoreHook<{
  stepIndex: number;
  guideIsRunning: boolean;
}>({
  initialState: { stepIndex: 0, guideIsRunning: false },
});

interface Props {
  run?: boolean;
  togglePopup: (issue: string, popup: string, show?: boolean) => void;
  issues: Issue[];
}

const PLACEMENT_RIGHT = 'right';
const DOC_LINK = '/user-guide/issues/#statuses';
const EXTRA_DELAY = 100;
const GUIDE_WIDTH = 360;

export default function IssueNewStatusAndTransitionGuide(props: Readonly<Props>) {
  const { run, issues } = props;
  const { currentUser, updateDismissedNotices } = useCurrentUser();
  const { mutateAsync: dismissNotice } = useDismissNoticeMutation();
  const intl = useIntl();
  const [{ guideIsRunning, stepIndex }, { setPartialState, resetState }] = useAcceptGuideState();

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
    // If should start the tour and the tour is not started yet
    if (!guideIsRunning && canRun) {
      setTimeout(() => {
        // Scroll to issue. This ensures proper rendering of the SpotlightTour.
        document
          .querySelector(`[data-guiding-id="issue-transition-${issueWithAcceptTransition.key}"]`)
          ?.scrollIntoView({ behavior: 'instant', block: 'center' });
        // Start the tour
        setPartialState({ guideIsRunning: true });
      }, SCREEN_POSITION_COMPUTE_DELAY + EXTRA_DELAY);
    }
  }, [canRun, guideIsRunning, setPartialState, issueWithAcceptTransition]);

  // We reset the state all the time so that the tour can be restarted when user revisits the page.
  // This has effect only when user is ignored guide.
  React.useEffect(() => {
    return resetState;
  }, [resetState]);

  if (!issueWithAcceptTransition || !guideIsRunning) {
    return null;
  }

  const dismissTour = async () => {
    if (userCompletedStatusGuide) {
      return;
    }

    try {
      await dismissNotice(NoticeType.ISSUE_NEW_STATUS_AND_TRANSITION_GUIDE);
      updateDismissedNotices(NoticeType.ISSUE_NEW_STATUS_AND_TRANSITION_GUIDE, true);
    } catch {
      // ignore
    }
  };

  const handleTourCallback = async ({ action, type, index }: CallBackProps) => {
    if (type === 'step:after') {
      // Open dropdown when going into step 1 and dismiss notice (we assume that the user has read the notice)
      if (action === 'next' && index === 0) {
        props.togglePopup(issueWithAcceptTransition.key, 'transition', true);
        setTimeout(() => {
          setPartialState({ stepIndex: index + 1 });
          dismissTour();
        }, 0);
        return;
      }

      // Close dropdown when going into step 0 from step 1
      if (action === 'prev' && index === 1) {
        props.togglePopup(issueWithAcceptTransition.key, 'transition', false);
      }

      setPartialState({ stepIndex: action === 'prev' ? index - 1 : index + 1 });
      return;
    }

    // When the tour is finished or skipped.
    if (action === 'reset' || action === 'skip' || action === 'close') {
      props.togglePopup(issueWithAcceptTransition.key, 'transition', false);
      await dismissTour();
    }
  };

  const constructContent = (stepIndex: number) => {
    return (
      <>
        <div className="sw-flex sw-flex-col sw-gap-4">
          <span>{intl.formatMessage({ id: `guiding.issue_accept.${stepIndex}.content.1` })}</span>
          <span>{intl.formatMessage({ id: `guiding.issue_accept.${stepIndex}.content.2` })}</span>
        </div>
        <DocumentationLink to={DOC_LINK} className="sw-mt-1 sw-inline-block">
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
      callback={handleTourCallback}
      steps={steps}
      stepIndex={stepIndex}
      run={guideIsRunning}
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
