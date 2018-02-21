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
import { changeKey, fetchProjectModules } from '../store/actions';
import { translate } from '../../../helpers/l10n';
import {
  addGlobalErrorMessage,
  addGlobalSuccessMessage,
  closeAllGlobalMessages
} from '../../../store/globalMessages/duck';
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

  handleChangeKey = (key, newKey) => {
    return this.props.changeKey(key, newKey).then(() => {
      if (key === this.props.component.key) {
        this.props.addGlobalSuccessMessage(translate('update_key.key_updated.reload'));
        RecentHistory.remove(key);
        reloadUpdateKeyPage(newKey);
      } else {
        this.props.addGlobalSuccessMessage(translate('update_key.key_updated'));
      }
    });
  };

  handleChangeTab = event => {
    event.preventDefault();
    event.currentTarget.blur();
    const { tab } = event.currentTarget.dataset;
    this.setState({ tab });
    this.props.closeAllGlobalMessages();
  };

  render() {
    const { component, modules } = this.props;

    const noModules = modules != null && modules.length === 0;
    const hasModules = modules != null && modules.length > 0;

    const { tab } = this.state;

    return (
      <div className="page page-limited" id="project-key">
        <Helmet title={translate('update_key.page')} />
        <Header />

        {modules == null && <i className="spinner" />}

        {noModules && (
          <div>
            <UpdateForm component={component} onKeyChange={this.handleChangeKey} />
          </div>
        )}

        {hasModules && (
          <div className="boxed-group boxed-group-inner">
            <div className="big-spacer-bottom">
              <ul className="tabs">
                <li>
                  <a
                    className={tab === 'bulk' ? 'selected' : ''}
                    data-tab="bulk"
                    href="#"
                    id="update-key-tab-bulk"
                    onClick={this.handleChangeTab}>
                    {translate('update_key.bulk_update')}
                  </a>
                </li>
                <li>
                  <a
                    className={tab === 'fine' ? 'selected' : ''}
                    data-tab="fine"
                    href="#"
                    id="update-key-tab-fine"
                    onClick={this.handleChangeTab}>
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
                onError={this.props.addGlobalErrorMessage}
                onKeyChange={this.handleChangeKey}
                onSuccess={this.props.closeAllGlobalMessages}
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
