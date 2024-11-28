/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { addGlobalSuccessMessage, Card, FlagMessage, InputField, Modal } from 'design-system';
import * as React from 'react';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Router } from '~sonar-aligned/types/router';
import { deleteOrganization } from '../../../api/organizations';
import InstanceMessage from '../../../components/common/InstanceMessage';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Organization } from '../../../types/types';
import { withOrganizationContext } from '../OrganizationContext';

interface Props {
  organization: Organization;
  router: Router;
}

interface State {
  hasPaidPlan?: boolean;
  verify: string;
  isOpen?: boolean;
}

export class OrganizationDelete extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { verify: '', isOpen: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleInput = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ verify: event.currentTarget.value });
  };

  isVerified = () => {
    return this.state.verify.toLowerCase() === this.props.organization.name.toLowerCase();
  };

  openDeletePopup = () => {
    this.setState({ isOpen: true });
  };

  closeDeletePopup = () => {
    this.setState({ isOpen: false });
  };

  onDelete = () => {
    const { organization } = this.props;
    return deleteOrganization(organization.kee).then(() => {
      if (this.state.hasPaidPlan) {
        this.props.router.replace({
          pathname: '/feedback/downgrade',
          state: {
            confirmationMessage: translateWithParameters(
              'organization.deleted_x',
              organization.name,
            ),
            organization,
            title: translate('billing.downgrade.reason.title_deleted'),
          },
        });
      } else {
        addGlobalSuccessMessage(translate('organization.deleted'));
        this.props.router.replace('/');
        window.location.reload();
      }
    });
  };

  render() {
    const { hasPaidPlan, isOpen } = this.state;
    return (
      <Card>
        <div className="boxed-group boxed-group-inner">
          <h2 className="boxed-title">{translate('organization.delete')}</h2>
          <p className="big-spacer-bottom width-50 sw-my-8">
            <InstanceMessage message={translate('organization.delete.description')} />
          </p>
          <Button variety={ButtonVariety.Danger} onClick={this.openDeletePopup}>
            {translate('delete')}
          </Button>
          <Modal
            isOpen={isOpen}
            onClose={this.closeDeletePopup}
            body={
              <div>
                {hasPaidPlan && (
                  <FlagMessage variant="warning">
                    {translate('organization.delete.sonarcloud.paid_plan_info')}
                  </FlagMessage>
                )}
                <p>{translate('organization.delete.question')}</p>
                <div className="spacer-top">
                  <label htmlFor="downgrade-organization-name">
                    {translate('billing.downgrade.modal.type_to_proceed')}
                  </label>
                  <div className="little-spacer-top sw-my-4">
                    <InputField
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
            headerTitle={translateWithParameters(
              'organization.delete_x',
              this.props.organization.name,
            )}
            primaryButton={
              <Button
                variety={ButtonVariety.Danger}
                isDisabled={!this.isVerified()}
                onClick={this.onDelete}
              >
                {translate('delete')}
              </Button>
            }
            secondaryButtonLabel={translate('close')}
          ></Modal>
        </div>
      </Card>
    );
  }
}

export default whenLoggedIn(withRouter(withOrganizationContext(OrganizationDelete)));
