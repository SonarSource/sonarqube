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

export function StatusResolvedIcon({ fill = 'iconStatusResolved', ...iconProps }: IconProps) {
  const theme = useTheme();

  return (
    <CustomIcon {...iconProps}>
      <circle cx="8" cy="8" fill={themeColor(fill)({ theme })} r="7" />
      <path
        clipRule="evenodd"
        d="M11.3105 6.22789c.2884.29737.2811.77219-.0163 1.06054L8.27211 10.25c-.29414.2852-.76273.2816-1.05244-.0081l-2-1.99999c-.29289-.2929-.29289-.76777 0-1.06066.29289-.2929.76777-.2929 1.06066 0L7.7581 8.65901 10.25 6.21158c.2974-.28835.7722-.28105 1.0605.01631Z"
        fill={themeContrast('iconStatusResolved')({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
