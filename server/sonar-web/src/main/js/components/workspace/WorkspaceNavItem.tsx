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
import { CloseIcon, InteractiveIcon } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';

export interface Props {
  children: React.ReactNode;
  onClose: () => void;
  onOpen: () => void;
}

export default class WorkspaceNavItem extends React.PureComponent<Props> {
  handleNameClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onOpen();
  };

  render() {
    return (
      <StyledWorkspaceNavItem className="workspace-nav-item">
        <a className="workspace-nav-item-link" href="#" onClick={this.handleNameClick}>
          {this.props.children}
        </a>
        <InteractiveIcon
          aria-label={translate('close')}
          className="js-close sw-ml-1 sw-absolute sw-right-1 sw-top-1"
          onClick={this.props.onClose}
          currentColor
          Icon={CloseIcon}
          size="small"
        />
      </StyledWorkspaceNavItem>
    );
  }
}

const StyledWorkspaceNavItem = styled.li`
  color: white;
`;
