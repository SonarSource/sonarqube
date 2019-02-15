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
import UpgradeOrganizationAdvantages from './UpgradeOrganizationAdvantages';
import BillingFormShim from './BillingFormShim';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import Modal from '../../../components/controls/Modal';
import { ResetButtonLink } from '../../../components/ui/buttons';
import { getExtensionStart } from '../../../app/components/extensions/utils';
import { translate } from '../../../helpers/l10n';
import { withCurrentUser } from '../../../components/hoc/withCurrentUser';

const BillingForm = withCurrentUser(BillingFormShim);

interface Props {
  insideModal?: boolean;
  onUpgradeDone: () => void;
  onClose: () => void;
  organization: T.Organization;
  subscriptionPlans: T.SubscriptionPlan[];
}

interface State {
  ready: boolean;
}

export default class UpgradeOrganizationModal extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { ready: false };

  componentDidMount() {
    this.mounted = true;
    getExtensionStart('billing/billing').then(
      () => {
        if (this.mounted) {
          this.setState({ ready: true });
        }
      },
      () => {}
    );
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  render() {
    const header = translate('billing.upgrade_box.upgrade_to_paid_plan');

    if (!this.state.ready) {
      return null;
    }

    return (
      <Modal
        contentLabel={header}
        noBackdrop={this.props.insideModal}
        onRequestClose={this.props.onClose}
        shouldCloseOnOverlayClick={false}
        size={'medium'}>
        <div className="modal-head">
          <h2>{header}</h2>
        </div>
        <BillingForm
          onCommit={this.props.onUpgradeDone}
          organizationKey={this.props.organization.key}
          subscriptionPlans={this.props.subscriptionPlans}>
          {({
            onSubmit,
            processingUpgrade,
            renderFormFields,
            renderNextCharge,
            renderRecap,
            renderSubmitButton
          }) => (
            <form id="organization-paid-plan-form" onSubmit={onSubmit}>
              <div className="modal-body modal-container">
                <div className="huge-spacer-bottom">
                  <p className="spacer-bottom">
                    <FormattedMessage
                      defaultMessage={translate('billing.upgrade.org_x_advantages')}
                      id="billing.coupon.description"
                      values={{
                        org: <strong>{this.props.organization.name}</strong>
                      }}
                    />
                  </p>
                  <UpgradeOrganizationAdvantages />
                </div>
                {renderFormFields()}
                <div className="big-spacer-top">{renderRecap()}</div>
              </div>
              <footer className="modal-foot display-flex-center display-flex-space-between">
                {renderNextCharge() || <span />}
                <div>
                  <DeferredSpinner loading={processingUpgrade} />
                  {renderSubmitButton()}
                  <ResetButtonLink onClick={this.props.onClose}>
                    {translate('cancel')}
                  </ResetButtonLink>
                </div>
              </footer>
            </form>
          )}
        </BillingForm>
      </Modal>
    );
  }
}
