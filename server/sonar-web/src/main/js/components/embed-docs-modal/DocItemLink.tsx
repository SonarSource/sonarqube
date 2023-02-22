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

import { ItemLink, OpenNewTabIcon } from 'design-system';
import * as React from 'react';
import { AppStateContext } from '../../app/components/app-state/AppStateContext';

import { getUrlForDoc } from '../../helpers/docs';

interface Props {
  to: string;
  innerRef?: React.Ref<HTMLAnchorElement>;
  children: React.ReactNode;
}

export function DocItemLink({ to, innerRef, children }: Props) {
  const { version } = React.useContext(AppStateContext);

  const toStatic = getUrlForDoc(version, to);

  return (
    <ItemLink innerRef={innerRef} to={toStatic}>
      <OpenNewTabIcon />
      {children}
    </ItemLink>
  );
}
