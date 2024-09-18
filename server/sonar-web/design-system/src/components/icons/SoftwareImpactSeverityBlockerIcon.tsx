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

export function SoftwareImpactSeverityBlockerIcon({
  disabled,
  ...iconProps
}: IconProps & { disabled?: boolean }) {
  const theme = useTheme();
  const color = disabled
    ? 'iconSoftwareImpactSeverityDisabled'
    : 'iconSoftwareImpactSeverityBlocker';

  return (
    <CustomIcon viewBox="0 0 14 14" {...iconProps}>
      <path
        clipRule="evenodd"
        d="M7 13.375C10.5208 13.375 13.375 10.5208 13.375 7C13.375 3.47918 10.5208 0.625 7 0.625C3.47918 0.625 0.625 3.47918 0.625 7C0.625 10.5208 3.47918 13.375 7 13.375ZM4 6C3.44772 6 3 6.44772 3 7C3 7.55228 3.44772 8 4 8H10C10.5523 8 11 7.55228 11 7C11 6.44772 10.5523 6 10 6H4Z"
        fill={themeColor(color)({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
