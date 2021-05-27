/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import OrganizationDetailsForm from './OrganizationDetailsForm';
import { Step } from './utils';
import { translate } from 'sonar-ui-common/helpers/l10n';

interface Props {
  createOrganization: (organization: T.Organization) => Promise<string>;
  className?: string;
  onUpgradeFail: () => void;
  handleOrgDetailsFinish: (organization: T.Organization) => Promise<void>;
  handleOrgDetailsStepOpen: () => void;
  onDone: () => void;
  organization?: T.Organization;
  step: Step;
  subscriptionPlans?: T.SubscriptionPlan[];
}

export default class ManualOrganizationCreate extends React.PureComponent<Props> {
  handleCreateOrganization = (organization: T.Organization) => {
    const { handleOrgDetailsFinish, createOrganization, onDone } = this.props;
    return handleOrgDetailsFinish(organization).then(() => {
      createOrganization(organization).then(() => {
        onDone();
      });
    });
  };

  render() {
    const { className, organization } = this.props;
    return (
      <div className={className}>
        <OrganizationDetailsForm
          onContinue={this.handleCreateOrganization}
          organization={organization}
          submitText={translate('my_account.create_organization')}
        />
      </div>
    );
  }
}
