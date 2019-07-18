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
import { withRouter, WithRouterProps } from 'react-router';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { save } from 'sonar-ui-common/helpers/storage';
import OrganizationSelect from '../components/OrganizationSelect';
import { ORGANIZATION_IMPORT_REDIRECT_TO_PROJECT_TIMESTAMP } from '../organization/utils';

interface Props {
  autoImport?: boolean;
  onChange: (organization: T.Organization) => void;
  organization: string;
  organizations: T.Organization[];
}

export class OrganizationInput extends React.PureComponent<Props & WithRouterProps> {
  handleLinkClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.stopPropagation();
    save(ORGANIZATION_IMPORT_REDIRECT_TO_PROJECT_TIMESTAMP, Date.now().toString(10));
    this.props.router.push({
      pathname: '/create-organization',
      state: { tab: this.props.autoImport ? 'auto' : 'manual' }
    });
  };

  render() {
    const { autoImport, onChange, organization, organizations } = this.props;
    return (
      <div className="form-field spacer-bottom">
        <label htmlFor="select-organization">
          <span className="text-middle">
            <strong>{translate('onboarding.create_project.organization')}</strong>
            <em className="mandatory">*</em>
          </span>
        </label>
        <OrganizationSelect
          hideIcons={!autoImport}
          onChange={onChange}
          organization={organization}
          organizations={organizations}
        />
        <a className="big-spacer-left js-new-org" href="#" onClick={this.handleLinkClick}>
          {autoImport
            ? translate('onboarding.create_project.import_new_org')
            : translate('onboarding.create_project.create_new_org')}
        </a>
      </div>
    );
  }
}

export default withRouter(OrganizationInput);
