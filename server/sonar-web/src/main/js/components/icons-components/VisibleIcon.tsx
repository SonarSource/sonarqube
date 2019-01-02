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

export default function VisibleIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M13.524 8.403q-1.093-1.697-2.74-2.539 0.439 0.748 0.439 1.618 0 1.331-0.946 2.276t-2.276 0.946-2.276-0.946-0.946-2.276q0-0.87 0.439-1.618-1.647 0.842-2.74 2.539 0.957 1.474 2.399 2.348t3.125 0.874 3.125-0.874 2.399-2.348zM8.345 5.641q0-0.144-0.101-0.245t-0.245-0.101q-0.899 0-1.543 0.644t-0.644 1.543q0 0.144 0.101 0.245t0.245 0.101 0.245-0.101 0.101-0.245q0-0.619 0.439-1.057t1.057-0.439q0.144 0 0.245-0.101t0.101-0.245zM14.444 8.403q0 0.245-0.144 0.496-1.007 1.654-2.708 2.65t-3.593 0.996-3.593-1-2.708-2.647q-0.144-0.252-0.144-0.496t0.144-0.496q1.007-1.647 2.708-2.647t3.593-1 3.593 1 2.708 2.647q0.144 0.252 0.144 0.496z"
        style={{ fill }}
      />
    </Icon>
  );
}
