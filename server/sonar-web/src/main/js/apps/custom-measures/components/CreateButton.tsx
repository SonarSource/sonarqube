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
import { translate } from '../../../helpers/l10n';

interface Props {
  onCreate: (data: { description: string; metricKey: string; value: string }) => Promise<void>;
  skipMetrics: string[] | undefined;
}

interface State {
  modal: boolean;
}

export default class CreateButton extends React.PureComponent<Props, State> {
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

  render() {
    return (
      <>
        <button id="custom-measures-create" onClick={this.handleClick} type="button">
          {translate('create')}
        </button>
        {this.state.modal && (
          <Form
            confirmButtonText={translate('create')}
            header={translate('custom_measures.create_custom_measure')}
            onClose={this.handleClose}
            onSubmit={this.props.onCreate}
            skipMetrics={this.props.skipMetrics}
          />
        )}
      </>
    );
  }
}
