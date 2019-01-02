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
import * as theme from '../../app/theme';

export default function PullRequestIcon({ className, fill = theme.blue, size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M13,11.9L13,5.5C13,5.4 13.232,1.996 7.9,2L9.1,0.8L8.5,0.1L5.9,2.6L8.5,5.1L9.2,4.4L7.905,3.008C12.256,2.99 12,5.4 12,5.5L12,11.9C11.1,12.1 10.5,12.9 10.5,13.8C10.5,14.9 11.4,15.8 12.5,15.8C13.6,15.8 14.5,14.9 14.5,13.8C14.5,12.9 13.9,12.2 13,11.9ZM4,11.9C4.9,12.2 5.5,12.9 5.5,13.8C5.5,14.9 4.6,15.8 3.5,15.8C2.4,15.8 1.5,14.9 1.5,13.8C1.5,12.9 2.1,12.1 3,11.9L3,4.1C2.1,3.9 1.5,3.1 1.5,2.2C1.5,1.1 2.4,0.2 3.5,0.2C4.6,0.2 5.5,1.1 5.5,2.2C5.5,3.1 4.9,3.9 4,4.1L4,11.9ZM12.5,14.9C11.9,14.9 11.5,14.5 11.5,13.9C11.5,13.3 11.9,12.9 12.5,12.9C13.1,12.9 13.5,13.3 13.5,13.9C13.5,14.5 13.1,14.9 12.5,14.9ZM3.5,14.9C2.9,14.9 2.5,14.5 2.5,13.9C2.5,13.3 2.9,12.9 3.5,12.9C4.1,12.9 4.5,13.3 4.5,13.9C4.5,14.5 4.1,14.9 3.5,14.9ZM2.5,2.2C2.5,1.6 2.9,1.2 3.5,1.2C4.1,1.2 4.5,1.6 4.5,2.2C4.5,2.8 4.1,3.2 3.5,3.2C2.9,3.2 2.5,2.8 2.5,2.2Z"
        style={{ fill }}
      />
    </Icon>
  );
}
