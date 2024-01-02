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
import * as React from 'react';
import { ComponentDescriptor } from './context';
import WorkspaceNavComponent from './WorkspaceNavComponent';

export interface Props {
  components: ComponentDescriptor[];
  onComponentClose: (componentKey: string) => void;
  onComponentOpen: (componentKey: string) => void;
  open: { component?: string; rule?: string };
}

export default function WorkspaceNav(props: Props) {
  // do not show a tab for the currently open component/rule
  const components = props.components.filter((x) => x.key !== props.open.component);

  return (
    <nav className="workspace-nav">
      <ul className="workspace-nav-list">
        {components.map((component) => (
          <WorkspaceNavComponent
            component={component}
            key={`component-${component.key}`}
            onClose={props.onComponentClose}
            onOpen={props.onComponentOpen}
          />
        ))}
      </ul>
    </nav>
  );
}
