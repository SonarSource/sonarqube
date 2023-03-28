/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Helmet } from 'react-helmet-async';
import ManualOrganizationCreate from './ManualOrganizationCreate';
import { translate } from "../../../helpers/l10n";
import { Organization } from "../../../types/types";
import { Router, withRouter } from "../../../components/hoc/withRouter";
import { getOrganizationUrl } from "../../../helpers/urls";
import { getUserOrganizations } from "../../../api/organizations";
import withCurrentUserContext from "../../../app/components/current-user/withCurrentUserContext";
import { CurrentUserContextInterface } from "../../../app/components/current-user/CurrentUserContext";

interface Props {
  router: Router;
}

export class CreateOrganizationPage extends React.PureComponent<Props & CurrentUserContextInterface> {

  handleOrgCreated = (organization: Organization) => {
    getUserOrganizations().then((organizations) => {
      this.props.updateUserOrganizations(organizations);
    }).catch(() => {
      /* noop */
    });
    this.props.router.push(getOrganizationUrl(organization.kee));
  };

  renderContent = () => {
    return (
        <ManualOrganizationCreate
            onOrganizationCreate={this.handleOrgCreated}
        />
    );
  };

  render() {
    const header = translate('onboarding.create_organization.page.header');

    return (
        <>
          <Helmet title={header} titleTemplate="%s"/>
          <div className="page page-limited huge-spacer-top huge-spacer-bottom">
            <header className="page-header huge-spacer-bottom">
              <h1 className="page-title huge big-spacer-bottom">
                <strong>{header}</strong>
              </h1>
            </header>
            {this.renderContent()}
          </div>
        </>
    );
  }
}

export default withCurrentUserContext(withRouter(CreateOrganizationPage));
