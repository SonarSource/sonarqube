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
import DeliveryItem from './DeliveryItem';
import Modal from '../../../components/controls/Modal';
import { ResetButtonLink } from '../../../components/ui/buttons';
import { getDelivery } from '../../../api/webhooks';
import { translateWithParameters, translate } from '../../../helpers/l10n';

interface Props {
  delivery: T.WebhookDelivery;
  onClose: () => void;
  webhook: T.Webhook;
}

interface State {
  loading: boolean;
  payload?: string;
}

export default class LatestDeliveryForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchPayload();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchPayload = ({ delivery } = this.props) => {
    return getDelivery({ deliveryId: delivery.id }).then(
      ({ delivery }) => {
        if (this.mounted) {
          this.setState({ payload: delivery.payload, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  formatPayload = (payload: string) => {
    try {
      return JSON.stringify(JSON.parse(payload), undefined, 2);
    } catch (error) {
      return payload;
    }
  };

  render() {
    const { delivery, webhook } = this.props;
    const { loading, payload } = this.state;
    const header = translateWithParameters('webhooks.latest_delivery_for_x', webhook.name);

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <DeliveryItem
          className="modal-body modal-container"
          delivery={delivery}
          loading={loading}
          payload={payload}
        />
        <footer className="modal-foot">
          <ResetButtonLink className="js-modal-close" onClick={this.props.onClose}>
            {translate('close')}
          </ResetButtonLink>
        </footer>
      </Modal>
    );
  }
}
