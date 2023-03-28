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
import { difference } from 'lodash';
import * as React from 'react';
import {
  deletePermissionTemplate,
  setDefaultPermissionTemplate,
  updatePermissionTemplate,
} from '../../../api/permissions';
import ActionsDropdown, { ActionsDropdownItem } from '../../../components/controls/ActionsDropdown';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { translate } from '../../../helpers/l10n';
import { queryToSearch } from '../../../helpers/urls';
import { Organization, PermissionTemplate } from '../../../types/types';
import DeleteForm from './DeleteForm';
import Form from './Form';
import { withOrganizationContext } from "../../organizations/OrganizationContext";

interface Props {
  fromDetails?: boolean;
  permissionTemplate: PermissionTemplate;
  refresh: () => void;
  router: Router;
  topQualifiers: string[];
  organization: Organization;
}

interface State {
  deleteForm: boolean;
  updateModal: boolean;
}

export class ActionsCell extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { deleteForm: false, updateModal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleUpdateClick = () => {
    this.setState({ updateModal: true });
  };

  handleCloseUpdateModal = () => {
    if (this.mounted) {
      this.setState({ updateModal: false });
    }
  };

  handleSubmitUpdateModal = (data: {
    description: string;
    name: string;
    projectKeyPattern: string;
  }) => {
    return updatePermissionTemplate({ id: this.props.permissionTemplate.id, ...data }).then(
      this.props.refresh
    );
  };

  handleDeleteClick = () => {
    this.setState({ deleteForm: true });
  };

  handleCloseDeleteForm = () => {
    if (this.mounted) {
      this.setState({ deleteForm: false });
    }
  };

  handleDeleteSubmit = () => {
    return deletePermissionTemplate({ templateId: this.props.permissionTemplate.id }).then(() => {
      this.props.router.replace(`/organizations/${this.props.organization.kee}/permission_templates`);
      this.props.refresh();
    });
  };

  setDefault = (qualifier: string) => () => {
    setDefaultPermissionTemplate(this.props.permissionTemplate.id, qualifier, this.props.organization.kee).then(
      this.props.refresh,
      () => {}
    );
  };

  getAvailableQualifiers() {
    return difference(this.props.topQualifiers, this.props.permissionTemplate.defaultFor);
  }

  renderSetDefaultsControl() {
    const availableQualifiers = this.getAvailableQualifiers();

    if (availableQualifiers.length === 0) {
      return null;
    }

    return this.props.topQualifiers.length === 1
      ? this.renderIfSingleTopQualifier(availableQualifiers)
      : this.renderIfMultipleTopQualifiers(availableQualifiers);
  }

  renderSetDefaultLink(qualifier: string, child: React.ReactNode) {
    return (
      <ActionsDropdownItem
        className="js-set-default"
        data-qualifier={qualifier}
        key={qualifier}
        onClick={this.setDefault(qualifier)}
      >
        {child}
      </ActionsDropdownItem>
    );
  }

  renderIfSingleTopQualifier(availableQualifiers: string[]) {
    return availableQualifiers.map((qualifier) =>
      this.renderSetDefaultLink(
        qualifier,
        <span>{translate('permission_templates.set_default')}</span>
      )
    );
  }

  renderIfMultipleTopQualifiers(availableQualifiers: string[]) {
    return availableQualifiers.map((qualifier) =>
      this.renderSetDefaultLink(
        qualifier,
        <span>
          {translate('permission_templates.set_default_for')}{' '}
          <QualifierIcon qualifier={qualifier} /> {translate('qualifiers', qualifier)}
        </span>
      )
    );
  }

  render() {
    const { permissionTemplate: t, organization } = this.props;

    return (
      <>
        <ActionsDropdown>
          {this.renderSetDefaultsControl()}

          {!this.props.fromDetails && (
            <ActionsDropdownItem
              to={{ pathname: `/organizations/${organization.kee}/permission_templates`, search: queryToSearch({ id: t.id }) }}
            >
              {translate('edit_permissions')}
            </ActionsDropdownItem>
          )}

          <ActionsDropdownItem className="js-update" onClick={this.handleUpdateClick}>
            {translate('update_details')}
          </ActionsDropdownItem>

          {t.defaultFor.length === 0 && (
            <ActionsDropdownItem
              className="js-delete"
              destructive={true}
              onClick={this.handleDeleteClick}
            >
              {translate('delete')}
            </ActionsDropdownItem>
          )}
        </ActionsDropdown>

        {this.state.updateModal && (
          <Form
            confirmButtonText={translate('update_verb')}
            header={translate('permission_template.edit_template')}
            onClose={this.handleCloseUpdateModal}
            onSubmit={this.handleSubmitUpdateModal}
            permissionTemplate={t}
          />
        )}

        {this.state.deleteForm && (
          <DeleteForm
            onClose={this.handleCloseDeleteForm}
            onSubmit={this.handleDeleteSubmit}
            permissionTemplate={t}
          />
        )}
      </>
    );
  }
}

export default withRouter(withOrganizationContext(ActionsCell));
