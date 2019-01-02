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
import * as classNames from 'classnames';
import Icon, { IconProps } from './Icon';
import * as theme from '../../app/theme';

export interface Props extends IconProps {
  filled?: boolean;
}

export default function HomeIcon({ className, fill = theme.orange, filled = false, size }: Props) {
  return (
    <Icon
      className={classNames(className, 'icon-outline', { 'is-filled': filled })}
      size={size}
      style={{ color: fill }}>
      <g transform="matrix(0.870918,0,0,0.870918,0.978227,0.978227)">
        <path d="M15.9,7.8L8.2,0.1C8.1,0 7.9,0 7.8,0.1L0.1,7.8C0,7.9 0,8.1 0.1,8.2C0.2,8.3 0.2,8.3 0.3,8.3L2.2,8.3L2.2,15.8C2.2,15.9 2.2,15.9 2.3,16C2.3,16 2.4,16.1 2.5,16.1L6.2,16.1C6.3,16.1 6.5,16 6.5,15.8L6.5,10.5L9.7,10.5L9.7,15.8C9.7,15.9 9.8,16.1 10,16.1L13.7,16.1C13.8,16.1 14,16 14,15.8L14,8.2L15.9,8.2C16,8.2 16,8.2 16.1,8.1C16,8 16.1,7.9 15.9,7.8Z" />
      </g>
    </Icon>
  );
}
