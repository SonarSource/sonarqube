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
import CreateWebhookForm from './CreateWebhookForm';
import DeliveriesForm from './DeliveriesForm';
import ActionsDropdown, {
  ActionsDropdownItem,
  ActionsDropdownDivider
} from '../../../components/controls/ActionsDropdown';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Webhook } from '../../../app/types';

interface Props {
  onDelete: (webhook: string) => Promise<void>;
  onUpdate: (data: { webhook: string; name: string; url: string }) => Promise<void>;
  webhook: Webhook;
}

interface State {
  deliveries: boolean;
  updating: boolean;
}

export default class WebhookActions extends React.PureComponent<Props, State> {
  mounted: boolean = false;
  state: State = { deliveries: false, updating: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleDelete = () => {
    return this.props.onDelete(this.props.webhook.key);
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
    // TODO Disable "Show deliveries" if there is no lastDelivery
    return (
      <>
        <ActionsDropdown className="big-spacer-left">
          <ActionsDropdownItem className="js-webhook-update" onClick={this.handleUpdateClick}>
            {translate('update_verb')}
          </ActionsDropdownItem>
          <ActionsDropdownItem
            className="js-webhook-deliveries"
            onClick={this.handleDeliveriesClick}>
            {translate('webhooks.deliveries.show')}
          </ActionsDropdownItem>
          <ActionsDropdownDivider />
          <ConfirmButton
            confirmButtonText={translate('delete')}
            isDestructive={true}
            modalBody={translateWithParameters('webhooks.delete.confirm', webhook.name)}
            modalHeader={translate('webhooks.delete')}
            onConfirm={this.handleDelete}>
            {({ onClick }) => (
              <ActionsDropdownItem
                className="js-webhook-delete"
                destructive={true}
                onClick={onClick}>
                {translate('delete')}
              </ActionsDropdownItem>
            )}
          </ConfirmButton>
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
      </>
    );
  }
}
