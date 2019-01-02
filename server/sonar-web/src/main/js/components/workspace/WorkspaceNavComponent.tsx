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
import { ComponentDescriptor } from './context';
import WorkspaceComponentTitle from './WorkspaceComponentTitle';
import WorkspaceNavItem from './WorkspaceNavItem';

export interface Props {
  component: ComponentDescriptor;
  onClose: (componentKey: string) => void;
  onOpen: (componentKey: string) => void;
}

export default class WorkspaceNavComponent extends React.PureComponent<Props> {
  handleClose = () => {
    this.props.onClose(this.props.component.key);
  };

  handleOpen = () => {
    this.props.onOpen(this.props.component.key);
  };

  render() {
    return (
      <WorkspaceNavItem onClose={this.handleClose} onOpen={this.handleOpen}>
        <WorkspaceComponentTitle component={this.props.component} limited={true} />
      </WorkspaceNavItem>
    );
  }
}
