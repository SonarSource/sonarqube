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

import styled from '@emotion/styled';
import {
  IconCheckCircle,
  IconError,
  IconInfo,
  IconRecommended,
  IconWarning,
  IconX,
} from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { HTMLAttributes } from 'react';
import { IntlShape, useIntl } from 'react-intl';
import tw from 'twin.macro';
import { themeBorder, themeColor } from '../../helpers/theme';
import { ThemeColors } from '../../types/theme';

export type FlagMessageV2Variant = 'error' | 'warning' | 'success' | 'info' | 'recommended';

interface Props {
  hasIcon?: boolean;
  onDismiss?: () => void;
  title?: string;
  variant: FlagMessageV2Variant;
}

interface VariantInformation {
  backGroundColor: ThemeColors;
  borderColor: ThemeColors;
  icon: JSX.Element;
  iconColor: ThemeColors;
  iconFocusBackground: ThemeColors;
  iconHover: ThemeColors;
  iconHoverBackground: ThemeColors;
}

function getAlertVariantInfo(variant: FlagMessageV2Variant, intl: IntlShape): VariantInformation {
  const variantList: Record<FlagMessageV2Variant, VariantInformation> = {
    error: {
      icon: <IconError aria-label={intl.formatMessage({ id: 'flagmessage.tooltip.error' })} />,
      borderColor: 'errorBorder',
      backGroundColor: 'errorBackground',
      iconColor: 'errorIcon',
      iconHover: 'errorIconHover',
      iconHoverBackground: 'errorIconHoverBackground',
      iconFocusBackground: 'errorIconFocusBackground',
    },
    warning: {
      icon: <IconWarning aria-label={intl.formatMessage({ id: 'flagmessage.tooltip.warning' })} />,
      borderColor: 'warningBorder',
      backGroundColor: 'warningBackground',
      iconColor: 'warningIcon',
      iconHover: 'warningIconHover',
      iconHoverBackground: 'warningIconHoverBackground',
      iconFocusBackground: 'warningIconFocusBackground',
    },
    success: {
      icon: (
        <IconCheckCircle aria-label={intl.formatMessage({ id: 'flagmessage.tooltip.success' })} />
      ),
      borderColor: 'successBorder',
      backGroundColor: 'successBackground',
      iconColor: 'successIcon',
      iconHover: 'successIconHover',
      iconHoverBackground: 'successIconHoverBackground',
      iconFocusBackground: 'successIconFocusBackground',
    },
    info: {
      icon: <IconInfo aria-label={intl.formatMessage({ id: 'flagmessage.tooltip.info' })} />,
      borderColor: 'infoBorder',
      backGroundColor: 'infoBackground',
      iconColor: 'infoIcon',
      iconHover: 'infoIconHover',
      iconHoverBackground: 'infoIconHoverBackground',
      iconFocusBackground: 'infoIconFocusBackground',
    },
    recommended: {
      icon: <IconRecommended aria-label={intl.formatMessage({ id: 'flagmessage.tooltip.info' })} />,
      borderColor: 'recommendedBorder',
      backGroundColor: 'recommendedBackground',
      iconColor: 'recommendedIcon',
      iconHover: 'recommendedIconHover',
      iconHoverBackground: 'recommendedIconHoverBackground',
      iconFocusBackground: 'recommendedIconFocusBackground',
    },
  };

  return variantList[variant];
}

export function FlagMessageV2(props: Readonly<Props & HTMLAttributes<HTMLDivElement>>) {
  const { className, children, hasIcon = true, onDismiss, title, variant, ...domProps } = props;
  const intl = useIntl();
  const variantInfo = getAlertVariantInfo(variant, intl);

  return (
    <StyledFlag
      className={classNames('js-flag-message', className)}
      role="alert"
      variantInfo={variantInfo}
      {...domProps}
    >
      {hasIcon && <IconWrapper variantInfo={variantInfo}>{variantInfo.icon}</IconWrapper>}
      <div className="sw-flex sw-flex-col sw-gap-2">
        {title && <Title>{title}</Title>}
        <StyledFlagContent>{children}</StyledFlagContent>
      </div>
      {onDismiss !== undefined && (
        <DismissButton
          aria-label={intl.formatMessage({ id: 'close' })}
          onClick={onDismiss}
          variantInfo={variantInfo}
        >
          <IconX />
        </DismissButton>
      )}
    </StyledFlag>
  );
}

const StyledFlag = styled.div<{
  variantInfo: VariantInformation;
}>`
  ${tw`sw-inline-flex sw-gap-1`}
  ${tw`sw-box-border`}
  ${tw`sw-px-4 sw-py-2`}
  ${tw`sw-mb-1`}
  ${tw`sw-rounded-2`}

  background-color: ${({ variantInfo }) => themeColor(variantInfo.backGroundColor)};
  border: ${({ variantInfo }) => themeBorder('default', variantInfo.borderColor)};
`;

const IconWrapper = styled.div<{
  variantInfo: VariantInformation;
}>`
  ${tw`sw-flex`}
  ${tw`sw-text-[1rem]`}
  color: ${({ variantInfo }) => themeColor(variantInfo.iconColor)};
`;

const Title = styled.span`
  ${tw`sw-typo-lg-semibold`}
  color: ${themeColor('flagMessageText')};
`;

const StyledFlagContent = styled.div`
  ${tw`sw-pt-1/2`}
  ${tw`sw-overflow-auto`}
  ${tw`sw-typo-default`}
`;

const DismissButton = styled.button<{
  variantInfo: VariantInformation;
}>`
  ${tw`sw-flex sw-justify-center sw-items-center sw-shrink-0`}
  ${tw`sw-w-6 sw-h-6`}
  ${tw`sw-box-border`}
  ${tw`sw-rounded-1`}
  ${tw`sw-cursor-pointer`}
  ${tw`sw-border-none`}
  background: none;

  color: ${({ variantInfo }) => themeColor(variantInfo.iconColor)};
  transition:
    box-shadow 0.2s ease,
    outline 0.2s ease,
    color 0.2s ease;

  &:focus,
  &:active {
    background-color: ${({ theme, variantInfo }) =>
      `${themeColor(variantInfo.iconFocusBackground)({ theme })}`};
    box-shadow:
      0px 0px 0px 1px ${themeColor('backgroundSecondary')},
      0px 0px 0px 3px ${themeColor('flagMessageFocusBackground')};
  }

  &:hover {
    color: ${({ theme, variantInfo }) => `${themeColor(variantInfo.iconHover)({ theme })}`};
    background-color: ${({ theme, variantInfo }) =>
      `${themeColor(variantInfo.iconHoverBackground)({ theme })}`};
  }
`;
