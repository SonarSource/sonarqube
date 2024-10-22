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

/** @deprecated Use IconInheritance from Echoes instead, if possible.
 *
 * Be aware that the full icon set is not yet available in Echoes, and therefore you may not be able
 * to replace all of the icons yet. There are situations where it is OK to ignore this deprecation
 * warning when revisiting old code, but all new code should use the icons from Echoes.
 */
export function InheritanceIcon({ fill = 'currentColor', ...iconProps }: Readonly<IconProps>) {
  const theme = useTheme();
  const fillColor = themeColor(fill)({ theme });
  return (
    <CustomIcon {...iconProps}>
      <mask fill="white" id="path-1-inside-1_3266_8058">
        <rect height="6" rx="0.5" width="6" x="1" y="1" />
      </mask>
      <rect
        height="6"
        mask="url(#path-1-inside-1_3266_8058)"
        rx="0.5"
        stroke={fillColor}
        strokeWidth="3"
        width="6"
        x="1"
        y="1"
      />
      <mask fill="white" id="path-2-inside-2_3266_8058">
        <rect height="6" rx="0.5" width="6" x="9" y="9" />
      </mask>
      <rect
        height="6"
        mask="url(#path-2-inside-2_3266_8058)"
        rx="0.5"
        stroke={fillColor}
        strokeWidth="3"
        width="6"
        x="9"
        y="9"
      />
      <path d="M4 7V11C4 11.5523 4.44772 12 5 12H9" stroke={fillColor} strokeWidth="1.5" />
    </CustomIcon>
  );
}
