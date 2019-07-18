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
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import CreateWebhookForm from './CreateWebhookForm';

interface Props {
  loading: boolean;
  onCreate: (data: { name: string; secret?: string; url: string }) => Promise<void>;
  webhooksCount: number;
}

interface State {
  openCreate: boolean;
}

const WEBHOOKS_LIMIT = 10;

export default class PageActions extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { openCreate: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCreateClose = () => {
    if (this.mounted) {
      this.setState({ openCreate: false });
    }
  };

  handleCreateOpen = () => {
    this.setState({ openCreate: true });
  };

  renderCreate = () => {
    if (this.props.webhooksCount >= WEBHOOKS_LIMIT) {
      return (
        <Tooltip overlay={translateWithParameters('webhooks.maximum_reached', WEBHOOKS_LIMIT)}>
          <Button className="js-webhook-create disabled">{translate('create')}</Button>
        </Tooltip>
      );
    }

    return (
      <>
        <Button className="js-webhook-create" onClick={this.handleCreateOpen}>
          {translate('create')}
        </Button>
        {this.state.openCreate && (
          <CreateWebhookForm onClose={this.handleCreateClose} onDone={this.props.onCreate} />
        )}
      </>
    );
  };

  render() {
    if (this.props.loading) {
      return null;
    }

    return <div className="page-actions">{this.renderCreate()}</div>;
  }
}
