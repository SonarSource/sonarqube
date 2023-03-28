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
import { createPermissionTemplate } from '../../../api/permissions';
import { Button } from '../../../components/controls/buttons';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import { translate } from '../../../helpers/l10n';
import Form from './Form';
import { withOrganizationContext } from "../../organizations/OrganizationContext";
import { Organization } from "../../../types/types";

interface Props {
  ready?: boolean;
  refresh: () => Promise<void>;
  router: Router;
  organization: Organization;
}

interface State {
  createModal: boolean;
}

class Header extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { createModal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCreateClick = () => {
    this.setState({ createModal: true });
  };

  handleCreateModalClose = () => {
    if (this.mounted) {
      this.setState({ createModal: false });
    }
  };

  handleCreateModalSubmit = (data: {
    description: string;
    name: string;
    projectKeyPattern: string;
  }) => {
    const organization = this.props.organization && this.props.organization.kee;
    return createPermissionTemplate({ ...data, organization }).then((response) => {
      this.props.refresh().then(() => {
        this.props.router.push({
          pathname: `/organizations/${organization}/permission_templates`,
          query: { id: response.permissionTemplate.id },
        });
      });
    });
  };

  render() {
    return (
      <header className="page-header" id="project-permissions-header">
        <h1 className="page-title">{translate('permission_templates.page')}</h1>

        {!this.props.ready && <i className="spinner" />}

        <div className="page-actions">
          <Button onClick={this.handleCreateClick}>{translate('create')}</Button>

          {this.state.createModal && (
            <Form
              confirmButtonText={translate('create')}
              header={translate('permission_template.new_template')}
              onClose={this.handleCreateModalClose}
              onSubmit={this.handleCreateModalSubmit}
            />
          )}
        </div>

        <p className="page-description">{translate('permission_templates.page.description')}</p>
      </header>
    );
  }
}

export default withRouter(withOrganizationContext(Header));
