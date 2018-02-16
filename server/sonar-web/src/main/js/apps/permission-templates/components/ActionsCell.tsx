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
import * as PropTypes from 'prop-types';
import { difference } from 'lodash';
import Form from './Form';
import {
  setDefaultPermissionTemplate,
  deletePermissionTemplate,
  updatePermissionTemplate
} from '../../../api/permissions';
import { PermissionTemplate } from '../../../app/types';
import ActionsDropdown, { ActionsDropdownItem } from '../../../components/controls/ActionsDropdown';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import QualifierIcon from '../../../components/shared/QualifierIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  fromDetails?: boolean;
  organization?: { isDefault?: boolean; key: string };
  permissionTemplate: PermissionTemplate;
  refresh: () => void;
  topQualifiers: string[];
}

interface State {
  updateModal: boolean;
}

export default class ActionsCell extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    router: PropTypes.object
  };

  state: State = { updateModal: false };

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

  handleDelete = (templateId: string) => {
    return deletePermissionTemplate({ templateId }).then(() => {
      const pathname = this.props.organization
        ? `/organizations/${this.props.organization.key}/permission_templates`
        : '/permission_templates';
      this.context.router.replace(pathname);
      this.props.refresh();
    });
  };

  setDefault = (qualifier: string) => () => {
    setDefaultPermissionTemplate(this.props.permissionTemplate.id, qualifier).then(
      this.props.refresh,
      () => {}
    );
  };

  getAvailableQualifiers() {
    const topQualifiers =
      this.props.organization && !this.props.organization.isDefault
        ? ['TRK']
        : this.props.topQualifiers;
    return difference(topQualifiers, this.props.permissionTemplate.defaultFor);
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
        onClick={this.setDefault(qualifier)}>
        {child}
      </ActionsDropdownItem>
    );
  }

  renderIfSingleTopQualifier(availableQualifiers: string[]) {
    return availableQualifiers.map(qualifier =>
      this.renderSetDefaultLink(
        qualifier,
        <span>{translate('permission_templates.set_default')}</span>
      )
    );
  }

  renderIfMultipleTopQualifiers(availableQualifiers: string[]) {
    return availableQualifiers.map(qualifier =>
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

    const pathname = organization
      ? `/organizations/${organization.key}/permission_templates`
      : '/permission_templates';

    return (
      <ActionsDropdown>
        {this.renderSetDefaultsControl()}

        {!this.props.fromDetails && (
          <ActionsDropdownItem to={{ pathname, query: { id: t.id } }}>
            {translate('edit_permissions')}
          </ActionsDropdownItem>
        )}

        <ActionsDropdownItem className="js-update" onClick={this.handleUpdateClick}>
          {translate('update_details')}
        </ActionsDropdownItem>
        {this.state.updateModal && (
          <Form
            confirmButtonText={translate('update_verb')}
            header={translate('permission_template.edit_template')}
            onClose={this.handleCloseUpdateModal}
            onSubmit={this.handleSubmitUpdateModal}
            permissionTemplate={t}
          />
        )}

        {t.defaultFor.length === 0 && (
          <ConfirmButton
            confirmButtonText={translate('delete')}
            confirmData={t.id}
            isDestructive={true}
            modalBody={translateWithParameters(
              'permission_template.do_you_want_to_delete_template_xxx',
              t.name
            )}
            modalHeader={translate('permission_template.delete_confirm_title')}
            onConfirm={this.handleDelete}>
            {({ onClick }) => (
              <ActionsDropdownItem className="js-delete" destructive={true} onClick={onClick}>
                {translate('delete')}
              </ActionsDropdownItem>
            )}
          </ConfirmButton>
        )}
      </ActionsDropdown>
    );
  }
}
