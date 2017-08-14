/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';

/*::
type Props = { className?: string, size?: number };
*/

export default function OrganizationIcon({ className, size = 16 } /*: Props */) {
  /* eslint-disable max-len */
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      height={size}
      width={size}
      viewBox="0 0 16 16">
      <path
        style={{ fill: '#4b9fd5' }}
        d="M13.5 6c-.4 0-.7.1-1.1.2L11 4.8v-.3C11 3.1 9.9 2 8.5 2S6 3.1 6 4.5v.2L4.5 6.2c-.3-.1-.7-.2-1-.2C2.1 6 1 7.1 1 8.5S2.1 11 3.5 11 6 9.9 6 8.5c0-.7-.3-1.3-.7-1.7l1-1c.4.6 1 1 1.7 1.1V9c-1.1.2-2 1.2-2 2.4C6 12.9 7.1 14 8.5 14s2.5-1.1 2.5-2.5c0-1.2-.9-2.2-2-2.4V6.9c.7-.1 1.2-.5 1.6-1.1l1 1c-.4.4-.6 1-.6 1.6 0 1.4 1.1 2.5 2.5 2.5s2.5-1 2.5-2.4S14.9 6 13.5 6zm-10 4C2.7 10 2 9.3 2 8.5S2.7 7 3.5 7 5 7.7 5 8.5 4.3 10 3.5 10zm6.5 1.5c0 .8-.7 1.5-1.5 1.5S7 12.3 7 11.5 7.7 10 8.5 10s1.5.7 1.5 1.5zM8.5 6C7.7 6 7 5.3 7 4.5S7.7 3 8.5 3s1.5.7 1.5 1.5S9.3 6 8.5 6zm5 4c-.8 0-1.5-.7-1.5-1.5S12.7 7 13.5 7s1.5.7 1.5 1.5-.7 1.5-1.5 1.5z"
      />
    </svg>
  );
}
