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

export function CollapseIcon({ fill = 'currentColor', ...iconProps }: Readonly<IconProps>) {
  const theme = useTheme();
  return (
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M2.82364 9h4.146a.25.25 0 0 1 .25.25v4.146a.2499.2499 0 0 1-.04199.1391.2503.2503 0 0 1-.11227.0923.24953.24953 0 0 1-.14463.0142.24976.24976 0 0 1-.12811-.0686l-1.543-1.543-2.98844 2.9884a.75009.75009 0 0 1-.52453.2012.75044.75044 0 0 1-.5171-.2196.75.75 0 0 1-.01837-1.0416L4.18964 10.97l-1.543-1.543a.24997.24997 0 0 1-.05439-.27273A.24995.24995 0 0 1 2.82364 9ZM13.396 7.24258H9.24996a.25.25 0 0 1-.25-.25v-4.146a.25.25 0 0 1 .427-.177l1.54304 1.543 3.0112-3.01129a.74997.74997 0 0 1 1.0416.01838.75042.75042 0 0 1 .2196.5171.75005.75005 0 0 1-.2012.52452L12.03 5.27258l1.543 1.543a.2505.2505 0 0 1 .0686.12811.25036.25036 0 0 1-.0142.14463.2503.2503 0 0 1-.0923.11227.2499.2499 0 0 1-.1391.04199Z"
        fill={themeColor(fill)({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
