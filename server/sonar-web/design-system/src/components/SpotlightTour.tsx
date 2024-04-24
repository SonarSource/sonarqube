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
import { keyframes } from '@emotion/react';
import styled from '@emotion/styled';
import React from 'react';
import { useIntl } from 'react-intl';
import ReactJoyride, {
  Props as JoyrideProps,
  Step as JoyrideStep,
  TooltipRenderProps,
} from 'react-joyride';
import tw from 'twin.macro';
import { GLOBAL_POPUP_Z_INDEX, PopupZLevel, themeColor } from '../helpers';
import { findAnchor } from '../helpers/dom';
import { ButtonPrimary } from '../sonar-aligned/components/buttons';
import { ButtonLink, WrapperButton } from './buttons';
import { CloseIcon } from './icons';
import { PopupWrapper } from './popups';

type Placement = 'left' | 'right' | 'top' | 'bottom' | 'center';

export interface SpotlightTourProps extends Omit<JoyrideProps, 'steps'> {
  backLabel?: string;
  closeLabel?: string;
  nextLabel?: string;
  skipLabel?: string;
  stepXofYLabel?: (x: number, y: number) => string;
  steps: SpotlightTourStep[];
  width?: number;
}

export type SpotlightTourStep = JoyrideStep & {
  placement?: Placement;
};

// React Joyride needs a "global" property to be defined on window. It will throw an error if it cannot find it.
// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-unsafe-member-access
(window as any).global = (window as any).global ?? {};

const PULSE_SIZE = 8;
const DEFAULT_PLACEMENT = 'bottom';
const DEFAULT_WIDTH = 315;
const defultRect = new DOMRect(0, 0, 0, 0);

function TooltipComponent({
  continuous,
  index,
  step,
  size,
  isLastStep,
  backProps,
  skipProps,
  closeProps,
  primaryProps,
  stepXofYLabel,
  tooltipProps,
  width = DEFAULT_WIDTH,
}: TooltipRenderProps & {
  step: SpotlightTourStep;
  stepXofYLabel: SpotlightTourProps['stepXofYLabel'];
  width?: number;
}) {
  const [timeStamp, setTimeStamp] = React.useState(0);
  const [ref, setRef] = React.useState<HTMLDivElement | null>(null);

  const placement = step.placement ?? DEFAULT_PLACEMENT;
  const intl = useIntl();

  React.useEffect(() => {
    const target =
      typeof step.target === 'string' ? document.querySelector(step.target) : step.target;
    // To show the highlight, target has to be HighlightRing from design system
    target?.classList.add('active');

    return () => {
      target?.classList.remove('active');
    };
  }, [step]);

  React.useEffect(() => {
    const updateScroll = (event: Event) => {
      // The spotlight is doint transition that would look strange when we
      // re-render arrow right away.
      setTimeout(() => {
        setTimeStamp(event.timeStamp);
      }, 0);
    };

    document.addEventListener('scroll', updateScroll, { capture: true });

    return () => {
      document.removeEventListener('scroll', updateScroll, { capture: true });
    };
  }, []);

  const rect = ref?.parentElement?.getBoundingClientRect();
  const targetElement =
    typeof step.target === 'string'
      ? document.querySelector<HTMLElement>(step.target)
      : step.target;
  const targetRect = targetElement?.getBoundingClientRect();

  /**
   * Preventing click events from bubbling to avoid closing other popups, in cases when the guide
   * is shown simultaneously with other popups.
   */
  function handleClick(e: React.MouseEvent) {
    e.stopPropagation();
  }

  const arrowPosition = React.useMemo(
    () => findAnchor(rect ?? defultRect, targetRect ?? defultRect, PULSE_SIZE),
    [rect, targetRect, timeStamp],
  );

  return (
    <StyledPopupWrapper
      className="sw-p-3 sw-body-sm sw-relative sw-border-0"
      onClick={handleClick}
      style={{ width }}
      zLevel={PopupZLevel.Absolute}
      {...tooltipProps}
    >
      {placement !== 'center' && (
        <SpotlightArrowWrapper left={arrowPosition.left} top={arrowPosition.top}>
          <SpotlightArrow rotate={arrowPosition.rotate} width={arrowPosition.width} />
        </SpotlightArrowWrapper>
      )}

      <div className="sw-flex sw-justify-between" ref={setRef}>
        <strong className="sw-body-md-highlight sw-mb-2">{step.title}</strong>
        <WrapperButton
          className="sw-w-[30px] sw-h-[30px] sw--mt-2 sw--mr-2 sw-flex sw-justify-center"
          {...skipProps}
        >
          <CloseIcon className="sw-mr-0" />
        </WrapperButton>
      </div>
      <div>{step.content}</div>
      <div className="sw-flex sw-justify-between sw-items-center sw-mt-4">
        {(stepXofYLabel || size > 1) && (
          <strong>
            {stepXofYLabel
              ? stepXofYLabel(index + 1, size)
              : intl.formatMessage({ id: 'guiding.step_x_of_y' }, { '0': index + 1, '1': size })}
          </strong>
        )}
        <span />
        <div>
          {index > 0 && (
            <ButtonLink className="sw-mr-4" {...backProps}>
              {backProps.title}
            </ButtonLink>
          )}
          {continuous && !isLastStep && (
            <ButtonPrimary {...primaryProps}>{primaryProps.title}</ButtonPrimary>
          )}
          {(!continuous || isLastStep) && (
            <ButtonPrimary {...closeProps}>{closeProps.title}</ButtonPrimary>
          )}
        </div>
      </div>
    </StyledPopupWrapper>
  );
}

export function SpotlightTour(props: SpotlightTourProps) {
  const {
    steps,
    skipLabel,
    backLabel,
    closeLabel,
    nextLabel,
    stepXofYLabel,
    disableOverlay = true,
    width,
    ...otherProps
  } = props;

  const intl = useIntl();

  return (
    <ReactJoyride
      disableOverlay={disableOverlay}
      floaterProps={{
        styles: {
          floater: {
            zIndex: GLOBAL_POPUP_Z_INDEX,
          },
        },
        hideArrow: true,
        offset: 0,
      }}
      locale={{
        skip: skipLabel ?? intl.formatMessage({ id: 'skip' }),
        back: backLabel ?? intl.formatMessage({ id: 'go_back' }),
        close: closeLabel ?? intl.formatMessage({ id: 'close' }),
        next: nextLabel ?? intl.formatMessage({ id: 'next' }),
      }}
      scrollDuration={0}
      scrollOffset={250}
      steps={steps.map((s) => ({
        ...s,
        disableScrolling: true,
        disableBeacon: true,
        floaterProps: {
          disableAnimation: true,
          offset: 0,
        },
      }))}
      tooltipComponent={(
        tooltipProps: React.PropsWithChildren<TooltipRenderProps & { step: SpotlightTourStep }>,
      ) => <TooltipComponent stepXofYLabel={stepXofYLabel} width={width} {...tooltipProps} />}
      {...otherProps}
    />
  );
}

const StyledPopupWrapper = styled(PopupWrapper)`
  background-color: ${themeColor('spotlightBackgroundColor')};
  ${tw`sw-overflow-visible`};
  ${tw`sw-rounded-1`};
`;

const SpotlightArrowWrapper = styled.div<{ left: number; top: number }>`
  ${tw`sw-absolute`}
  ${tw`sw-z-popup`}

  width: ${PULSE_SIZE}px;
  height: ${PULSE_SIZE}px;
  left: ${({ left }) => left}px;
  top: ${({ top }) => top}px;
`;

const pulseKeyFrame = keyframes`
  0% { transform: scale(.50) }
  80%, 100% { opacity: 0 }
`;

const SpotlightArrow = styled.div<{ rotate: string; width: number }>`
  ${tw`sw-w-full sw-h-full`}
  ${tw`sw-rounded-pill`}
  background: ${themeColor('spotlightPulseBackground')};
  opacity: 1;
  transform: rotate(${({ rotate }) => rotate});

  &::after {
    ${tw`sw-block sw-absolute`}
    ${tw`sw-rounded-pill`}

    top: -100%;
    left: -100%;
    width: 300%;
    height: 300%;
    background-color: ${themeColor('spotlightPulseBackground')};
    animation: ${pulseKeyFrame} 1.25s cubic-bezier(0.215, 0.61, 0.355, 1) infinite;
    content: '';
  }

  &::before {
    ${tw`sw-block sw-absolute`}

    width: ${({ width }) => width}px;
    height: 0.125rem;
    background-color: ${themeColor('spotlightPulseBackground')};
    left: 100%;
    top: calc(50% - calc(0.125rem / 2));
    transition:
      margin 0.3s,
      left 0.3s;
    content: '';
  }
`;
