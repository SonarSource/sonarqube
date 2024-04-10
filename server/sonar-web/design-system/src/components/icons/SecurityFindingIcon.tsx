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

/** @deprecated Use IconSecurityFinding from Echoes instead, if possible.
 *
 * Be aware that the full icon set is not yet available in Echoes, and therefore you may not be able
 * to replace all of the icons yet. There are situations where it is OK to ignore this deprecation
 * warning when revisiting old code, but all new code should use the icons from Echoes.
 */
export function SecurityFindingIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  const theme = useTheme();
  const fillColor = themeColor(fill)({ theme });
  return (
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M13.2114 3.76857a.8571.8571 0 0 0-.5743-.66L8.13714 1.85714a.90869.90869 0 0 0-.42857 0l-4.5 1.25143c-.3046.08786-.52937.34618-.57429.66-.06857.48857-.63428 4.82572.97715 7.12283a7.7138 7.7138 0 0 0 4.11428 2.9657.72308.72308 0 0 0 .19714 0 .66187.66187 0 0 0 .18857 0 7.65392 7.65392 0 0 0 4.12288-2.9657c1.5732-2.27896 1.0457-6.56583.9786-7.11053l-.0015-.0123Zm-1.6028 4.08857a5.27096 5.27096 0 0 1-.7372 2.07429A6.78813 6.78813 0 0 1 8 12.1429V7.85714h3.6086Zm-3.6086 0H4.22a20.81886 20.81886 0 0 1 0-3.27428L8 3.57143v4.28571Z"
        fill={fillColor}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
