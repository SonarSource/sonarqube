/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import Icon, { IconProps } from './Icon';

interface DropdownIconProps {
  turned?: boolean;
}

export default function DropdownIcon({
  className,
  fill = 'currentColor',
  size = 16,
  turned = false
}: IconProps & DropdownIconProps) {
  return (
    <Icon
      className={className}
      height={size}
      style={turned ? { transform: 'rotate(180deg)' } : undefined}
      viewBox="0 0 7 16"
      width={(size / 16) * 7}>
      <path
        d="M7 6.469a.42.42 0 0 1-.13.307L3.808 9.84a.42.42 0 0 1-.308.13.42.42 0 0 1-.308-.13L.13 6.776A.42.42 0 0 1 0 6.47a.42.42 0 0 1 .13-.308.42.42 0 0 1 .307-.13h6.126a.42.42 0 0 1 .307.13.42.42 0 0 1 .13.308z"
        style={{ fill }}
      />
    </Icon>
  );
}
