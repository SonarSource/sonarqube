/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { Helmet } from 'react-helmet-async';
import { injectIntl, WrappedComponentProps } from 'react-intl';
import { connect } from 'react-redux';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import { getExtensionStart } from '../../../helpers/extensions';
import { getCurrentL10nBundle, translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { addGlobalErrorMessage } from '../../../store/globalMessages';
import { getCurrentUser, Store } from '../../../store/rootReducer';
import { ExtensionStartMethod } from '../../../types/extension';
import { AppState, CurrentUser, Dict, Extension as TypeExtension } from '../../../types/types';
import * as theme from '../../theme';
import getStore from '../../utils/getStore';
import withAppStateContext from '../app-state/withAppStateContext';

interface Props extends WrappedComponentProps {
  appState: AppState;
  currentUser: CurrentUser;
  extension: TypeExtension;
  location: Location;
  onFail: (message: string) => void;
  options?: Dict<any>;
  router: Router;
}

interface State {
  extensionElement?: React.ReactElement<any>;
}

export class Extension extends React.PureComponent<Props, State> {
  container?: HTMLElement | null;
  stop?: Function;
  state: State = {};

  componentDidMount() {
    this.startExtension();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.extension !== this.props.extension) {
      this.stopExtension();
      this.startExtension();
    } else if (prevProps.location !== this.props.location) {
      this.startExtension();
    }
  }

  componentWillUnmount() {
    this.stopExtension();
  }

  handleStart = (start: ExtensionStartMethod) => {
    const store = getStore();
    const result = start({
      appState: this.props.appState,
      store,
      el: this.container,
      currentUser: this.props.currentUser,
      intl: this.props.intl,
      location: this.props.location,
      router: this.props.router,
      theme,
      baseUrl: getBaseUrl(),
      l10nBundle: getCurrentL10nBundle(),
      ...this.props.options
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
    this.props.onFail(translate('page_extension_failed'));
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
          <div ref={container => (this.container = container)} />
        )}
      </div>
    );
  }
}

const mapStateToProps = (state: Store) => ({ currentUser: getCurrentUser(state) });
const mapDispatchToProps = { onFail: addGlobalErrorMessage };

export default injectIntl(
  withRouter(withAppStateContext(connect(mapStateToProps, mapDispatchToProps)(Extension)))
);
