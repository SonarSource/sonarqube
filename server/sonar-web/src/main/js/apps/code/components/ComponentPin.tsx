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
import * as React from 'react';
import Workspace from '../../../components/workspace/main';
import PinIcon from '../../../components/shared/pin-icon';
import { translate } from '../../../helpers/l10n';
import { Component } from '../types';

interface Props {
  branch?: string;
  component: Component;
}

export default function ComponentPin({ branch, component }: Props) {
  const handleClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    Workspace.openComponent({ branch, key: component.key });
  };

  return (
    <a
      className="link-no-underline"
      onClick={handleClick}
      title={translate('component_viewer.open_in_workspace')}
      href="#">
      <PinIcon />
    </a>
  );
}
