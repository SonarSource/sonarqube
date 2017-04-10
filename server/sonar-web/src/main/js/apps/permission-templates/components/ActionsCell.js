/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { Link } from 'react-router';
import { difference } from 'lodash';
import Backbone from 'backbone';
import { PermissionTemplateType, CallbackType } from '../propTypes';
import QualifierIcon from '../../../components/shared/qualifier-icon';
import UpdateView from '../views/UpdateView';
import DeleteView from '../views/DeleteView';
import { translate } from '../../../helpers/l10n';
import { setDefaultPermissionTemplate } from '../../../api/permissions';

export default class ActionsCell extends React.Component {
  static propTypes = {
    organization: React.PropTypes.object,
    permissionTemplate: PermissionTemplateType.isRequired,
    topQualifiers: React.PropTypes.array.isRequired,
    refresh: CallbackType,
    fromDetails: React.PropTypes.bool
  };

  static defaultProps = {
    fromDetails: false
  };

  static contextTypes = {
    router: React.PropTypes.object
  };

  handleUpdateClick(e) {
    e.preventDefault();
    new UpdateView({
      model: new Backbone.Model(this.props.permissionTemplate),
      refresh: this.props.refresh
    }).render();
  }

  handleDeleteClick(e) {
    e.preventDefault();
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
  }

  setDefault(qualifier, e) {
    e.preventDefault();
    setDefaultPermissionTemplate(this.props.permissionTemplate.id, qualifier).then(
      this.props.refresh
    );
  }

  getAvailableQualifiers() {
    const topQualifiers = this.props.organization && !this.props.organization.isDefault
      ? ['TRK']
      : this.props.topQualifiers;
    return difference(topQualifiers, this.props.permissionTemplate.defaultFor);
  }

  renderDropdownIcon(icon) {
    const style = {
      display: 'inline-block',
      width: 16,
      marginRight: 4,
      textAlign: 'center'
    };
    return <div style={style}>{icon}</div>;
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
      <li key={qualifier}>
        <a
          href="#"
          className="js-set-default"
          data-qualifier={qualifier}
          onClick={this.setDefault.bind(this, qualifier)}>
          {this.renderDropdownIcon(<i className="icon-check" />)}
          {child}
        </a>
      </li>
    );
  }

  renderIfSingleTopQualifier(availableQualifiers) {
    return availableQualifiers.map(qualifier =>
      this.renderSetDefaultLink(
        qualifier,
        <span>{translate('permission_templates.set_default')}</span>
      ));
  }

  renderIfMultipleTopQualifiers(availableQualifiers) {
    return availableQualifiers.map(qualifier =>
      this.renderSetDefaultLink(
        qualifier,
        <span>
          {translate('permission_templates.set_default_for')}
          {' '}
          <QualifierIcon qualifier={qualifier} />
          {' '}
          {translate('qualifiers', qualifier)}
        </span>
      ));
  }

  render() {
    const { permissionTemplate: t, organization } = this.props;

    const pathname = organization
      ? `/organizations/${organization.key}/permission_templates`
      : '/permission_templates';

    return (
      <div className="dropdown">
        <button className="dropdown-toggle" data-toggle="dropdown">
          {translate('actions')}
          {' '}
          <i className="icon-dropdown" />
        </button>

        <ul className="dropdown-menu dropdown-menu-right">
          {this.renderSetDefaultsControl()}

          {!this.props.fromDetails &&
            <li>
              <Link to={{ pathname, query: { id: t.id } }}>
                {this.renderDropdownIcon(<i className="icon-edit" />)}
                Edit Permissions
              </Link>
            </li>}

          <li>
            <a href="#" className="js-update" onClick={this.handleUpdateClick.bind(this)}>
              {this.renderDropdownIcon(<i className="icon-edit" />)}
              Update Details
            </a>
          </li>

          {t.defaultFor.length === 0 &&
            <li>
              <a href="#" className="js-delete" onClick={this.handleDeleteClick.bind(this)}>
                {this.renderDropdownIcon(<i className="icon-delete" />)}
                {translate('delete')}
              </a>
            </li>}
        </ul>
      </div>
    );
  }
}
