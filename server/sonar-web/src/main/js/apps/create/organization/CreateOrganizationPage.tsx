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
import { Card } from '~design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Router } from '~sonar-aligned/types/router';
import { getUserOrganizations } from '../../../api/organizations';
import { CurrentUserContextInterface } from '../../../app/components/current-user/CurrentUserContext';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { translate } from '../../../helpers/l10n';
import { getOrganizationUrl } from '../../../helpers/urls';
import { Organization } from '../../../types/types';
import ManualOrganizationCreate from './ManualOrganizationCreate';

interface Props {
  router: Router;
}

export class CreateOrganizationPage extends React.PureComponent<
  Props & CurrentUserContextInterface
> {
  handleOrgCreated = (organization: Organization) => {
    getUserOrganizations()
      .then((organizations) => {
        this.props.updateUserOrganizations(organizations);
      })
      .catch(() => {
        /* noop */
      });
    this.props.router.push(getOrganizationUrl(organization.kee));
  };

  renderContent = () => {
    return <ManualOrganizationCreate onOrganizationCreate={this.handleOrgCreated} />;
  };

  render() {
    const header = translate('onboarding.create_organization.page.header');

    return (
      <>
        <Card className="sw-mt-16 sw-ml-16 sw-mb-8 sw-mr-8">
          <Helmet title={header} titleTemplate="%s" />

          <div className="page page-limited huge-spacer-top huge-spacer-bottom ">
            <header className="page-header huge-spacer-bottom">
              <h2 className="page-title huge big-spacer-bottom">
                <strong>{header}</strong>
              </h2>
            </header>
            {this.renderContent()}
          </div>
        </Card>
      </>
    );
  }
}

export default withCurrentUserContext(withRouter(CreateOrganizationPage));
