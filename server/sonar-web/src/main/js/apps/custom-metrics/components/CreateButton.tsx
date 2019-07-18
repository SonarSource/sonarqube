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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
import Form, { MetricProps } from './Form';

interface Props {
  domains: string[];
  onCreate: (data: MetricProps) => Promise<void>;
  types: string[];
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
        <Button id="metrics-create" onClick={this.handleClick}>
          {translate('custom_metrics.create_metric')}
        </Button>
        {this.state.modal && (
          <Form
            confirmButtonText={translate('create')}
            domains={this.props.domains}
            header={translate('custom_metrics.create_metric')}
            onClose={this.handleClose}
            onSubmit={this.props.onCreate}
            types={this.props.types}
          />
        )}
      </>
    );
  }
}
