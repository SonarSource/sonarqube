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

import { Button, ButtonVariety, Modal, ModalSize } from '@sonarsource/echoes-react';
import { debounce } from 'lodash';
import { useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useIntl } from 'react-intl';
import { CallBackProps } from 'react-joyride';
import { SpotlightTour, SpotlightTourStep } from '~design-system';
import { Image } from '~sonar-aligned/components/common/Image';
import { dismissNotice } from '../../api/users';
import DocumentationLink from '../../components/common/DocumentationLink';
import { CustomEvents } from '../../helpers/constants';
import { DocLink } from '../../helpers/doc-links';
import { useStandardExperienceModeQuery } from '../../queries/mode';
import { Permissions } from '../../types/permissions';
import { NoticeType } from '../../types/users';
import { useAppState } from './app-state/withAppStateContext';
import { CurrentUserContext } from './current-user/CurrentUserContext';

const MAX_STEPS = 4;

export default function ModeTour() {
  const { currentUser, updateDismissedNotices } = useContext(CurrentUserContext);
  const appState = useAppState();
  const intl = useIntl();
  const { data: isStandardMode } = useStandardExperienceModeQuery();
  const [step, setStep] = useState(1);
  const [runManually, setRunManually] = useState(false);

  const dismissedTour = currentUser.dismissedNotices[NoticeType.MODE_TOUR];

  const nextStep = () => {
    if (step === MAX_STEPS) {
      document.dispatchEvent(new CustomEvent(CustomEvents.OpenHelpMenu));
      setTimeout(() => setStep(5));
    } else {
      setStep(step + 1);
    }
  };

  const dismissTourWithDebounce = useMemo(
    () =>
      debounce(() => {
        dismissNotice(NoticeType.MODE_TOUR)
          .then(() => {
            updateDismissedNotices(NoticeType.MODE_TOUR, true);
          })
          .catch(() => {
            /* noop */
          });
      }, 500),
    [updateDismissedNotices],
  );

  const dismissTour = useCallback(() => {
    document.dispatchEvent(new CustomEvent(CustomEvents.CloseHelpMenu));
    setStep(6);
    if (!dismissedTour) {
      dismissTourWithDebounce();
    }
  }, [dismissedTour, dismissTourWithDebounce]);

  const onToggle = (props: CallBackProps) => {
    switch (props.action) {
      case 'close':
      case 'skip':
      case 'reset':
        dismissTour();
        break;
      case 'next':
        if (props.lifecycle === 'complete') {
          nextStep();
        }
        break;
      default:
        break;
    }
  };

  useEffect(() => {
    const listener = () => {
      setStep(1);
      setRunManually(true);
    };
    document.addEventListener(CustomEvents.RunTourMode, listener);

    return () => document.removeEventListener(CustomEvents.RunTourMode, listener);
  }, []);

  const isAdmin = currentUser.permissions?.global.includes(Permissions.Admin);
  const isAdminOrQGAdmin =
    isAdmin || currentUser.permissions?.global.includes(Permissions.QualityGateAdmin);

  useEffect(() => {
    if (currentUser.isLoggedIn && !isAdminOrQGAdmin && !dismissedTour) {
      dismissTour();
    }
  }, [currentUser.isLoggedIn, isAdminOrQGAdmin, dismissedTour, dismissTour]);

  if (!runManually && (currentUser.dismissedNotices[NoticeType.MODE_TOUR] || !isAdminOrQGAdmin)) {
    return null;
  }

  const steps: SpotlightTourStep[] = [
    ...(isAdmin
      ? [
          {
            target: '[data-guiding-id="mode-tour-1"]',
            content: intl.formatMessage(
              { id: 'mode_tour.step4.description' },
              {
                mode: intl.formatMessage({
                  id: `settings.mode.${isStandardMode ? 'standard' : 'mqr'}.name`,
                }),
                p1: (text) => <p>{text}</p>,
                p: (text) => <p className="sw-mt-2">{text}</p>,
                b: (text) => <b>{text}</b>,
              },
            ),
            title: intl.formatMessage({ id: 'mode_tour.step4.title' }),
            placement: 'bottom' as const,
          },
        ]
      : []),
    {
      target: '[data-guiding-id="mode-tour-2"]',
      title: intl.formatMessage({ id: 'mode_tour.step5.title' }),
      content: null,
      placement: 'left',
      hideFooter: true,
    },
  ];

  const maxModalSteps = isAdmin ? MAX_STEPS - 1 : MAX_STEPS;

  return (
    <>
      <Modal
        size={ModalSize.Wide}
        isOpen={step <= maxModalSteps}
        onOpenChange={(isOpen) => isOpen === false && dismissTour()}
        title={
          step <= maxModalSteps &&
          intl.formatMessage({ id: `mode_tour.step${step}.title` }, { version: appState.version })
        }
        content={
          <>
            {step <= maxModalSteps && (
              <>
                <Image
                  alt={intl.formatMessage({ id: `mode_tour.step${step}.img_alt` })}
                  className="sw-w-full sw-mb-4"
                  src={`/images/mode-tour/step${step}.png`}
                />
                {intl.formatMessage(
                  { id: `mode_tour.step${step}.description` },
                  {
                    mode: intl.formatMessage({
                      id: `settings.mode.${isStandardMode ? 'standard' : 'mqr'}.name`,
                    }),
                    p1: (text) => <p>{text}</p>,
                    p: (text) => <p className="sw-mt-4">{text}</p>,
                    b: (text) => <b>{text}</b>,
                  },
                )}
                <div className="sw-mt-6">
                  <b>
                    {intl.formatMessage({ id: 'guiding.step_x_of_y' }, { 0: step, 1: MAX_STEPS })}
                  </b>
                </div>
              </>
            )}
          </>
        }
        footerLink={
          <DocumentationLink standalone to={DocLink.ModeMQR}>
            {intl.formatMessage({ id: `mode_tour.link` })}
          </DocumentationLink>
        }
        primaryButton={
          <Button variety={ButtonVariety.Primary} onClick={nextStep}>
            {intl.formatMessage({ id: step === 1 ? 'lets_go' : 'next' })}
          </Button>
        }
        secondaryButton={
          step === 1 && (
            <Button variety={ButtonVariety.Default} onClick={dismissTour}>
              {intl.formatMessage({ id: 'later' })}
            </Button>
          )
        }
      />
      <SpotlightTour
        callback={onToggle}
        steps={steps}
        run={step > maxModalSteps}
        continuous
        disableOverlay={false}
        showProgress={step !== 5}
        stepIndex={step - maxModalSteps - 1}
        nextLabel={intl.formatMessage({ id: 'next' })}
        stepXofYLabel={(x: number) =>
          intl.formatMessage({ id: 'guiding.step_x_of_y' }, { 0: x + maxModalSteps, 1: MAX_STEPS })
        }
      />
    </>
  );
}
