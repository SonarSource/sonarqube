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
import { WithRouterProps } from 'react-router';
import Helmet from 'react-helmet';
import ManualProjectCreate from './ManualProjectCreate';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import { translate } from '../../../helpers/l10n';
import { addWhitePageClass, removeWhitePageClass } from '../../../helpers/pages';
import { getProjectUrl } from '../../../helpers/urls';
import './style.css';

interface Props {
  currentUser: T.LoggedInUser;
}

export class CreateProjectPageSonarQube extends React.PureComponent<Props & WithRouterProps> {
  componentDidMount() {
    addWhitePageClass();
  }

  componentWillUnmount() {
    removeWhitePageClass();
  }

  handleProjectCreate = (projectKeys: string[]) => {
    if (projectKeys.length === 1) {
      this.props.router.push(getProjectUrl(projectKeys[0]));
    }
  };

  render() {
    const { currentUser } = this.props;
    const header = translate('my_account.create_new.TRK');
    return (
      <>
        <Helmet title={header} titleTemplate="%s" />
        <div className="page page-limited huge-spacer-top huge-spacer-bottom">
          <header className="page-header bordered-bottom big-spacer-bottom">
            <h1 className="page-title huge big-spacer-bottom">
              <strong>{header}</strong>
            </h1>
          </header>
          <ManualProjectCreate
            currentUser={currentUser}
            onProjectCreate={this.handleProjectCreate}
          />
        </div>
      </>
    );
  }
}

export default whenLoggedIn(CreateProjectPageSonarQube);
