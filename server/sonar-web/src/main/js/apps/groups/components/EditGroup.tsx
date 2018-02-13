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
import Form from './Form';
import { Group } from '../../../app/types';
import { translate } from '../../../helpers/l10n';
import { omitNil } from '../../../helpers/request';

interface Props {
  children: (props: { onClick: () => void }) => React.ReactNode;
  group: Group;
  onEdit: (data: { description?: string; id: number; name?: string }) => Promise<void>;
}

interface State {
  modal: boolean;
}

export default class EditGroup extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { modal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleClick = () => {
    this.setState({ modal: true });
  };

  handleClose = () => {
    if (this.mounted) {
      this.setState({ modal: false });
    }
  };

  handleSubmit = ({ name, description }: { name: string; description: string }) => {
    const { group } = this.props;
    return this.props.onEdit({
      description,
      id: group.id,
      // pass `name` only if it has changed, otherwise the WS fails
      ...omitNil({ name: name !== group.name ? name : undefined })
    });
  };

  render() {
    return (
      <>
        {this.props.children({ onClick: this.handleClick })}
        {this.state.modal && (
          <Form
            confirmButtonText={translate('update_verb')}
            group={this.props.group}
            header={translate('groups.update_group')}
            onClose={this.handleClose}
            onSubmit={this.handleSubmit}
          />
        )}
      </>
    );
  }
}
