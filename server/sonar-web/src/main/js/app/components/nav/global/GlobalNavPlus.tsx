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
import * as classNames from 'classnames';
import * as PropTypes from 'prop-types';
import CreateOrganizationForm from '../../../../apps/account/organizations/CreateOrganizationForm';
import PlusIcon from '../../../../components/icons-components/PlusIcon';
import Dropdown from '../../../../components/controls/Dropdown';
import { translate } from '../../../../helpers/l10n';

interface Props {
  openOnboardingTutorial: () => void;
}

interface State {
  createOrganization: boolean;
}

export default class GlobalNavPlus extends React.PureComponent<Props, State> {
  static contextTypes = {
    router: PropTypes.object
  };

  constructor(props: Props) {
    super(props);
    this.state = { createOrganization: false };
  }

  handleNewProjectClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.openOnboardingTutorial();
  };

  openCreateOrganizationForm = () => this.setState({ createOrganization: true });

  closeCreateOrganizationForm = () => this.setState({ createOrganization: false });

  handleNewOrganizationClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.openCreateOrganizationForm();
  };

  handleCreateOrganization = ({ key }: { key: string }) => {
    this.closeCreateOrganizationForm();
    this.context.router.push(`/organizations/${key}`);
  };

  render() {
    return (
      <Dropdown>
        {({ onToggleClick, open }) => (
          <li className={classNames('dropdown', { open })}>
            <a className="navbar-plus" href="#" onClick={onToggleClick}>
              <PlusIcon />
            </a>
            <ul className="dropdown-menu dropdown-menu-right">
              <li>
                <a className="js-new-project" href="#" onClick={this.handleNewProjectClick}>
                  {translate('my_account.analyze_new_project')}
                </a>
              </li>
              <li className="divider" />
              <li>
                <a
                  className="js-new-organization"
                  href="#"
                  onClick={this.handleNewOrganizationClick}>
                  {translate('my_account.create_new_organization')}
                </a>
              </li>
            </ul>
            {this.state.createOrganization && (
              <CreateOrganizationForm
                onClose={this.closeCreateOrganizationForm}
                onCreate={this.handleCreateOrganization}
              />
            )}
          </li>
        )}
      </Dropdown>
    );
  }
}
