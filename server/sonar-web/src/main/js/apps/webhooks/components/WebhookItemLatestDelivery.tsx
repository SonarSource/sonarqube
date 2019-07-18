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
import { ButtonIcon } from 'sonar-ui-common/components/controls/buttons';
import AlertErrorIcon from 'sonar-ui-common/components/icons/AlertErrorIcon';
import AlertSuccessIcon from 'sonar-ui-common/components/icons/AlertSuccessIcon';
import BulletListIcon from 'sonar-ui-common/components/icons/BulletListIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import LatestDeliveryForm from './LatestDeliveryForm';

interface Props {
  webhook: T.Webhook;
}

interface State {
  modal: boolean;
}

export default class WebhookItemLatestDelivery extends React.PureComponent<Props, State> {
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

  handleModalClose = () => {
    if (this.mounted) {
      this.setState({ modal: false });
    }
  };

  render() {
    const { webhook } = this.props;
    if (!webhook.latestDelivery) {
      return <span>{translate('webhooks.last_execution.none')}</span>;
    }

    const { modal } = this.state;
    return (
      <>
        {webhook.latestDelivery.success ? (
          <AlertSuccessIcon className="text-text-top" />
        ) : (
          <AlertErrorIcon className="text-text-top" />
        )}
        <span className="spacer-left display-inline-flex-center">
          <DateTimeFormatter date={webhook.latestDelivery.at} />
          <ButtonIcon className="button-small little-spacer-left" onClick={this.handleClick}>
            <BulletListIcon />
          </ButtonIcon>
        </span>

        {modal && (
          <LatestDeliveryForm
            delivery={webhook.latestDelivery}
            onClose={this.handleModalClose}
            webhook={webhook}
          />
        )}
      </>
    );
  }
}
