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
import IdentityProviderLink from 'sonar-ui-common/components/controls/IdentityProviderLink';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { save } from 'sonar-ui-common/helpers/storage';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { getAlmAppInfo } from '../../../api/alm-integration';
import { sanitizeAlmId } from '../../../helpers/almIntegrations';
import {
  BIND_ORGANIZATION_KEY,
  BIND_ORGANIZATION_REDIRECT_TO_ORG_TIMESTAMP
} from '../../create/organization/utils';

interface Props {
  currentUser: T.LoggedInUser;
  organization: T.Organization;
}

interface State {
  almApplication?: T.AlmApplication;
}

export default class OrganizationBind extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.fetchAlmApplication();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchAlmApplication = () => {
    return getAlmAppInfo().then(({ application }) => {
      if (this.mounted) {
        this.setState({ almApplication: application });
      }
    });
  };

  handleInstallAppClick = () => {
    save(BIND_ORGANIZATION_KEY, this.props.organization.key);
    save(BIND_ORGANIZATION_REDIRECT_TO_ORG_TIMESTAMP, Date.now().toString());
  };

  render() {
    const { currentUser, organization } = this.props;

    const { almApplication } = this.state;

    const almKey = sanitizeAlmId(currentUser.externalProvider || '');
    const orgAlmKey = organization.alm ? sanitizeAlmId(organization.alm.key) : '';
    return (
      <div className="boxed-group boxed-group-inner">
        <h2 className="boxed-title">
          {translateWithParameters('organization.bind_to_x', translate(almKey))}
        </h2>
        {organization.alm ? (
          <>
            <span>{translate('organization.bound')}</span>
            <a
              className="link-no-underline big-spacer-left"
              href={organization.alm.url}
              rel="noopener noreferrer"
              target="_blank">
              <img
                alt={translate(orgAlmKey)}
                className="text-text-top little-spacer-right"
                height={16}
                src={`${getBaseUrl()}/images/sonarcloud/${orgAlmKey}.svg`}
                width={16}
              />
              {translateWithParameters('organization.see_on_x', translate(orgAlmKey))}
            </a>
          </>
        ) : (
          <>
            <p className="spacer-bottom">
              {translateWithParameters('organization.binding_with_x_easy_sync', translate(almKey))}
            </p>
            <p className="big-spacer-bottom">
              {translateWithParameters(
                'organization.app_will_be_installed_on_x',
                translate(almKey)
              )}
            </p>
            {almApplication && (
              <IdentityProviderLink
                backgroundColor={almApplication.backgroundColor}
                className="display-inline-block"
                iconPath={almApplication.iconPath}
                name={almApplication.name}
                onClick={this.handleInstallAppClick}
                small={true}
                url={almApplication.installationUrl}>
                {translate(
                  'onboarding.import_organization.choose_the_organization_button',
                  almApplication.key
                )}
              </IdentityProviderLink>
            )}
          </>
        )}
      </div>
    );
  }
}
