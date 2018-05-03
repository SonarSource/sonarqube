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
import * as PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import InstanceMessage from '../../../components/common/InstanceMessage';
import { translate } from '../../../helpers/l10n';
import { deleteOrganization } from '../actions';
import { Organization } from '../../../app/types';
import { Button } from '../../../components/ui/buttons';
import { getOrganizationBilling } from '../../../api/organizations';

interface DispatchToProps {
  deleteOrganization: (key: string) => Promise<void>;
}

interface OwnProps {
  organization: Pick<Organization, 'key' | 'name'>;
}

type Props = OwnProps & DispatchToProps;

interface State {
  hasPaidPlan?: boolean;
}

export class OrganizationDelete extends React.PureComponent<Props, State> {
  mounted = false;
  static contextTypes = {
    router: PropTypes.object,
    onSonarCloud: PropTypes.bool
  };

  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.fetchOrganizationPlanInfo();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchOrganizationPlanInfo = () => {
    if (this.context.onSonarCloud) {
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

  onDelete = () => {
    return this.props.deleteOrganization(this.props.organization.key).then(() => {
      this.context.router.replace('/');
    });
  };

  render() {
    const { hasPaidPlan } = this.state;
    const title = translate('organization.delete');
    return (
      <>
        <Helmet title={title} />
        <div className="page page-limited">
          <header className="page-header">
            <h1 className="page-title">{title}</h1>
            <div className="page-description">
              <InstanceMessage message={translate('organization.delete.description')} />
            </div>
          </header>
          <ConfirmButton
            confirmButtonText={translate('delete')}
            isDestructive={true}
            modalBody={
              <div>
                {translate('organization.delete.question')}
                {hasPaidPlan && (
                  <p className="alert alert-warn big-spacer-top">
                    {translate('organization.delete.sonarcloud.paid_plan_info')}
                  </p>
                )}
              </div>
            }
            modalHeader={translate('organization.delete')}
            onConfirm={this.onDelete}>
            {({ onClick }) => (
              <Button className="js-custom-measure-delete button-red" onClick={onClick}>
                {translate('delete')}
              </Button>
            )}
          </ConfirmButton>
        </div>
      </>
    );
  }
}

const mapDispatchToProps: DispatchToProps = { deleteOrganization: deleteOrganization as any };

export default connect<null, DispatchToProps, OwnProps>(null, mapDispatchToProps)(
  OrganizationDelete
);
