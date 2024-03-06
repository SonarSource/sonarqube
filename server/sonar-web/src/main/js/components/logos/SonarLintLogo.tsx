/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { uniqueId } from 'lodash';
import * as React from 'react';
import { isDefined } from '../../helpers/types';

interface Props {
  className?: string;
  size?: number;
  description?: React.ReactNode;
}

export function SonarLintLogo({ className, description, size }: Readonly<Props>) {
  const id = uniqueId('icon');
  return (
    <svg
      className={className}
      height={size}
      style={{
        fillRule: 'evenodd',
        clipRule: 'evenodd',
        strokeLinejoin: 'round',
        strokeMiterlimit: 1.41421,
      }}
      version="1.1"
      viewBox="0 0 512 512"
      width={size}
      xmlnsXlink="http://www.w3.org/1999/xlink"
      xmlSpace="preserve"
      aria-describedby={isDefined(description) && description !== '' ? id : undefined}
    >
      {isDefined(description) && description !== '' && <desc id={id}>{description}</desc>}
      <defs>
        <path id="a" d="M0 0h512v512H0z" />
      </defs>
      <clipPath id="b">
        <use xlinkHref="#a" style={{ overflow: 'visible' }} />
      </clipPath>
      <path
        d="M255.7 450.7c-107.8 0-195.5-87.6-195.5-195.3 0-107.7 87.7-195.3 195.5-195.3s195.5 87.6 195.5 195.3c0 107.7-87.7 195.3-195.5 195.3m0-355.2c-88.2 0-160 71.7-160 159.8 0 88.1 71.8 159.8 160 159.8s160-71.7 160-159.8c0-88.1-71.8-159.8-160-159.8"
        style={{ clipPath: 'url(#b)', fill: '#cb2029' }}
      />
      <path
        d="M392.6 244.9c-5.1-10.2-11.5-23-24.1-23-12.5 0-18.9 12.7-24.1 23-4.8 9.5-7.5 14.5-13.5 14.5s-8.7-5-13.5-14.5c-5.1-10.2-11.5-23-24.1-23s-18.9 12.7-24.1 23c-4.8 9.5-7.5 14.5-13.5 14.5s-8.7-5-13.5-14.5c-5.1-10.2-11.5-23-24.1-23s-18.9 12.7-24.1 23c-4.8 9.5-7.5 14.5-13.5 14.5s-8.7-5-13.5-14.5c-5.1-10.2-11.5-23-24.1-23-12.5 0-18.9 12.1-24 22.3 0 0-3.9 8.7-5.6 12.8-1.7 4.1-8.3 19.7-2.3 27.2 3.4 4.2 8.3-1.8 11.1-6.2 2.1-3.4 7.1-13.2 7.1-13.2 5.1-9.1 7.8-14 13.7-14 6 0 8.7 5 13.5 14.5 5.1 10.2 11.5 23 24.1 23 12.5 0 18.9-12.7 24.1-23 4.8-9.5 7.5-14.5 13.5-14.5s8.7 5 13.5 14.5c5.1 10.2 11.5 23 24.1 23 12.5 0 18.9-12.7 24.1-23 4.8-9.5 7.5-14.5 13.5-14.5s8.7 5 13.5 14.5c5.1 10.2 11.5 23 24.1 23s18.9-12.7 24.1-23c4.8-9.5 7.5-14.5 13.5-14.5s8.7 5 13.5 14.5c0 0 2.2 4.3 5.4 9.9 1.4 2.3 6.6 11.1 11.2 10.8 4.1-.3 5.6-13.3 1-24.7-3.1-7.6-7-16.4-7-16.4z"
        style={{ fill: '#cb2029' }}
      />
    </svg>
  );
}
