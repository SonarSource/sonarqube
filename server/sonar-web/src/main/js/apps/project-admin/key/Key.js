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
import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import Header from './Header';
import UpdateForm from './UpdateForm';
import BulkUpdate from './BulkUpdate';
import FineGrainedUpdate from './FineGrainedUpdate';
import { reloadUpdateKeyPage } from './utils';
import { fetchProjectModules, changeKey } from '../store/actions';
import { translate } from '../../../helpers/l10n';
import {
  addGlobalErrorMessage,
  closeAllGlobalMessages,
  addGlobalSuccessMessage
} from '../../../store/globalMessages/duck';
import { parseError } from '../../../helpers/request';
import RecentHistory from '../../../app/components/RecentHistory';
import { getProjectAdminProjectModules } from '../../../store/rootReducer';

class Key extends React.PureComponent {
  static propTypes = {
    component: PropTypes.object,
    fetchProjectModules: PropTypes.func.isRequired,
    changeKey: PropTypes.func.isRequired,
    addGlobalErrorMessage: PropTypes.func.isRequired,
    addGlobalSuccessMessage: PropTypes.func.isRequired,
    closeAllGlobalMessages: PropTypes.func.isRequired
  };

  state = {
    tab: 'bulk'
  };

  componentDidMount() {
    this.props.fetchProjectModules(this.props.component.key);
  }

  handleChangeKey(key, newKey) {
    return this.props
      .changeKey(key, newKey)
      .then(() => {
        if (key === this.props.component.key) {
          this.props.addGlobalSuccessMessage(translate('update_key.key_updated.reload'));
          RecentHistory.remove(key);
          reloadUpdateKeyPage(newKey);
        } else {
          this.props.addGlobalSuccessMessage(translate('update_key.key_updated'));
        }
      })
      .catch(e => {
        parseError(e).then(this.props.addGlobalErrorMessage);
      });
  }

  handleChangeTab(tab, e) {
    e.preventDefault();
    e.target.blur();
    this.setState({ tab });
    this.props.closeAllGlobalMessages();
  }

  render() {
    const { component, modules } = this.props;

    const noModules = modules != null && modules.length === 0;
    const hasModules = modules != null && modules.length > 0;

    const { tab } = this.state;

    return (
      <div id="project-key" className="page page-limited">
        <Helmet title={translate('update_key.page')} />
        <Header />

        {modules == null && <i className="spinner" />}

        {noModules && (
          <div>
            <UpdateForm component={component} onKeyChange={this.handleChangeKey.bind(this)} />
          </div>
        )}

        {hasModules && (
          <div className="boxed-group boxed-group-inner">
            <div className="big-spacer-bottom">
              <ul className="tabs">
                <li>
                  <a
                    id="update-key-tab-bulk"
                    className={tab === 'bulk' ? 'selected' : ''}
                    href="#"
                    onClick={this.handleChangeTab.bind(this, 'bulk')}>
                    {translate('update_key.bulk_update')}
                  </a>
                </li>
                <li>
                  <a
                    id="update-key-tab-fine"
                    className={tab === 'fine' ? 'selected' : ''}
                    href="#"
                    onClick={this.handleChangeTab.bind(this, 'fine')}>
                    {translate('update_key.fine_grained_key_update')}
                  </a>
                </li>
              </ul>
            </div>

            {tab === 'bulk' && <BulkUpdate component={component} />}

            {tab === 'fine' && (
              <FineGrainedUpdate
                component={component}
                modules={modules}
                onKeyChange={this.handleChangeKey.bind(this)}
                onSuccess={this.props.closeAllGlobalMessages}
                onError={this.props.addGlobalErrorMessage}
              />
            )}
          </div>
        )}
      </div>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  modules: getProjectAdminProjectModules(state, ownProps.location.query.id)
});

export default connect(mapStateToProps, {
  fetchProjectModules,
  changeKey,
  addGlobalErrorMessage,
  addGlobalSuccessMessage,
  closeAllGlobalMessages
})(Key);
