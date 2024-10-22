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

export function ChevronLeftIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  const theme = useTheme();
  return (
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M10.16801 12.7236c-.19527.1953-.51185.1953-.70711 0l-4.185-4.18499c-.1953-.19526-.1953-.51184 0-.7071l4.185-4.18503c.19526-.19527.51184-.19527.70711 0 .19526.19526.19526.51184 0 .7071L6.33653 8.18506l3.83148 3.83144c.19526.1953.19526.5119 0 .7071Z"
        fill={themeColor(fill)({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
