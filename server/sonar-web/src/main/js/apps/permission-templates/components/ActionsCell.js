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
import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router';
import { difference } from 'lodash';
import Backbone from 'backbone';
import { PermissionTemplateType, CallbackType } from '../propTypes';
import ActionsDropdown, { ActionsDropdownItem } from '../../../components/controls/ActionsDropdown';
import QualifierIcon from '../../../components/shared/QualifierIcon';
import UpdateView from '../views/UpdateView';
import DeleteView from '../views/DeleteView';
import { translate } from '../../../helpers/l10n';
import { setDefaultPermissionTemplate } from '../../../api/permissions';

export default class ActionsCell extends React.PureComponent {
  static propTypes = {
    organization: PropTypes.object,
    permissionTemplate: PermissionTemplateType.isRequired,
    topQualifiers: PropTypes.array.isRequired,
    refresh: CallbackType,
    fromDetails: PropTypes.bool
  };

  static defaultProps = {
    fromDetails: false
  };

  static contextTypes = {
    router: PropTypes.object
  };

  handleUpdateClick = () => {
    new UpdateView({
      model: new Backbone.Model(this.props.permissionTemplate),
      refresh: this.props.refresh
    }).render();
  };

  handleDeleteClick = () => {
    new DeleteView({
      model: new Backbone.Model(this.props.permissionTemplate)
    })
      .on('done', () => {
        const pathname = this.props.organization
          ? `/organizations/${this.props.organization.key}/permission_templates`
          : '/permission_templates';
        this.context.router.replace(pathname);
        this.props.refresh();
      })
      .render();
  };

  setDefault = qualifier => () => {
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

  renderSetDefaultLink(qualifier, child) {
    return (
      <ActionsDropdownItem
        key={qualifier}
        className="js-set-default"
        data-qualifier={qualifier}
        onClick={this.setDefault(qualifier)}>
        {child}
      </ActionsDropdownItem>
    );
  }

  renderIfSingleTopQualifier(availableQualifiers) {
    return availableQualifiers.map(qualifier =>
      this.renderSetDefaultLink(
        qualifier,
        <span>{translate('permission_templates.set_default')}</span>
      )
    );
  }

  renderIfMultipleTopQualifiers(availableQualifiers) {
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

        {t.defaultFor.length === 0 && (
          <ActionsDropdownItem className="js-delete" onClick={this.handleDeleteClick}>
            {translate('delete')}
          </ActionsDropdownItem>
        )}
      </ActionsDropdown>
    );
  }
}
