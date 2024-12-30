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
import { themeColor } from '../../helpers';

interface OverviewQGPassedIconProps {
  className?: string;
  height?: number;
  width?: number;
}

const DEFAULT_WIDTH = 154;
const DEFAULT_HEIGHT = 136;

export function OverviewQGPassedIcon({
  className,
  height,
  width,
}: Readonly<OverviewQGPassedIconProps>) {
  const theme = useTheme();
  const actualWidth = width ?? DEFAULT_WIDTH;
  const actualHeight = height ?? DEFAULT_HEIGHT;

  return (
    <svg
      className={className}
      fill="none"
      height={actualHeight}
      role="img"
      viewBox={`0 0 ${DEFAULT_WIDTH} ${DEFAULT_HEIGHT}`}
      width={actualWidth}
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M18 26.3839L18 128.594H26L26 26.3839H18Z"
        fill={themeColor('illustrationSecondary')({ theme })}
      />
      <path
        d="M32 43.5982L47 43.5982L47 34.9911L32 34.9911L32 43.5982Z"
        fill={themeColor('illustrationSecondary')({ theme })}
      />
      <path
        d="M55 43.5982L70 43.5982L70 34.9911L55 34.9911L55 43.5982Z"
        fill={themeColor('illustrationSecondary')({ theme })}
      />
      <path d="M15 52.2054L87 52.2054" stroke="var(--echoes-color-icon-subdued)" strokeWidth="6" />
      <path
        d="M87 26.3839H19C16.7909 26.3839 15 28.1748 15 30.3839V126.746C15 128.955 16.7909 130.746 19 130.746H147C149.209 130.746 151 128.955 151 126.746V62.9643M136 26.3839H147C149.209 26.3839 151 28.1748 151 30.3839V42.5223"
        stroke="var(--echoes-color-icon-subdued)"
        strokeWidth="6"
      />
      <path
        d="M70.6736 103.733L59 91.1733L70.6736 78.614"
        stroke={themeColor('illustrationSecondary')({ theme })}
        strokeWidth="6"
      />
      <path
        d="M95.4744 78.614L107.148 91.1733L95.4744 103.733"
        stroke={themeColor('illustrationSecondary')({ theme })}
        strokeWidth="6"
      />
      <path
        d="M87.9937 71.5714L78.6187 109.9"
        stroke={themeColor('illustrationSecondary')({ theme })}
        strokeWidth="6"
      />
      <ellipse
        cx="22.5"
        cy="122.676"
        fill={themeColor('illustrationPrimary')({ theme })}
        rx="22.5"
        ry="22.5"
      />
      <path d="M14 121.063L21 128.594L34 114.607" stroke="white" strokeWidth="6" />
      <path
        d="M108.684 52.7433C116.712 48.065 123.243 41.1875 127.5 32.9269C131.757 41.1875 138.288 48.065 146.316 52.7433C138.288 57.4216 131.757 64.2991 127.5 72.5597C123.243 64.2991 116.712 57.4216 108.684 52.7433Z"
        stroke={themeColor('illustrationPrimary')({ theme })}
        strokeWidth="6"
      />
      <path
        d="M94.8732 23.1563C99.0981 20.5339 102.585 16.8739 105 12.5277C107.415 16.8739 110.902 20.5339 115.127 23.1563C110.902 25.7786 107.415 29.4386 105 33.7848C102.585 29.4386 99.0981 25.7786 94.8732 23.1563Z"
        stroke={themeColor('illustrationPrimary')({ theme })}
        strokeWidth="4.5"
      />
      <path
        d="M123.126 8.6317C124.893 7.43681 126.384 5.87768 127.5 4.06049C128.616 5.87768 130.107 7.43681 131.874 8.6317C130.107 9.82658 128.616 11.3857 127.5 13.2029C126.384 11.3857 124.893 9.82658 123.126 8.6317Z"
        stroke={themeColor('illustrationPrimary')({ theme })}
        strokeWidth="3"
      />
    </svg>
  );
}
