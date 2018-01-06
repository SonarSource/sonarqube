/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
/* eslint max-len: 0 */
import React from 'react';
import * as theme from '../../app/theme';

export default function PinIcon() {
  return (
    <svg width="9" height="14" viewBox="0 0 288 448">
      <path
        fill={theme.darkBlue}
        d="M120 216v-112q0-3.5-2.25-5.75t-5.75-2.25-5.75 2.25-2.25 5.75v112q0 3.5 2.25 5.75t5.75 2.25 5.75-2.25 2.25-5.75zM288 304q0 6.5-4.75 11.25t-11.25 4.75h-107.25l-12.75 120.75q-0.5 3-2.625 5.125t-5.125 2.125h-0.25q-6.75 0-8-6.75l-19-121.25h-101q-6.5 0-11.25-4.75t-4.75-11.25q0-30.75 19.625-55.375t44.375-24.625v-128q-13 0-22.5-9.5t-9.5-22.5 9.5-22.5 22.5-9.5h160q13 0 22.5 9.5t9.5 22.5-9.5 22.5-22.5 9.5v128q24.75 0 44.375 24.625t19.625 55.375z"
      />
    </svg>
  );
}
