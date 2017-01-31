/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

export default class BugIcon extends React.Component {
  render () {
    /* eslint-disable max-len */
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 14 14" width="14" height="14" style={{ position: 'relative', top: -1, verticalAlign: 'middle' }}>
          <g transform="matrix(1,0,0,1,0.495158,0.453789)">
            <path style={{ fill: 'currentColor' }} d="M10.3 8l1.4 1.2.7-.8L10.7 7H9v-.3l2-2.3V2h-1v2L9 5.1V4h-.2c-.1-.8-.6-1.5-1.3-1.8L8.9.8 8.1.1 6.5 1.7 4.9.1l-.7.7 1.4 1.4c-.8.3-1.3 1-1.4 1.8H4v1.1L3 4V2H2v2.3l2 2.3V7H2.3L.7 8.4l.7.8L2.7 8H4v.3l-2 1.9V13h1v-2.4l1-1C4 11 5.1 12 6.4 12h.8c.7 0 1.4-.3 1.8-.9.3-.4.3-.9.2-1.4l.9.9V13h1v-2.8L9 8.3V8h1.3zM6 10V4.3h1V10H6z"/>
          </g>
        </svg>
    );
  }
}
