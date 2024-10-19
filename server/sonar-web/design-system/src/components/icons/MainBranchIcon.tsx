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

/** @deprecated Use IconBranch from Echoes instead, if possible.
 *
 * Be aware that the full icon set is not yet available in Echoes, and therefore you may not be able
 * to replace all of the icons yet. There are situations where it is OK to ignore this deprecation
 * warning when revisiting old code, but all new code should use the icons from Echoes.
 */
export function MainBranchIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  const theme = useTheme();
  return (
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M8.251 2.49932a.75003.75003 0 0 0-.75.75.75001.75001 0 1 0 .75-.75Zm-2.25.75A2.25004 2.25004 0 0 1 7.21713 1.2516a2.25 2.25 0 0 1 2.33319.16148 2.24917 2.24917 0 0 1 .76538.94287c.1639.37851.2206.79478.1639 1.20334a2.25026 2.25026 0 0 1-.48534 1.11323 2.25 2.25 0 0 1-.99326.6988v5.25598c.50069.177.92271.5252 1.1915.9832.2687.458.3669.9963.2771 1.5197a2.25092 2.25092 0 0 1-2.2186 1.8705 2.25092 2.25092 0 0 1-2.21861-1.8705 2.25115 2.25115 0 0 1 .27716-1.5197 2.2514 2.2514 0 0 1 1.19145-.9832V5.37132a2.24999 2.24999 0 0 1-1.5-2.122Zm2.25 8.74998a.74985.74985 0 0 0-.53033.2197.74987.74987 0 0 0-.21967.5303c0 .1989.07902.3897.21967.5304a.75017.75017 0 0 0 1.06066 0 .75023.75023 0 0 0 .21967-.5304.74983.74983 0 0 0-.21967-.5303.74981.74981 0 0 0-.53033-.2197Z"
        fill={themeColor(fill)({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
