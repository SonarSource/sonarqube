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
import * as classNames from 'classnames';
import { WithRouterProps, withRouter } from 'react-router';
import { FormattedMessage } from 'react-intl';
import { sortBy } from 'lodash';
import { serializeQuery, ORGANIZATION_IMPORT_BINDING_IN_PROGRESS_TIMESTAMP } from './utils';
import IdentityProviderLink from '../../../components/ui/IdentityProviderLink';
import OrganizationAvatar from '../../../components/common/OrganizationAvatar';
import Select from '../../../components/controls/Select';
import { Alert } from '../../../components/ui/Alert';
import { SubmitButton } from '../../../components/ui/buttons';
import { sanitizeAlmId } from '../../../helpers/almIntegrations';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { save } from '../../../helpers/storage';
import { getBaseUrl } from '../../../helpers/urls';

interface Props {
  almApplication: T.AlmApplication;
  almInstallId?: string;
  almOrganization?: T.AlmOrganization;
  almUnboundApplications: T.AlmUnboundApplication[];
  boundOrganization?: T.OrganizationBase;
  className?: string;
}

interface State {
  unboundInstallationId: string;
}

export class RemoteOrganizationChoose extends React.PureComponent<Props & WithRouterProps, State> {
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

  handleInstallAppClick = () => {
    save(ORGANIZATION_IMPORT_BINDING_IN_PROGRESS_TIMESTAMP, Date.now().toString(10));
  };

  handleInstallationChange = ({ installationId }: T.AlmUnboundApplication) => {
    this.setState({ unboundInstallationId: installationId });
  };

  renderOption = (organization: T.AlmUnboundApplication) => {
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

  render() {
    const {
      almApplication,
      almInstallId,
      almOrganization,
      almUnboundApplications,
      boundOrganization,
      className
    } = this.props;
    const { unboundInstallationId } = this.state;
    return (
      <div className={classNames('boxed-group', className)}>
        <div className="boxed-group-header">
          <h2>{translate('onboarding.import_organization.import_org_details')}</h2>
        </div>
        <div className="boxed-group-inner">
          {almInstallId && !almOrganization && (
            <Alert className="big-spacer-bottom width-60" variant="error">
              <div className="markdown">
                {translate('onboarding.import_organization.org_not_found')}
                <ul>
                  <li>{translate('onboarding.import_organization.org_not_found.tips_1')}</li>
                  <li>{translate('onboarding.import_organization.org_not_found.tips_2')}</li>
                </ul>
              </div>
            </Alert>
          )}
          {almOrganization && boundOrganization && (
            <Alert className="big-spacer-bottom width-60" variant="error">
              <FormattedMessage
                defaultMessage={translate('onboarding.import_organization.already_bound_x')}
                id="onboarding.import_organization.already_bound_x"
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
                  name: <strong>{almOrganization.name}</strong>,
                  boundAvatar: (
                    <OrganizationAvatar
                      className="little-spacer-left"
                      organization={boundOrganization}
                      small={true}
                    />
                  ),
                  boundName: <strong>{boundOrganization.name}</strong>
                }}
              />
            </Alert>
          )}
          <div className="display-flex-center">
            <div className="display-inline-block">
              <IdentityProviderLink
                className="display-inline-block"
                identityProvider={almApplication}
                onClick={this.handleInstallAppClick}
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
                    <label className="text-normal" htmlFor="select-unbound-installation">
                      {translateWithParameters(
                        'onboarding.import_organization.choose_unbound_installation_x',
                        translate(sanitizeAlmId(almApplication.key))
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
      </div>
    );
  }
}

export default withRouter(RemoteOrganizationChoose);
