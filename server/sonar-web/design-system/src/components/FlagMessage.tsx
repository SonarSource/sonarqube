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
import tw from 'twin.macro';
import { themeBorder, themeColor, themeContrast } from '../helpers/theme';
import { ThemeColors } from '../types/theme';
import { FlagErrorIcon, FlagInfoIcon, FlagSuccessIcon, FlagWarningIcon } from './icons';

export type Variant = 'error' | 'warning' | 'success' | 'info';

interface Props {
  ariaLabel: string;
  variant: Variant;
}

interface VariantInformation {
  backGroundColor: ThemeColors;
  borderColor: ThemeColors;
  icon: JSX.Element;
  role: string;
}

function getVariantInfo(variant: Variant): VariantInformation {
  const variantList: Record<Variant, VariantInformation> = {
    error: {
      icon: <FlagErrorIcon />,
      borderColor: 'errorBorder',
      backGroundColor: 'errorBackground',
      role: 'alert',
    },
    warning: {
      icon: <FlagWarningIcon />,
      borderColor: 'warningBorder',
      backGroundColor: 'warningBackground',
      role: 'alert',
    },
    success: {
      icon: <FlagSuccessIcon />,
      borderColor: 'successBorder',
      backGroundColor: 'successBackground',
      role: 'status',
    },
    info: {
      icon: <FlagInfoIcon />,
      borderColor: 'infoBorder',
      backGroundColor: 'infoBackground',
      role: 'status',
    },
  };

  return variantList[variant];
}

export function FlagMessage(props: Props & React.HTMLAttributes<HTMLDivElement>) {
  const { ariaLabel, className, variant, ...domProps } = props;
  const variantInfo = getVariantInfo(variant);

  return (
    <StyledFlag
      aria-label={ariaLabel}
      className={classNames('alert', className)}
      role={variantInfo.role}
      variantInfo={variantInfo}
      {...domProps}
    >
      <StyledFlagInner>
        <StyledFlagIcon variantInfo={variantInfo}>{variantInfo.icon}</StyledFlagIcon>
        <StyledFlagContent>{props.children}</StyledFlagContent>
      </StyledFlagInner>
    </StyledFlag>
  );
}

export const StyledFlag = styled.div<{
  variantInfo: VariantInformation;
}>`
  ${tw`sw-inline-flex`}
  ${tw`sw-min-h-10`}
  ${tw`sw-rounded-1`}
  border: ${({ variantInfo }) => themeBorder('default', variantInfo.borderColor)};
  background-color: ${themeColor('flagMessageBackground')};
`;

const StyledFlagInner = styled.div`
  ${tw`sw-flex sw-items-stretch`}
  ${tw`sw-box-border`}
`;

const StyledFlagIcon = styled.div<{ variantInfo: VariantInformation }>`
  ${tw`sw-flex sw-justify-center sw-items-center`}
  ${tw`sw-rounded-l-1`}
  ${tw`sw-px-3`}
  background-color: ${({ variantInfo }) => themeColor(variantInfo.backGroundColor)};
`;

const StyledFlagContent = styled.div`
  ${tw`sw-flex sw-flex-auto sw-items-center`}
  ${tw`sw-overflow-auto`}
  ${tw`sw-text-left`}
  ${tw`sw-mx-3 sw-my-2`}
  ${tw`sw-body-sm`}
  color: ${themeContrast('flagMessageBackground')};
`;
