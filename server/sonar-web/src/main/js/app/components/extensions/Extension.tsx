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
import { withTheme } from '@emotion/react';
import { QueryClient } from '@tanstack/react-query';
import { Theme } from 'design-system';
import { isEqual } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { injectIntl, WrappedComponentProps } from 'react-intl';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import { getExtensionStart } from '../../../helpers/extensions';
import { addGlobalErrorMessage } from '../../../helpers/globalMessages';
import { translate } from '../../../helpers/l10n';
import { getCurrentL10nBundle } from '../../../helpers/l10nBundle';
import { getBaseUrl } from '../../../helpers/system';
import { withQueryClient } from '../../../queries/withQueryClientHoc';
import { AppState } from '../../../types/appstate';
import { ExtensionStartMethod } from '../../../types/extension';
import { Dict, Extension as TypeExtension } from '../../../types/types';
import { CurrentUser, HomePage } from '../../../types/users';
import * as theme from '../../theme';
import withAppStateContext from '../app-state/withAppStateContext';
import withCurrentUserContext from '../current-user/withCurrentUserContext';

export interface ExtensionProps extends WrappedComponentProps {
  theme: Theme;
  appState: AppState;
  currentUser: CurrentUser;
  extension: TypeExtension;
  location: Location;
  options?: Dict<any>;
  router: Router;
  queryClient: QueryClient;
  updateCurrentUserHomepage: (homepage: HomePage) => void;
}

interface State {
  extensionElement?: React.ReactElement<any>;
}

class Extension extends React.PureComponent<ExtensionProps, State> {
  container?: HTMLElement | null;
  stop?: Function;
  state: State = {};

  componentDidMount() {
    this.startExtension();
  }

  componentDidUpdate(prevProps: ExtensionProps) {
    if (prevProps.extension.key !== this.props.extension.key) {
      this.stopExtension();
      this.startExtension();
    } else if (!isEqual(prevProps.location, this.props.location)) {
      this.startExtension();
    }
  }

  componentWillUnmount() {
    this.stopExtension();
  }

  handleStart = (start: ExtensionStartMethod) => {
    const { theme: dsTheme, queryClient } = this.props;
    const result = start({
      appState: this.props.appState,
      el: this.container,
      currentUser: this.props.currentUser,
      intl: this.props.intl,
      location: this.props.location,
      router: this.props.router,
      theme,
      // New theme from design-system, we should drop old theme once the migration to miui is done
      dsTheme,
      baseUrl: getBaseUrl(),
      l10nBundle: getCurrentL10nBundle(),
      // See SONAR-16207 and core-extension-enterprise-server/src/main/js/portfolios/components/Header.tsx
      // for more information on why we're passing this as a prop to an extension.
      updateCurrentUserHomepage: this.props.updateCurrentUserHomepage,
      queryClient,
      ...this.props.options,
    });

    if (result) {
      if (React.isValidElement(result)) {
        this.setState({ extensionElement: result });
      } else if (typeof result === 'function') {
        this.stop = result;
      }
    }
  };

  handleFailure = () => {
    addGlobalErrorMessage(translate('page_extension_failed'));
  };

  startExtension() {
    getExtensionStart(this.props.extension.key).then(this.handleStart, this.handleFailure);
  }

  stopExtension() {
    if (this.stop) {
      this.stop();
      this.stop = undefined;
    } else {
      this.setState({ extensionElement: undefined });
    }
  }

  render() {
    return (
      <div>
        <Helmet title={this.props.extension.name} />
        {this.state.extensionElement ? (
          this.state.extensionElement
        ) : (
          <div ref={(container) => (this.container = container)} />
        )}
      </div>
    );
  }
}

export default injectIntl(
  withRouter(withTheme(withAppStateContext(withCurrentUserContext(withQueryClient(Extension))))),
);
