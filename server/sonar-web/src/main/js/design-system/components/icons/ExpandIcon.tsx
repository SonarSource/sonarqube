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

/** @deprecated Use IconExpand from Echoes instead, if possible.
 *
 * Be aware that the full icon set is not yet available in Echoes, and therefore you may not be able
 * to replace all of the icons yet. There are situations where it is OK to ignore this deprecation
 * warning when revisiting old code, but all new code should use the icons from Echoes.
 */
export function ExpandIcon({ fill = 'currentColor', ...iconProps }: Readonly<IconProps>) {
  const theme = useTheme();
  return (
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M10.604 1h4.146a.24994.24994 0 0 1 .25.25v4.146a.24966.24966 0 0 1-.042.13913.24968.24968 0 0 1-.1122.09226.25028.25028 0 0 1-.1447.01423.25034.25034 0 0 1-.1281-.06862L13.03 4.03l-2.9884 2.98838a.75002.75002 0 0 1-1.06001-1.06L11.97 2.97l-1.543-1.543a.25047.25047 0 0 1-.0686-.1281.25035.25035 0 0 1 .0142-.14463.25027.25027 0 0 1 .0923-.11228A.25.25 0 0 1 10.604 1ZM5.39608 14.9813h-4.146a.2499.2499 0 0 1-.17677-.0732.25007.25007 0 0 1-.07323-.1768v-4.146a.25001.25001 0 0 1 .04199-.1391.25037.25037 0 0 1 .11228-.0923.24953.24953 0 0 1 .14463-.0142.24973.24973 0 0 1 .1281.0686l1.543 1.543L5.98132 8.94a.75.75 0 0 1 1.06 1.06l-3.01124 3.0113 1.543 1.543a.24987.24987 0 0 1 .06862.1281.24996.24996 0 0 1-.10649.2569.24964.24964 0 0 1-.13913.042Z"
        fill={themeColor(fill)({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
