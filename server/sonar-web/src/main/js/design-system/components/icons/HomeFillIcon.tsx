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

/** @deprecated Use IconHome with the isFilled prop from Echoes instead, if possible.
 *
 * Be aware that the full icon set is not yet available in Echoes, and therefore you may not be able
 * to replace all of the icons yet. There are situations where it is OK to ignore this deprecation
 * warning when revisiting old code, but all new code should use the icons from Echoes.
 */
export function HomeFillIcon({ fill = 'iconFavorite', ...iconProps }: IconProps) {
  const theme = useTheme();
  const fillColor = themeColor(fill)({ theme });
  return (
    <CustomIcon {...iconProps}>
      <path
        d="M6.9995 0.280296C6.602 0.280296 6.21634 0.415622 5.906 0.664003L0.657 4.864C0.242 5.196 0 5.699 0 6.23V13.25C0 13.7141 0.184374 14.1593 0.512563 14.4874C0.840752 14.8156 1.28587 15 1.75 15H5.25C5.44891 15 5.63968 14.921 5.78033 14.7803C5.92098 14.6397 6 14.4489 6 14.25V9H8V14.25C8 14.4489 8.07902 14.6397 8.21967 14.7803C8.36032 14.921 8.55109 15 8.75 15H12.25C12.7141 15 13.1592 14.8156 13.4874 14.4874C13.8156 14.1593 14 13.7141 14 13.25V6.231C14 5.699 13.758 5.196 13.343 4.864L8.093 0.664003C7.78266 0.415622 7.397 0.280296 6.9995 0.280296Z"
        fill={fillColor}
      />
    </CustomIcon>
  );
}
