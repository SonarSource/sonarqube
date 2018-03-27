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
import * as PropTypes from 'prop-types';
import { Component } from '../types';
import { BranchLike } from '../../../app/types';
import PinIcon from '../../../components/shared/pin-icon';
import { WorkspaceContext } from '../../../components/workspace/context';
import { translate } from '../../../helpers/l10n';

interface Props {
  branchLike?: BranchLike;
  component: Component;
}

export default class ComponentPin extends React.PureComponent<Props> {
  context!: { workspace: WorkspaceContext };

  static contextTypes = {
    workspace: PropTypes.object.isRequired
  };

  handleClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.context.workspace.openComponent({
      branchLike: this.props.branchLike,
      key: this.props.component.key,
      name: this.props.component.path,
      qualifier: this.props.component.qualifier
    });
  };

  render() {
    return (
      <a
        className="link-no-underline"
        href="#"
        onClick={this.handleClick}
        title={translate('component_viewer.open_in_workspace')}>
        <PinIcon />
      </a>
    );
  }
}
