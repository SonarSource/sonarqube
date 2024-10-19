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
import { themeColor, themeContrast } from '../../helpers/theme';
import { CustomIcon, IconProps } from './Icon';

/** @deprecated Use IconInfo from Echoes instead, if possible.
 *
 * Be aware that the full icon set is not yet available in Echoes, and therefore you may not be able
 * to replace all of the icons yet. There are situations where it is OK to ignore this deprecation
 * warning when revisiting old code, but all new code should use the icons from Echoes.
 */
export function SeverityInfoIcon({ fill = 'iconSeverityInfo', ...iconProps }: IconProps) {
  const theme = useTheme();

  return (
    <CustomIcon {...iconProps}>
      <circle cx="8" cy="8" fill={themeColor(fill)({ theme })} r="7" />
      <path
        clipRule="evenodd"
        d="M6.5 7.75C6.5 7.55109 6.57902 7.36032 6.71967 7.21967C6.86032 7.07902 7.05109 7 7.25 7H8.25C8.44891 7 8.63968 7.07902 8.78033 7.21967C8.92098 7.36032 9 7.55109 9 7.75V10.5H9.25C9.44891 10.5 9.63968 10.579 9.78033 10.7197C9.92098 10.8603 10 11.0511 10 11.25C10 11.4489 9.92098 11.6397 9.78033 11.7803C9.63968 11.921 9.44891 12 9.25 12H7.25C7.05109 12 6.86032 11.921 6.71967 11.7803C6.57902 11.6397 6.5 11.4489 6.5 11.25C6.5 11.0511 6.57902 10.8603 6.71967 10.7197C6.86032 10.579 7.05109 10.5 7.25 10.5H7.5V8.5H7.25C7.05109 8.5 6.86032 8.42098 6.71967 8.28033C6.57902 8.13968 6.5 7.94891 6.5 7.75ZM8 6C8.26522 6 8.51957 5.89464 8.70711 5.70711C8.89464 5.51957 9 5.26522 9 5C9 4.73478 8.89464 4.48043 8.70711 4.29289C8.51957 4.10536 8.26522 4 8 4C7.73478 4 7.48043 4.10536 7.29289 4.29289C7.10536 4.48043 7 4.73478 7 5C7 5.26522 7.10536 5.51957 7.29289 5.70711C7.48043 5.89464 7.73478 6 8 6Z"
        fill={themeContrast(fill)({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
