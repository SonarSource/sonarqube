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

export default function TagsIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M4.303 5.36a.94.94 0 0 0-.944-.945.94.94 0 0 0-.944.944c0 .524.42.944.944.944a.94.94 0 0 0 .944-.944zm7.866 4.246a.95.95 0 0 1-.273.663l-3.62 3.627a.95.95 0 0 1-1.334 0L1.671 8.618C1.295 8.249 1 7.534 1 7.01V3.944A.95.95 0 0 1 1.944 3H5.01c.523 0 1.238.295 1.614.67l5.271 5.265a.98.98 0 0 1 .273.67zm2.831 0a.95.95 0 0 1-.273.663l-3.62 3.627a.98.98 0 0 1-.67.273c-.384 0-.575-.177-.826-.435l3.465-3.465a.95.95 0 0 0 0-1.334L7.805 3.67C7.429 3.295 6.714 3 6.19 3h1.651c.524 0 1.239.295 1.615.67l5.271 5.265a.98.98 0 0 1 .273.67z"
        style={{ fill }}
      />
    </Icon>
  );
}
