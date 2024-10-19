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

/** @deprecated Use IconCodeSmell from Echoes instead, if possible.
 *
 * Be aware that the full icon set is not yet available in Echoes, and therefore you may not be able
 * to replace all of the icons yet. There are situations where it is OK to ignore this deprecation
 * warning when revisiting old code, but all new code should use the icons from Echoes.
 */
export function CodeSmellIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  const theme = useTheme();

  return (
    <CustomIcon {...iconProps}>
      <path
        d="M8,15.1a7,7,0,1,0-7-7A7,7,0,0,0,8,15.1Zm.74-8.9,1.46-2.52a.29.29,0,0,1,.25-.14.3.3,0,0,1,.15,0,5.26,5.26,0,0,1,2.61,4.53.28.28,0,0,1-.29.29H10a.28.28,0,0,1-.29-.29,1.78,1.78,0,0,0-.88-1.51A.29.29,0,0,1,8.75,6.2Zm.11,3.44A.23.23,0,0,1,9,9.6a.29.29,0,0,1,.25.14l1.46,2.52a.18.18,0,0,1,0,.13.3.3,0,0,1-.15.27,5.3,5.3,0,0,1-5.23,0,.3.3,0,0,1-.1-.4L6.73,9.74A.29.29,0,0,1,7,9.6a.23.23,0,0,1,.14,0A1.79,1.79,0,0,0,8.86,9.64ZM5.33,3.59a.3.3,0,0,1,.41.1L7.2,6.21a.29.29,0,0,1-.1.4,1.79,1.79,0,0,0-.87,1.51.28.28,0,0,1-.29.29H3a.32.32,0,0,1-.32-.29A5.26,5.26,0,0,1,5.33,3.59Z"
        fill={themeColor(fill)({ theme })}
      />
    </CustomIcon>
  );
}
