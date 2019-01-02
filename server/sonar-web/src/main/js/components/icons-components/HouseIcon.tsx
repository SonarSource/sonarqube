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

export default function HouseIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M13.002 8.848v4.168a.56.56 0 0 1-.556.555H9.11v-3.334H6.89v3.334H3.554a.56.56 0 0 1-.556-.555V8.848c0-.018.01-.035.01-.052L8 4.68l4.993 4.116c.009.017.009.034.009.052zm1.936-.6l-.538.643a.289.289 0 0 1-.183.096h-.026a.273.273 0 0 1-.182-.061L8 3.916l-6.009 5.01a.297.297 0 0 1-.208.06.289.289 0 0 1-.183-.095l-.538-.642a.285.285 0 0 1 .035-.391L7.34 2.656a1.07 1.07 0 0 1 1.32 0l2.119 1.772V2.735c0-.157.121-.278.278-.278h1.667c.156 0 .278.121.278.278v3.542l1.901 1.58c.113.096.13.279.035.392z"
        style={{ fill }}
      />
    </Icon>
  );
}
