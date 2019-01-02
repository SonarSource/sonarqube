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

export default function RecommendedIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M15.089 13.199l-1.742-3.736c-0.962 1.401-2.464 2.398-4.203 2.701l1.459 3.128c0.186 0.4 0.764 0.373 0.914-0.040l0.748-2.054 0.154-0.072 2.054 0.748c0.412 0.151 0.804-0.276 0.618-0.675zM8.040 0.384c-3.003 0-5.446 2.443-5.446 5.446s2.443 5.446 5.446 5.446c3.003 0 5.446-2.443 5.446-5.446s-2.443-5.446-5.446-5.446zM10.689 5.429l-0.966 0.941 0.228 1.33c0.070 0.406-0.358 0.711-0.718 0.522l-1.194-0.628-1.194 0.628c-0.363 0.19-0.788-0.118-0.718-0.522l0.228-1.33-0.966-0.941c-0.293-0.286-0.131-0.786 0.274-0.844l1.335-0.194 0.597-1.209c0.181-0.367 0.707-0.368 0.888 0l0.597 1.209 1.335 0.194c0.405 0.059 0.568 0.558 0.274 0.844zM2.732 9.463l-1.742 3.736c-0.187 0.4 0.208 0.825 0.618 0.674l2.054-0.748 0.154 0.072 0.748 2.054c0.15 0.412 0.727 0.441 0.914 0.040l1.459-3.128c-1.739-0.302-3.241-1.3-4.203-2.701z"
        style={{ fill }}
      />
    </Icon>
  );
}
