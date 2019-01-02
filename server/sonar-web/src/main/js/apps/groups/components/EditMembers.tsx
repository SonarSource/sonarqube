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
import EditMembersModal from './EditMembersModal';
import BulletListIcon from '../../../components/icons-components/BulletListIcon';
import { ButtonIcon } from '../../../components/ui/buttons';

interface Props {
  group: T.Group;
  onEdit: () => void;
  organization: string | undefined;
}

interface State {
  modal: boolean;
}

export default class EditMembers extends React.PureComponent<Props, State> {
  container?: HTMLElement | null;
  mounted = false;
  state: State = { modal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleMembersClick = () => {
    this.setState({ modal: true });
  };

  handleModalClose = () => {
    if (this.mounted) {
      this.setState({ modal: false });
      this.props.onEdit();
    }
  };

  render() {
    return (
      <>
        <ButtonIcon className="button-small" onClick={this.handleMembersClick}>
          <BulletListIcon />
        </ButtonIcon>
        {this.state.modal && (
          <EditMembersModal
            group={this.props.group}
            onClose={this.handleModalClose}
            organization={this.props.organization}
          />
        )}
      </>
    );
  }
}
