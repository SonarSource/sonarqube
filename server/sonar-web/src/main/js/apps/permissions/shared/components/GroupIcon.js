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
import React from 'react';

const GroupIcon = () => {
  /* eslint max-len: 0 */
  return (
    <div style={{ padding: '4px 3px 0' }}>
      <svg xmlns="http://www.w3.org/2000/svg" width="30" height="28" viewBox="0 0 480 448">
        <path
          fill="#aaa"
          d="M148.25 224q-40.5 1.25-66.25 32H48.5Q28 256 14 245.875T0 216.25Q0 128 31 128q1.5 0 10.875 5.25t24.375 10.625T96 149.25q16.75 0 33.25-5.75Q128 152.75 128 160q0 34.75 20.25 64zM416 383.25q0 30-18.25 47.375T349.25 448h-218.5q-30.25 0-48.5-17.375T64 383.25q0-13.25.875-25.875t3.5-27.25T75 303t10.75-24.375 15.5-20.25T122.625 245t27.875-5q2.5 0 10.75 5.375t18.25 12 26.75 12T240 274.75t33.75-5.375 26.75-12 18.25-12T329.5 240q15.25 0 27.875 5t21.375 13.375 15.5 20.25T405 303t6.625 27.125 3.5 27.25.875 25.875zM160 64q0 26.5-18.75 45.25T96 128t-45.25-18.75T32 64t18.75-45.25T96 0t45.25 18.75T160 64zm176 96q0 39.75-28.125 67.875T240 256t-67.875-28.125T144 160t28.125-67.875T240 64t67.875 28.125T336 160zm144 56.25q0 19.5-14 29.625T431.5 256H398q-25.75-30.75-66.25-32Q352 194.75 352 160q0-7.25-1.25-16.5 16.5 5.75 33.25 5.75 14.75 0 29.75-5.375t24.375-10.625T449 128q31 0 31 88.25zM448 64q0 26.5-18.75 45.25T384 128t-45.25-18.75T320 64t18.75-45.25T384 0t45.25 18.75T448 64z"
        />
      </svg>
    </div>
  );
};

export default GroupIcon;
