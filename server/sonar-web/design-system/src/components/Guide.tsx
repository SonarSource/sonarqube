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
import styled from '@emotion/styled';
import { Suspense } from 'react';
import ReactJoyride, { Props as JoyrideProps, TooltipRenderProps } from 'react-joyride';
import tw from 'twin.macro';
import { PopupZLevel } from '../helpers';
import { Spinner } from './DeferredSpinner';
import { ButtonPrimary, ButtonSecondary, WrapperButton } from './buttons';
import { CloseIcon } from './icons';
import { PopupWrapper } from './popups';

type Props = JoyrideProps;

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-unsafe-member-access
(window as any).global = (window as any).global ?? {};

const Popup = styled(PopupWrapper)`
  position: relative;
  width: 300px;
  border: none;
  ${tw`sw-body-sm`}
  ${tw`sw-p-3`}
`;

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
  tooltipProps,
}: TooltipRenderProps) {
  return (
    <Popup zLevel={PopupZLevel.Absolute} {...tooltipProps}>
      <div className="sw-flex sw-justify-between">
        <b className="sw-mb-2">{step.title}</b>
        <WrapperButton
          className="sw-w-[30px] sw-h-[30px] sw--mt-2 sw--mr-2 sw-flex sw-justify-center"
          {...skipProps}
        >
          <CloseIcon className="sw-mr-0" />
        </WrapperButton>
      </div>
      <div>{step.content}</div>
      <div className="sw-flex sw-justify-between sw-items-center sw-mt-3">
        <b>
          {index + 1} of {size}
        </b>
        <div>
          {index > 0 && (
            <ButtonSecondary className="sw-mr-2" {...backProps}>
              {backProps.title}
            </ButtonSecondary>
          )}
          {continuous && !isLastStep && (
            <ButtonPrimary {...primaryProps}>{primaryProps.title}</ButtonPrimary>
          )}
          {(!continuous || isLastStep) && (
            <ButtonPrimary {...closeProps}>{closeProps.title}</ButtonPrimary>
          )}
        </div>
      </div>
    </Popup>
  );
}

export function Guide(props: Props) {
  return (
    <Suspense fallback={<Spinner />}>
      <ReactJoyride
        scrollDuration={0}
        scrollOffset={250}
        tooltipComponent={TooltipComponent}
        {...props}
      />
    </Suspense>
  );
}
