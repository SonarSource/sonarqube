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
import { Badge, CellComponent } from 'design-system';
import * as React from 'react';
import { Plugin } from '../../../types/plugins';

interface Props {
  plugin: Plugin;
}

export default function PluginDescription(props: Readonly<Props>) {
  return (
    <CellComponent>
      <strong className="sw-typo-semibold">{props.plugin.name}</strong>
      {props.plugin.category && <Badge className="sw-ml-2">{props.plugin.category}</Badge>}
      {props.plugin.description && <div className="sw-mt-2">{props.plugin.description}</div>}
    </CellComponent>
  );
}
