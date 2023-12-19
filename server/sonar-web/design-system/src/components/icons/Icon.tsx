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
import { useTheme } from '@emotion/react';
import { OcticonProps } from '@primer/octicons-react';
import React from 'react';
import { theme } from 'twin.macro';
import { themeColor } from '../../helpers/theme';
import { CSSColor, ThemeColors } from '../../types/theme';

interface Props {
  'aria-hidden'?: boolean | 'true' | 'false';
  'aria-label'?: string;
  children: React.ReactNode;
  className?: string;
  description?: React.ReactNode;
}

export interface IconProps extends Omit<Props, 'children'> {
  ['data-guiding-id']?: string;
  fill?: ThemeColors | CSSColor;
  height?: number;
  transform?: string;
  viewBox?: string;
  width?: number;
}

const PIXELS_IN_ONE_REM = 16;

function convertRemToPixel(remString: string) {
  return Number(remString.replace('rem', '')) * PIXELS_IN_ONE_REM;
}

export function CustomIcon(props: Props) {
  const {
    'aria-label': ariaLabel,
    'aria-hidden': ariaHidden,
    children,
    className,
    description,
    ...iconProps
  } = props;
  return (
    <svg
      aria-hidden={ariaHidden ?? (ariaLabel ? 'false' : 'true')}
      aria-label={ariaLabel}
      className={className}
      fill="none"
      height={convertRemToPixel(theme('height.icon'))}
      role="img"
      style={{
        clipRule: 'evenodd',
        display: 'inline-block',
        fillRule: 'evenodd',
        userSelect: 'none',
        verticalAlign: 'middle',
        strokeLinejoin: 'round',
        strokeMiterlimit: 1.414,
      }}
      version="1.1"
      viewBox="0 0 16 16"
      width={convertRemToPixel(theme('width.icon'))}
      xmlSpace="preserve"
      xmlnsXlink="http://www.w3.org/1999/xlink"
      {...iconProps}
    >
      {description && <desc>{description}</desc>}
      {children}
    </svg>
  );
}

export function OcticonHoc(
  WrappedOcticon: React.ComponentType<React.PropsWithChildren<OcticonProps>>,
  displayName?: string,
): React.ComponentType<React.PropsWithChildren<IconProps>> {
  function IconWrapper({ fill, ...props }: IconProps) {
    const theme = useTheme();

    const size = props.width ?? props.height ?? 'small';

    return (
      <WrappedOcticon
        fill={fill && themeColor(fill)({ theme })}
        size={size}
        verticalAlign="middle"
        {...props}
      />
    );
  }

  IconWrapper.displayName = displayName ?? WrappedOcticon.displayName ?? WrappedOcticon.name;
  return IconWrapper;
}
