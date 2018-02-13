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
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { translate } from '../../../helpers/l10n';

interface Props {
  loading: boolean;
  onCreate: (data: { description: string; name: string }) => Promise<void>;
}

interface State {
  createModal: boolean;
}

export default class Header extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { createModal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCreateClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ createModal: true });
  };

  handleClose = () => {
    if (this.mounted) {
      this.setState({ createModal: false });
    }
  };

  handleSubmit = (data: { name: string; description: string }) => {
    return this.props.onCreate(data);
  };

  render() {
    return (
      <>
        <header className="page-header" id="groups-header">
          <h1 className="page-title">{translate('user_groups.page')}</h1>

          <DeferredSpinner loading={this.props.loading} />

          <div className="page-actions">
            <button id="groups-create" onClick={this.handleCreateClick}>
              {translate('groups.create_group')}
            </button>
          </div>

          <p className="page-description">{translate('user_groups.page.description')}</p>
        </header>
        {this.state.createModal && (
          <Form
            confirmButtonText={translate('create')}
            header={translate('groups.create_group')}
            onClose={this.handleClose}
            onSubmit={this.handleSubmit}
          />
        )}
      </>
    );
  }
}
