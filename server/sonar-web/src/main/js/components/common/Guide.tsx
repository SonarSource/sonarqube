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
import {
  ButtonLink,
  ButtonPrimary,
  CloseIcon,
  PopupWrapper,
  PopupZLevel,
  WrapperButton,
} from 'design-system';
import React from 'react';
import ReactJoyride, { Props as JoyrideProps, TooltipRenderProps } from 'react-joyride';
import { translate, translateWithParameters } from '../../helpers/l10n';

type Props = JoyrideProps;

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-unsafe-member-access
(window as any).global = (window as any).global ?? {};

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
    <PopupWrapper
      zLevel={PopupZLevel.Absolute}
      className="sw-p-3 sw-body-sm sw-w-[300px] sw-relative sw-border-0"
      {...tooltipProps}
    >
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
        <b>{translateWithParameters('guiding.step_x_of_y', index + 1, size)}</b>
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
    </PopupWrapper>
  );
}

export function Guide(props: Props) {
  return (
    <ReactJoyride
      scrollDuration={0}
      scrollOffset={250}
      tooltipComponent={TooltipComponent}
      locale={{
        skip: translate('skip'),
        back: translate('go_back'),
        close: translate('close'),
        next: translate('next'),
      }}
      {...props}
    />
  );
}
