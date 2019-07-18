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
import ActionsDropdown, {
  ActionsDropdownDivider,
  ActionsDropdownItem
} from 'sonar-ui-common/components/controls/ActionsDropdown';
import { translate } from 'sonar-ui-common/helpers/l10n';
import CreateWebhookForm from './CreateWebhookForm';
import DeleteWebhookForm from './DeleteWebhookForm';
import DeliveriesForm from './DeliveriesForm';

interface Props {
  onDelete: (webhook: string) => Promise<void>;
  onUpdate: (data: { webhook: string; name: string; url: string }) => Promise<void>;
  webhook: T.Webhook;
}

interface State {
  deleting: boolean;
  deliveries: boolean;
  updating: boolean;
}

export default class WebhookActions extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { deleting: false, deliveries: false, updating: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleDelete = () => {
    return this.props.onDelete(this.props.webhook.key);
  };

  handleDeleteClick = () => {
    this.setState({ deleting: true });
  };

  handleDeletingStop = () => {
    if (this.mounted) {
      this.setState({ deleting: false });
    }
  };

  handleDeliveriesClick = () => {
    this.setState({ deliveries: true });
  };

  handleDeliveriesStop = () => {
    this.setState({ deliveries: false });
  };

  handleUpdate = (data: { name: string; url: string }) => {
    return this.props.onUpdate({ ...data, webhook: this.props.webhook.key });
  };

  handleUpdateClick = () => {
    this.setState({ updating: true });
  };

  handleUpdatingStop = () => {
    this.setState({ updating: false });
  };

  render() {
    const { webhook } = this.props;
    return (
      <>
        <ActionsDropdown className="big-spacer-left">
          <ActionsDropdownItem className="js-webhook-update" onClick={this.handleUpdateClick}>
            {translate('update_verb')}
          </ActionsDropdownItem>
          {webhook.latestDelivery && (
            <ActionsDropdownItem
              className="js-webhook-deliveries"
              onClick={this.handleDeliveriesClick}>
              {translate('webhooks.deliveries.show')}
            </ActionsDropdownItem>
          )}
          <ActionsDropdownDivider />
          <ActionsDropdownItem
            className="js-webhook-delete"
            destructive={true}
            onClick={this.handleDeleteClick}>
            {translate('delete')}
          </ActionsDropdownItem>
        </ActionsDropdown>

        {this.state.deliveries && (
          <DeliveriesForm onClose={this.handleDeliveriesStop} webhook={webhook} />
        )}

        {this.state.updating && (
          <CreateWebhookForm
            onClose={this.handleUpdatingStop}
            onDone={this.handleUpdate}
            webhook={webhook}
          />
        )}

        {this.state.deleting && (
          <DeleteWebhookForm
            onClose={this.handleDeletingStop}
            onSubmit={this.handleDelete}
            webhook={webhook}
          />
        )}
      </>
    );
  }
}
