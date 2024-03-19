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
import { themeColor } from '../../helpers';
import { CustomIcon, IconProps } from './Icon';

export function AddNewIcon({ fill = 'currentColor', ...iconProps }: Readonly<IconProps>) {
  const theme = useTheme();

  return (
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M8 0c-.55228 0-1 .44771-1 1v6H1c-.55229 0-1 .44771-1 1 0 .55228.44771 1 1 1h6v6c0 .5523.44772 1 1 1 .55229 0 1-.4477 1-1V9h6c.5523 0 1-.44771 1-1 0-.55228-.4477-1-1-1H9V1c0-.55229-.44771-1-1-1Z"
        fill={themeColor(fill)({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
