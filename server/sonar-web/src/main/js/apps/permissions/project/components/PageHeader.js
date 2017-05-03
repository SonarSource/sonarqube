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
// @flow
import React from 'react';
import { translate } from '../../../../helpers/l10n';
import ApplyTemplateView from '../views/ApplyTemplateView';

type Props = {|
  component: {
    configuration?: {
      canApplyPermissionTemplate: boolean,
      canUpdateProjectVisibilityToPrivate: boolean
    },
    key: string,
    qualifier: string,
    visibility: string
  },
  loadHolders: () => void,
  loading: boolean
|};

export default class PageHeader extends React.PureComponent {
  props: Props;

  handleApplyTemplate = (e: Event & { target: HTMLButtonElement }) => {
    e.preventDefault();
    e.target.blur();
    const { component, loadHolders } = this.props;
    const organization = component.organization ? { key: component.organization } : null;
    new ApplyTemplateView({ project: component, organization })
      .on('done', () => loadHolders())
      .render();
  };

  render() {
    const { component } = this.props;
    const configuration = component.configuration;
    const canApplyPermissionTemplate =
      configuration != null && configuration.canApplyPermissionTemplate;

    const description = ['VW', 'SVW'].includes(component.qualifier)
      ? translate('roles.page.description_portfolio')
      : translate('roles.page.description2');

    const visibilityDescription = component.qualifier === 'TRK'
      ? translate('visibility', component.visibility, 'description')
      : null;

    return (
      <header className="page-header">
        <h1 className="page-title">
          {translate('permissions.page')}
        </h1>

        {this.props.loading && <i className="spinner" />}

        {canApplyPermissionTemplate &&
          <div className="page-actions">
            <button className="js-apply-template" onClick={this.handleApplyTemplate}>
              Apply Template
            </button>
          </div>}

        <div className="page-description">
          <p>{description}</p>
          {visibilityDescription != null && <p>{visibilityDescription}</p>}
        </div>
      </header>
    );
  }
}
