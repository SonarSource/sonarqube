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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import RadioCard from 'sonar-ui-common/components/controls/RadioCard';
import { hasMessage, translate } from 'sonar-ui-common/helpers/l10n';
import { getSubscriptionPlans } from '../../../api/billing';
import { formatPrice } from '../organization/utils';
import UpgradeOrganizationAdvantages from './UpgradeOrganizationAdvantages';
import UpgradeOrganizationModal from './UpgradeOrganizationModal';

interface Props {
  className?: string;
  insideModal?: boolean;
  onOrganizationUpgrade: () => void;
  organization: T.Organization;
}

interface State {
  subscriptionPlans: T.SubscriptionPlan[];
  upgradeOrganizationModal: boolean;
}

export default class UpgradeOrganizationBox extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { subscriptionPlans: [], upgradeOrganizationModal: false };

  componentDidMount() {
    this.mounted = true;
    this.fetchSubscriptionPlans();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchSubscriptionPlans = () => {
    return getSubscriptionPlans().then(subscriptionPlans => {
      if (this.mounted) {
        this.setState({ subscriptionPlans });
      }
    });
  };

  handleUpgradeClick = () => {
    this.setState({ upgradeOrganizationModal: true });
  };

  handleUpgradeOrganizationModalClose = () => {
    if (this.mounted) {
      this.setState({ upgradeOrganizationModal: false });
    }
  };

  handleOrganizationUpgrade = () => {
    this.props.onOrganizationUpgrade();
    this.handleUpgradeOrganizationModalClose();
  };

  render() {
    if (!hasMessage('billing.upgrade_box.header')) {
      return null;
    }

    const { subscriptionPlans, upgradeOrganizationModal } = this.state;
    const startingPrice = subscriptionPlans[0] && subscriptionPlans[0].price;

    return (
      <>
        <RadioCard
          className={this.props.className}
          title={translate('billing.upgrade_box.header')}
          titleInfo={
            startingPrice !== undefined && (
              <FormattedMessage
                defaultMessage={translate('billing.price_from_x')}
                id="billing.price_from_x"
                values={{
                  price: <span className="big">{formatPrice(startingPrice)}</span>
                }}
              />
            )
          }>
          <>
            <UpgradeOrganizationAdvantages />
            <div className="big-spacer-left">
              <Button className="js-upgrade-organization" onClick={this.handleUpgradeClick}>
                {translate('billing.paid_plan.upgrade')}
              </Button>
              <Link className="spacer-left" target="_blank" to="/about/pricing">
                {translate('billing.pricing.learn_more')}
              </Link>
            </div>
          </>
        </RadioCard>
        {upgradeOrganizationModal && (
          <UpgradeOrganizationModal
            insideModal={this.props.insideModal}
            onClose={this.handleUpgradeOrganizationModalClose}
            onUpgradeDone={this.handleOrganizationUpgrade}
            organization={this.props.organization}
            subscriptionPlans={subscriptionPlans}
          />
        )}
      </>
    );
  }
}
