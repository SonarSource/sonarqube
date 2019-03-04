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
import { connect } from 'react-redux';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import InstanceMessage from '../../../components/common/InstanceMessage';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { deleteOrganization } from '../actions';
import { Button } from '../../../components/ui/buttons';
import { getOrganizationBilling } from '../../../api/organizations';
import { isSonarCloud } from '../../../helpers/system';
import { Alert } from '../../../components/ui/Alert';
import { withRouter, Router } from '../../../components/hoc/withRouter';
import addGlobalSuccessMessage from '../../../app/utils/addGlobalSuccessMessage';

interface DispatchToProps {
  deleteOrganization: (key: string) => Promise<void>;
}

interface OwnProps {
  organization: Pick<T.Organization, 'key' | 'name'>;
  router: Pick<Router, 'replace'>;
}

type Props = OwnProps & DispatchToProps;

interface State {
  hasPaidPlan?: boolean;
  verify: string;
}

export class OrganizationDelete extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { verify: '' };

  componentDidMount() {
    this.mounted = true;
    this.fetchOrganizationPlanInfo();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchOrganizationPlanInfo = () => {
    if (isSonarCloud()) {
      getOrganizationBilling(this.props.organization.key).then(
        billingInfo => {
          if (this.mounted) {
            this.setState({
              hasPaidPlan: billingInfo.subscription.status !== 'inactive'
            });
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ hasPaidPlan: false });
          }
        }
      );
    }
  };

  handleInput = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ verify: event.currentTarget.value });
  };

  isVerified = () => {
    return this.state.verify.toLowerCase() === this.props.organization.name.toLowerCase();
  };

  onDelete = () => {
    const { organization } = this.props;
    return this.props.deleteOrganization(organization.key).then(() => {
      if (this.state.hasPaidPlan) {
        this.props.router.replace({
          pathname: '/feedback/downgrade',
          state: {
            confirmationMessage: translateWithParameters(
              'organization.deleted_x',
              organization.name
            ),
            organization,
            title: translate('billing.downgrade.reason.title_deleted')
          }
        });
      } else {
        addGlobalSuccessMessage(translate('organization.deleted'));
        this.props.router.replace('/');
      }
    });
  };

  render() {
    const { hasPaidPlan } = this.state;
    return (
      <div className="boxed-group boxed-group-inner">
        <h2 className="boxed-title">{translate('organization.delete')}</h2>
        <p className="big-spacer-bottom width-50">
          <InstanceMessage message={translate('organization.delete.description')} />
        </p>
        <ConfirmButton
          confirmButtonText={translate('delete')}
          confirmDisable={!this.isVerified()}
          isDestructive={true}
          modalBody={
            <div>
              {hasPaidPlan && (
                <Alert variant="warning">
                  {translate('organization.delete.sonarcloud.paid_plan_info')}
                </Alert>
              )}
              <p>{translate('organization.delete.question')}</p>
              <div className="spacer-top">
                <label htmlFor="downgrade-organization-name">
                  {translate('billing.downgrade.modal.type_to_proceed')}
                </label>
                <div className="little-spacer-top">
                  <input
                    autoFocus={true}
                    className="input-super-large"
                    id="downgrade-organization-name"
                    onChange={this.handleInput}
                    type="text"
                    value={this.state.verify}
                  />
                </div>
              </div>
            </div>
          }
          modalHeader={translateWithParameters(
            'organization.delete_x',
            this.props.organization.name
          )}
          onConfirm={this.onDelete}>
          {({ onClick }) => (
            <Button className="js-custom-measure-delete button-red" onClick={onClick}>
              {translate('delete')}
            </Button>
          )}
        </ConfirmButton>
      </div>
    );
  }
}

const mapDispatchToProps: DispatchToProps = { deleteOrganization: deleteOrganization as any };

export default withRouter(
  connect(
    null,
    mapDispatchToProps
  )(OrganizationDelete)
);
