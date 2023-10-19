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
import classNames from 'classnames';
import * as React from 'react';
import { useIntl } from 'react-intl';
import tw from 'twin.macro';
import { themeBorder, themeColor, themeContrast } from '../helpers/theme';
import { ThemeColors } from '../types/theme';
import { InteractiveIcon } from './InteractiveIcon';
import { CloseIcon, FlagErrorIcon, FlagInfoIcon, FlagSuccessIcon, FlagWarningIcon } from './icons';

export type Variant = 'error' | 'warning' | 'success' | 'info';

interface Props {
  variant: Variant;
}

function getVariantInfo(variant: Variant) {
  const variantList = {
    error: {
      icon: <FlagErrorIcon />,
      borderColor: 'errorBorder',
      backGroundColor: 'errorBackground',
    },
    warning: {
      icon: <FlagWarningIcon />,
      borderColor: 'warningBorder',
      backGroundColor: 'warningBackground',
    },
    success: {
      icon: <FlagSuccessIcon />,
      borderColor: 'successBorder',
      backGroundColor: 'successBackground',
    },
    info: {
      icon: <FlagInfoIcon />,
      borderColor: 'infoBorder',
      backGroundColor: 'infoBackground',
    },
  } as const;

  return variantList[variant];
}

export function FlagMessage(props: Props & React.HTMLAttributes<HTMLDivElement>) {
  const { className, variant, ...domProps } = props;
  const variantInfo = getVariantInfo(variant);

  return (
    <StyledFlag
      backGroundColor={variantInfo.backGroundColor}
      borderColor={variantInfo.borderColor}
      className={classNames('alert', className)}
      {...domProps}
    >
      <div className="flag-inner">
        <div className="flag-icon">{variantInfo.icon}</div>
        <div className="flag-content">{props.children}</div>
      </div>
    </StyledFlag>
  );
}

FlagMessage.displayName = 'FlagMessage'; // so that tests don't see the obfuscated production name

interface DismissableFlagMessageProps extends Props {
  onDismiss: () => void;
}

export function DismissableFlagMessage(
  props: DismissableFlagMessageProps & React.HTMLAttributes<HTMLDivElement>,
) {
  const { onDismiss, children, ...flagMessageProps } = props;
  const intl = useIntl();
  return (
    <FlagMessage {...flagMessageProps}>
      {children}
      <DismissIcon
        Icon={CloseIcon}
        aria-label={intl.formatMessage({ id: 'dismiss' })}
        className="sw-ml-3"
        onClick={onDismiss}
        size="small"
      />
    </FlagMessage>
  );
}

DismissableFlagMessage.displayName = 'DismissableFlagMessage'; // so that tests don't see the obfuscated production name

export const StyledFlag = styled.div<{
  backGroundColor: ThemeColors;
  borderColor: ThemeColors;
}>`
  ${tw`sw-inline-flex`}
  ${tw`sw-min-h-10`}
  ${tw`sw-rounded-1`}
  ${tw`sw-box-border`}
  border: ${({ borderColor }) => themeBorder('default', borderColor)};
  background-color: ${themeColor('flagMessageBackground')};

  & > .flag-inner {
    ${tw`sw-flex sw-items-stretch`}
    ${tw`sw-box-border`}
  }

  & .flag-icon {
    ${tw`sw-flex sw-justify-center sw-items-center`}
    ${tw`sw-rounded-l-1`}
    ${tw`sw-px-3`}
    background-color: ${({ backGroundColor }) => themeColor(backGroundColor)};
  }

  & .flag-content {
    ${tw`sw-flex sw-flex-auto sw-items-center`}
    ${tw`sw-overflow-auto`}
    ${tw`sw-text-left`}
    ${tw`sw-px-3 sw-py-2`}
    ${tw`sw-body-sm`}
    color: ${themeContrast('flagMessageBackground')};
  }
`;

export const DismissIcon = styled(InteractiveIcon)`
  --background: ${themeColor('productNews')};
  --backgroundHover: ${themeColor('productNewsHover')};
  --color: ${themeContrast('productNews')};
  --colorHover: ${themeContrast('productNewsHover')};
  --focus: ${themeColor('interactiveIconFocus', 0.2)};

  height: 28px;
`;
