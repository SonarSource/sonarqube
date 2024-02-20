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
import { ReactNode } from 'react';
import { useIntl } from 'react-intl';
import tw from 'twin.macro';
import { LAYOUT_BANNER_HEIGHT, LAYOUT_VIEWPORT_MIN_WIDTH, themeColor } from '../helpers';
import { ThemeColors } from '../types';
import { InteractiveIconBase } from './InteractiveIcon';
import { CloseIcon, FlagErrorIcon, FlagInfoIcon, FlagSuccessIcon, FlagWarningIcon } from './icons';

export type Variant = 'error' | 'warning' | 'success' | 'info';

interface Props {
  children: ReactNode;
  className?: string;
  onDismiss?: VoidFunction;
  variant: Variant;
}

function getVariantInfo(variant: Variant) {
  const variantList = {
    error: {
      icon: <FlagErrorIcon />,
      fontColor: 'errorText',
      backGroundColor: 'errorBackground',
    },
    warning: {
      icon: <FlagWarningIcon />,
      fontColor: 'warningText',
      backGroundColor: 'warningBackground',
    },
    success: {
      icon: <FlagSuccessIcon />,
      fontColor: 'successText',
      backGroundColor: 'successBackground',
    },
    info: {
      icon: <FlagInfoIcon />,
      fontColor: 'infoText',
      backGroundColor: 'infoBackground',
    },
  } as const;

  return variantList[variant];
}

export function Banner({ children, className, onDismiss, variant }: Props) {
  const variantInfo = getVariantInfo(variant);

  const intl = useIntl();

  return (
    <div className={className} role="alert" style={{ height: LAYOUT_BANNER_HEIGHT }}>
      <BannerWrapper
        backGroundColor={variantInfo.backGroundColor}
        fontColor={variantInfo.fontColor}
      >
        <BannerInner>
          <div className="sw-flex sw-items-center">
            <div className="sw-mr-3">{variantInfo.icon}</div>
            {children}
          </div>

          {onDismiss && (
            <BannerCloseIcon
              Icon={CloseIcon}
              aria-label={intl.formatMessage({ id: 'dismiss' })}
              backGroundColor={variantInfo.backGroundColor}
              fontColor={variantInfo.fontColor}
              onClick={onDismiss}
              size="small"
            />
          )}
        </BannerInner>
      </BannerWrapper>
    </div>
  );
}

const BannerWrapper = styled.div<{
  backGroundColor: ThemeColors;
  fontColor: ThemeColors;
}>`
  min-width: ${LAYOUT_VIEWPORT_MIN_WIDTH}px;
  max-width: 100%;
  height: inherit;
  background-color: ${({ backGroundColor }) => themeColor(backGroundColor)};
  color: ${({ fontColor }) => themeColor(fontColor)};
  ${tw`sw-z-popup sw-fixed sw-w-full`}
  ${tw`sw-sticky sw-top-0`}
`;

const BannerInner = styled.div`
  width: 100%;
  height: inherit;
  ${tw`sw-box-border`}
  ${tw`sw-flex sw-items-center sw-justify-between sw-gap-3`}
  ${tw`sw-px-4`}
  ${tw`sw-body-sm`}
`;

const BannerCloseIcon = styled(InteractiveIconBase)<{
  backGroundColor: ThemeColors;
  fontColor: ThemeColors;
}>`
  --background: ${({ backGroundColor }) => themeColor(backGroundColor)};
  --backgroundHover: ${({ fontColor }) => themeColor(fontColor)};
  --color: ${({ fontColor }) => themeColor(fontColor)};
  --colorHover: ${({ backGroundColor }) => themeColor(backGroundColor)};
  --focus: ${themeColor('bannerIconFocus', 0.2)};
`;
