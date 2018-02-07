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
// @flow
import React from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import { injectIntl } from 'react-intl';
import { getExtensionStart } from './utils';
import { addGlobalErrorMessage } from '../../../store/globalMessages/duck';
import { getCurrentUser } from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';
import getStore from '../../utils/getStore';

/*::
type Props = {
  currentUser: Object,
  extension: {
    key: string,
    name: string
  },
  intl: Object,
  location: { hash: string },
  onFail: string => void,
  options?: {},
  router: Object
};
*/

class Extension extends React.PureComponent {
  /*:: container: Object; */
  /*:: props: Props; */
  /*:: stop: ?Function; */

  componentDidMount() {
    this.startExtension();
  }

  componentDidUpdate(prevProps /*: Props */) {
    if (prevProps.extension !== this.props.extension) {
      this.stopExtension();
      this.startExtension();
    } else if (
      prevProps.location !== this.props.location &&
      // old router from backbone app updates hash, don't react in this case
      prevProps.location.hash === this.props.location.hash
    ) {
      this.startExtension();
    }
  }

  componentWillUnmount() {
    this.stopExtension();
  }

  handleStart = (start /*: Function */) => {
    const store = getStore();
    this.stop = start({
      store,
      el: this.container,
      currentUser: this.props.currentUser,
      intl: this.props.intl,
      location: this.props.location,
      router: this.props.router,
      ...this.props.options
    });
  };

  handleFailure = () => {
    this.props.onFail(translate('page_extension_failed'));
  };

  startExtension() {
    const { extension } = this.props;
    getExtensionStart(extension.key).then(this.handleStart, this.handleFailure);
  }

  stopExtension() {
    if (this.stop) {
      this.stop();
      this.stop = null;
    }
  }

  render() {
    return (
      <div>
        <Helmet title={this.props.extension.name} />
        <div ref={container => (this.container = container)} />
      </div>
    );
  }
}

export default injectIntl(withRouter(Extension));
