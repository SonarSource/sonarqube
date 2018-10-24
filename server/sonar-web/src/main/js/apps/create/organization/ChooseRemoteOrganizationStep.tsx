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
import { WithRouterProps, withRouter } from 'react-router';
import { sortBy } from 'lodash';
import { serializeQuery } from './utils';
import IdentityProviderLink from '../../../components/ui/IdentityProviderLink';
import Select from '../../../components/controls/Select';
import Step from '../../tutorials/components/Step';
import { Alert } from '../../../components/ui/Alert';
import { SubmitButton } from '../../../components/ui/buttons';
import { AlmApplication, AlmUnboundApplication } from '../../../app/types';
import { getBaseUrl } from '../../../helpers/urls';
import { sanitizeAlmId } from '../../../helpers/almIntegrations';
import { translate } from '../../../helpers/l10n';

interface Props {
  almApplication: AlmApplication;
  almInstallId?: string;
  almUnboundApplications: AlmUnboundApplication[];
}

interface State {
  unboundInstallationId: string;
}

export class ChooseRemoteOrganizationStep extends React.PureComponent<
  Props & WithRouterProps,
  State
> {
  state: State = { unboundInstallationId: '' };

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const { unboundInstallationId } = this.state;
    if (unboundInstallationId) {
      this.props.router.push({
        pathname: '/create-organization',
        query: serializeQuery({
          almInstallId: unboundInstallationId,
          almKey: this.props.almApplication.key
        })
      });
    }
  };

  handleInstallationChange = ({ installationId }: AlmUnboundApplication) => {
    this.setState({ unboundInstallationId: installationId });
  };

  renderOption = (organization: AlmUnboundApplication) => {
    const { almApplication } = this.props;
    return (
      <span>
        <img
          alt={almApplication.name}
          className="spacer-right"
          height={14}
          src={`${getBaseUrl()}/images/sonarcloud/${sanitizeAlmId(almApplication.key)}.svg`}
        />
        {organization.name}
      </span>
    );
  };

  renderForm = () => {
    const { almApplication, almInstallId, almUnboundApplications } = this.props;
    const { unboundInstallationId } = this.state;
    return (
      <div className="boxed-group-inner">
        {almInstallId && (
          <Alert className="markdown big-spacer-bottom width-60" variant="error">
            {translate('onboarding.import_organization.org_not_found')}
            <ul>
              <li>{translate('onboarding.import_organization.org_not_found.tips_1')}</li>
              <li>{translate('onboarding.import_organization.org_not_found.tips_2')}</li>
            </ul>
          </Alert>
        )}
        <div className="display-flex-center">
          <div className="display-inline-block abs-width-400">
            <IdentityProviderLink
              className="display-inline-block"
              identityProvider={almApplication}
              small={true}
              url={almApplication.installationUrl}>
              {translate(
                'onboarding.import_organization.choose_organization_button',
                almApplication.key
              )}
            </IdentityProviderLink>
          </div>
          {almUnboundApplications.length > 0 && (
            <div className="display-flex-stretch">
              <div className="vertical-pipe-separator">
                <div className="vertical-separator " />
                <span className="note">{translate('or')}</span>
                <div className="vertical-separator" />
              </div>
              <form className="big-spacer-top big-spacer-bottom" onSubmit={this.handleSubmit}>
                <div className="form-field abs-width-400">
                  <label htmlFor="select-unbound-installation">
                    {translate(
                      'onboarding.import_organization.choose_unbound_installation',
                      almApplication.key
                    )}
                  </label>
                  <Select
                    className="input-super-large"
                    clearable={false}
                    id="select-unbound-installation"
                    labelKey="name"
                    onChange={this.handleInstallationChange}
                    optionRenderer={this.renderOption}
                    options={sortBy(almUnboundApplications, o => o.name.toLowerCase())}
                    placeholder={translate('onboarding.import_organization.choose_organization')}
                    value={unboundInstallationId}
                    valueKey="installationId"
                    valueRenderer={this.renderOption}
                  />
                </div>
                <SubmitButton disabled={!unboundInstallationId}>
                  {translate('continue')}
                </SubmitButton>
              </form>
            </div>
          )}
        </div>
      </div>
    );
  };

  renderResult = () => {
    return null;
  };

  render() {
    return (
      <Step
        finished={false}
        onOpen={() => {}}
        open={true}
        renderForm={this.renderForm}
        renderResult={this.renderResult}
        stepNumber={1}
        stepTitle={translate('onboarding.import_organization.import_org_details')}
      />
    );
  }
}

export default withRouter(ChooseRemoteOrganizationStep);
