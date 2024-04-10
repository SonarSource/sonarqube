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

/** @deprecated Use IconProps from Echoes instead.
 *
 * Some of the props have been dropped:
 * - ~`aria-hidden`~ is now inferred from the absence of an `aria-label`
 * - ~`data-guiding-id`~ is no longer passed down to the DOM, put it on a wrapper instead
 * - ~`description`~ doesn't exist anymore
 * - ~`fill`~ doesn't exist anymore, icon colors cannot be overrriden anymore, they take the color
 *   of their surroundings (currentColor) or have an intrinsic color. If you need to change the
 *   color, either make sure the wrapper has a color, or define a styled(MyIcon). Those cases should
 *   be rare and happen only during the transition to Echoes icons.
 * - ~`height`~ doesn't exist anymore, icons are sized based on the font-size of their parent
 * - ~`transform`~ doesn't exist anymore
 * - ~`viewbox`~ doesn't exist anymore, icons are sized based on the font-size of their parent
 * - ~`width`~ doesn't exist anymore, icons are sized based on the font-size of their parent
 */
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

/** @deprecated Don't create new icons based on this, use the ones from Echoes instead. */
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

/** @deprecated Don't create new icons based on this, use the ones from Echoes instead. */
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
