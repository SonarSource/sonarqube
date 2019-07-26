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
import Helmet from 'react-helmet';
import { InjectedIntlProps, injectIntl } from 'react-intl';
import { connect } from 'react-redux';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import { getExtensionStart } from '../../../helpers/extensions';
import { addGlobalErrorMessage } from '../../../store/globalMessages';
import { getCurrentUser, Store } from '../../../store/rootReducer';
import * as theme from '../../theme';
import getStore from '../../utils/getStore';

interface Props extends InjectedIntlProps {
  currentUser: T.CurrentUser;
  extension: T.Extension;
  location: Location;
  onFail: (message: string) => void;
  options?: T.Dict<any>;
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

  handleStart = (start: Function) => {
    const store = getStore();
    const result = start({
      store,
      el: this.container,
      currentUser: this.props.currentUser,
      intl: this.props.intl,
      location: this.props.location,
      router: this.props.router,
      theme,
      ...this.props.options
    });

    if (React.isValidElement(result)) {
      this.setState({ extensionElement: result });
    } else {
      this.stop = result;
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
  withRouter(
    connect(
      mapStateToProps,
      mapDispatchToProps
    )(Extension)
  )
);
