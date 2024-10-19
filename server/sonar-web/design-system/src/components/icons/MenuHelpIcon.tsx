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

/** @deprecated Use IconQuestionMark from Echoes instead, if possible.
 *
 * Be aware that the full icon set is not yet available in Echoes, and therefore you may not be able
 * to replace all of the icons yet. There are situations where it is OK to ignore this deprecation
 * warning when revisiting old code, but all new code should use the icons from Echoes.
 */
export function MenuHelpIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  const theme = useTheme();
  return (
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16Zm.507-5.451H6.66v-.166c.005-1.704.462-2.226 1.28-2.742.6-.38 1.062-.803 1.062-1.441 0-.677-.53-1.116-1.188-1.116-.638 0-1.227.424-1.257 1.218H4.571c.044-1.948 1.486-2.873 3.254-2.873 1.933 0 3.307.993 3.307 2.698 0 1.144-.595 1.86-1.505 2.4-.77.463-1.11.906-1.12 1.856v.166Zm.282 1.948a1.185 1.185 0 0 1-1.169 1.169 1.164 1.164 0 1 1 0-2.328c.624 0 1.164.52 1.169 1.159Z"
        fill={themeColor(fill)({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
