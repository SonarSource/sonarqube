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

/** @deprecated Use IconError from Echoes instead, if possible.
 *
 * Be aware that the full icon set is not yet available in Echoes, and therefore you may not be able
 * to replace all of the icons yet. There are situations where it is OK to ignore this deprecation
 * warning when revisiting old code, but all new code should use the icons from Echoes.
 */
export function FlagErrorIcon({ fill = 'iconError', ...iconProps }: IconProps) {
  const theme = useTheme();
  return (
    <CustomIcon {...iconProps}>
      <path
        d="M7.364 1.707a1 1 0 0 1 1.414 0l5.657 5.657a1 1 0 0 1 0 1.414l-5.657 5.657a1 1 0 0 1-1.414 0L1.707 8.778a1 1 0 0 1 0-1.414l5.657-5.657ZM7 5a1 1 0 0 1 2 0v3a1 1 0 1 1-2 0V5Zm1 5a1 1 0 1 0 0 2 1 1 0 0 0 0-2Z"
        style={{ fill: themeColor(fill)({ theme }) }}
      />
    </CustomIcon>
  );
}
