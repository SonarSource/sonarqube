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
import styled from '@emotion/styled';
import * as React from 'react';
import WorkspaceNavComponent from './WorkspaceNavComponent';
import { ComponentDescriptor } from './context';

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
    <WorkspaceNavStyled>
      <ul className="sw-float-right">
        {components.map((component) => (
          <WorkspaceNavComponent
            component={component}
            key={`component-${component.key}`}
            onClose={props.onComponentClose}
            onOpen={props.onComponentOpen}
          />
        ))}
      </ul>
    </WorkspaceNavStyled>
  );
}

const WorkspaceNavStyled = styled.nav`
  position: fixed;
  z-index: 451;
  bottom: 0;
  right: 0;
  height: 1.75rem;
`;
