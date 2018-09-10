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
import { Helmet } from 'react-helmet';
import { FormattedMessage } from 'react-intl';
import { Link, withRouter, WithRouterProps } from 'react-router';
import { connect } from 'react-redux';
import OrganizationDetailsStep from './OrganizationDetailsStep';
import { whenLoggedIn } from './whenLoggedIn';
import { translate } from '../../../helpers/l10n';
import { OrganizationBase, Organization } from '../../../app/types';
import { createOrganization } from '../../organizations/actions';
import { getOrganizationUrl } from '../../../helpers/urls';
import '../../../app/styles/sonarcloud.css';
import '../../tutorials/styles.css'; // TODO remove me

interface Props {
  createOrganization: (organization: OrganizationBase) => Promise<Organization>;
}

export class CreateOrganization extends React.PureComponent<Props & WithRouterProps> {
  mounted = false;

  componentDidMount() {
    this.mounted = true;
    document.body.classList.add('white-page');
    document.documentElement.classList.add('white-page');
  }

  componentWillUnmount() {
    this.mounted = false;
    document.body.classList.remove('white-page');
  }

  handleOrganizationCreate = (organization: Required<OrganizationBase>) => {
    return this.props
      .createOrganization({
        avatar: organization.avatar,
        description: organization.description,
        key: organization.key,
        name: organization.name || organization.key,
        url: organization.url
      })
      .then(organization => {
        this.props.router.push(getOrganizationUrl(organization.key));
      });
  };

  render() {
    const header = translate('onboarding.create_organization.page.header');

    return (
      <>
        <Helmet title={header} titleTemplate="%s" />
        <div className="sonarcloud page page-limited">
          <header className="page-header">
            <h1 className="page-title big-spacer-bottom">{header}</h1>
            <div className="page-actions">
              <Link to="/">{translate('cancel')}</Link>
            </div>
            <p className="page-description">
              <FormattedMessage
                defaultMessage={translate('onboarding.create_organization.page.description')}
                id="onboarding.create_organization.page.description"
                values={{
                  break: <br />,
                  price: '€10', // TODO
                  more: (
                    <Link to="/documentation/sonarcloud-pricing">{translate('learn_more')}</Link>
                  )
                }}
              />
            </p>
          </header>

          <OrganizationDetailsStep onContinue={this.handleOrganizationCreate} />
        </div>
      </>
    );
  }
}

const mapDispatchToProps = { createOrganization: createOrganization as any };

export default whenLoggedIn(
  connect(
    null,
    mapDispatchToProps
  )(withRouter(CreateOrganization))
);
