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
import OrganizationDetailsForm from './OrganizationDetailsForm';
import OrganizationDetailsStep from './OrganizationDetailsStep';
import PlanStep from './PlanStep';
import { Step } from './utils';
import { DeleteButton } from '../../../components/ui/buttons';
import { getBaseUrl } from '../../../helpers/urls';
import { translate } from '../../../helpers/l10n';
import { sanitizeAlmId } from '../../../helpers/almIntegrations';
import OrganizationAvatar from '../../../components/common/OrganizationAvatar';

interface Props {
  almApplication: T.AlmApplication;
  almInstallId?: string;
  almOrganization: T.AlmOrganization;
  handleCancelImport: () => void;
  handleOrgDetailsFinish: (organization: T.Organization) => Promise<void>;
  handleOrgDetailsStepOpen: () => void;
  importPersonalOrg: T.Organization;
  onDone: () => void;
  organization?: T.Organization;
  step: Step;
  subscriptionPlans?: T.SubscriptionPlan[];
  updateOrganization: (
    organization: T.Organization & { installationId?: string }
  ) => Promise<string>;
}

export default class AutoPersonalOrganizationBind extends React.PureComponent<Props> {
  handleCreateOrganization = () => {
    const { organization } = this.props;
    if (!organization) {
      return Promise.reject();
    }
    return this.props.updateOrganization({
      ...organization,
      installationId: this.props.almInstallId
    });
  };

  handleOrgDetailsFinish = (organization: T.Organization) => {
    return this.props.handleOrgDetailsFinish({
      ...organization,
      key: this.props.importPersonalOrg.key
    });
  };

  render() {
    const { almApplication, importPersonalOrg, organization, step, subscriptionPlans } = this.props;
    return (
      <>
        <OrganizationDetailsStep
          finished={organization !== undefined}
          onOpen={this.props.handleOrgDetailsStepOpen}
          open={step === Step.OrganizationDetails}
          organization={organization}
          stepTitle={translate('onboarding.import_organization.personal.import_org_details')}>
          <div className="display-flex-center big-spacer-bottom">
            <FormattedMessage
              defaultMessage={translate('onboarding.import_personal_organization_x')}
              id="onboarding.import_personal_organization_x"
              values={{
                avatar: (
                  <img
                    alt={almApplication.name}
                    className="little-spacer-left"
                    src={`${getBaseUrl()}/images/sonarcloud/${sanitizeAlmId(
                      almApplication.key
                    )}.svg`}
                    width={16}
                  />
                ),
                name: <strong>{this.props.almOrganization.name}</strong>,
                personalAvatar: importPersonalOrg && (
                  <OrganizationAvatar organization={importPersonalOrg} small={true} />
                ),
                personalName: importPersonalOrg && <strong>{importPersonalOrg.name}</strong>
              }}
            />
            <DeleteButton className="little-spacer-left" onClick={this.props.handleCancelImport} />
          </div>
          <OrganizationDetailsForm
            keyReadOnly={true}
            onContinue={this.handleOrgDetailsFinish}
            organization={importPersonalOrg}
            submitText={translate('continue')}
          />
        </OrganizationDetailsStep>
        {subscriptionPlans !== undefined && (
          <PlanStep
            almApplication={this.props.almApplication}
            almOrganization={this.props.almOrganization}
            createOrganization={this.handleCreateOrganization}
            onDone={this.props.onDone}
            open={step === Step.Plan}
            subscriptionPlans={subscriptionPlans}
          />
        )}
      </>
    );
  }
}
