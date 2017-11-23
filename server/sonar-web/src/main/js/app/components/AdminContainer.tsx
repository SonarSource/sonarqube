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
import * as React from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import { getAppState } from '../../store/rootReducer';
import { translate } from '../../helpers/l10n';
import { AppState } from '../../store/appState/duck';

interface Props {
  appState: AppState;
}

class AdminContainer extends React.PureComponent<Props> {
  componentDidMount() {
    if (!this.props.appState.canAdmin) {
      // workaround cyclic dependencies
      import('../utils/handleRequiredAuthorization').then(
        handleRequredAuthorization => handleRequredAuthorization.default(),
        () => {}
      );
    }
  }

  render() {
    const { adminPages, canAdmin } = this.props.appState;

    // Check that the adminPages are loaded
    if (!adminPages || !canAdmin) {
      return null;
    }

    const defaultTitle = translate('layout.settings');

    return (
      <div>
        <Helmet defaultTitle={defaultTitle} titleTemplate={'%s - ' + defaultTitle} />
        {this.props.children}
      </div>
    );
  }
}

const mapStateToProps = (state: any) => ({
  appState: getAppState(state)
});

export default connect(mapStateToProps)(AdminContainer);
