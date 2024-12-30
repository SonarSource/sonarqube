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
import { themeColor } from '../../helpers/theme';
import { CustomIcon, IconProps } from './Icon';

export function OverridenIcon({ fill = 'currentColor', ...iconProps }: Readonly<IconProps>) {
  const theme = useTheme();
  const fillColor = themeColor(fill)({ theme });
  return (
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M2.5 2.5L2.5 5.5L5.5 5.5L5.5 2.5L2.5 2.5ZM1.5 1C1.22386 1 1 1.22386 1 1.5L0.999999 6.5C0.999999 6.77614 1.22386 7 1.5 7L6.5 7C6.77614 7 7 6.77614 7 6.5L7 1.5C7 1.22386 6.77614 1 6.5 1L1.5 1Z"
        fill={fillColor}
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M3.25 11V7H4.78913V11C4.78913 11.1381 4.90398 11.25 5.04565 11.25H9.15V12.75H5.04565C4.05394 12.75 3.25 11.9665 3.25 11Z"
        fill={fillColor}
        fillRule="evenodd"
      />
      <circle cx="11.5" cy="11.5" fill={fillColor} r="3.5" />
    </CustomIcon>
  );
}
