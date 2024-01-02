/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import classNames from 'classnames';
import { Badge } from 'design-system';
import React from 'react';

interface Props {
  method: string;
}

export default function RestMethodPill({ method }: Readonly<Props>) {
  const getMethodClassName = (): string => {
    switch (method.toLowerCase()) {
      case 'get':
        return 'sw-bg-green-200';
      case 'delete':
        return 'sw-bg-red-200';
      case 'post':
        return 'sw-bg-blue-200';
      case 'put':
        return 'sw-bg-purple-200';
      case 'patch':
        return 'sw-bg-yellow-200';
      default:
        return 'sw-bg-gray-200';
    }
  };

  return (
    <Badge
      className={classNames(
        'sw-self-center sw-align-middle sw-min-w-[50px] sw-text-center',
        getMethodClassName(),
      )}
    >
      {method.toUpperCase()}
    </Badge>
  );
}
