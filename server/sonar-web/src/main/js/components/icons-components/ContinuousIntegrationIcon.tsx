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

export default function ContinuousIntegrationIcon({
  className,
  fill = 'currentColor',
  size
}: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M13.805 9.25c0 .016 0 .04-.008.055C13.133 12.07 10.852 14 7.969 14c-1.524 0-3-.602-4.11-1.656l-1.007 1.008a.497.497 0 0 1-.352.148.504.504 0 0 1-.5-.5V9.5c0-.273.227-.5.5-.5H6c.273 0 .5.227.5.5a.497.497 0 0 1-.148.352l-1.07 1.07a3.988 3.988 0 0 0 6.125-.828c.187-.305.28-.602.413-.914.04-.11.117-.18.235-.18h1.5c.14 0 .25.117.25.25zM14 3v3.5c0 .273-.227.5-.5.5H10a.504.504 0 0 1-.5-.5c0-.133.055-.258.148-.352l1.079-1.078A4.019 4.019 0 0 0 8 4c-1.39 0-2.68.719-3.406 1.906-.188.305-.282.602-.414.914-.04.11-.117.18-.235.18H2.391a.252.252 0 0 1-.25-.25v-.055C2.812 3.922 5.117 2 8 2c1.531 0 3.023.61 4.133 1.656l1.015-1.008A.497.497 0 0 1 13.5 2.5c.273 0 .5.227.5.5z"
        style={{ fill }}
      />
    </Icon>
  );
}
