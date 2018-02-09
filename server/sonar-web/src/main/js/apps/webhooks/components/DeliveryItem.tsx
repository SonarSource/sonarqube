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
import AlertErrorIcon from '../../../components/icons-components/AlertErrorIcon';
import AlertSuccessIcon from '../../../components/icons-components/AlertSuccessIcon';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import BoxedGroupAccordion from '../../../components/controls/BoxedGroupAccordion';
import CodeSnippet from '../../../components/common/CodeSnippet';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { getDelivery } from '../../../api/webhooks';
import { formatMeasure } from '../../../helpers/measures';
import { translateWithParameters, translate } from '../../../helpers/l10n';
import { WebhookDelivery } from '../../../app/types';

interface Props {
  delivery: WebhookDelivery;
}

interface State {
  loading: boolean;
  open: boolean;
  payload?: string;
}

export default class DeliveryItem extends React.PureComponent<Props, State> {
  mounted: boolean = false;
  state: State = { loading: false, open: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchPayload = ({ delivery } = this.props) => {
    this.setState({ loading: true });
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

  handleClick = () => {
    if (!this.state.payload) {
      this.fetchPayload();
    }
    this.setState(({ open }) => ({ open: !open }));
  };

  render() {
    const { delivery } = this.props;
    const { loading, open, payload } = this.state;

    return (
      <BoxedGroupAccordion
        onClick={this.handleClick}
        open={open}
        renderHeader={() =>
          delivery.success ? (
            <AlertSuccessIcon className="pull-right js-success" />
          ) : (
            <AlertErrorIcon className="pull-right js-error" />
          )
        }
        title={<DateTimeFormatter date={delivery.at} />}>
        <div className="big-spacer-left">
          <p className="spacer-bottom">
            {translateWithParameters('webhooks.delivery.response_x', delivery.httpStatus)}
          </p>
          <p className="spacer-bottom">
            {translateWithParameters(
              'webhooks.delivery.duration_x',
              formatMeasure(delivery.durationMs, 'MILLISEC')
            )}
          </p>
          <p className="spacer-bottom">{translate('webhooks.delivery.payload')}</p>
          <DeferredSpinner className="spacer-left spacer-top" loading={loading}>
            {payload && <CodeSnippet noCopy={true} snippet={this.formatPayload(payload)} />}
          </DeferredSpinner>
        </div>
      </BoxedGroupAccordion>
    );
  }
}
