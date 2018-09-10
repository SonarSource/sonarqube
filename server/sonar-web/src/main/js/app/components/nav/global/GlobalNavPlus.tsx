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
import { Link } from 'react-router';
import PlusIcon from '../../../../components/icons-components/PlusIcon';
import Dropdown from '../../../../components/controls/Dropdown';
import { translate } from '../../../../helpers/l10n';

interface Props {
  openProjectOnboarding: () => void;
}

export default class GlobalNavPlus extends React.PureComponent<Props> {
  handleNewProjectClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.openProjectOnboarding();
  };

  render() {
    return (
      <Dropdown
        overlay={
          <ul className="menu">
            <li>
              <a className="js-new-project" href="#" onClick={this.handleNewProjectClick}>
                {translate('provisioning.create_new_project')}
              </a>
            </li>
            <li className="divider" />
            <li>
              <Link className="js-new-organization" to="/create-organization">
                {translate('my_account.create_new_organization')}
              </Link>
            </li>
          </ul>
        }
        tagName="li">
        <a
          className="navbar-plus"
          href="#"
          title={translate('my_account.create_new_project_or_organization')}>
          <PlusIcon />
        </a>
      </Dropdown>
    );
  }
}
