/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import shallowCompare from 'react-addons-shallow-compare';
import { connect } from 'react-redux';
import Header from './Header';
import UpdateForm from './UpdateForm';
import BulkUpdate from './BulkUpdate';
import FineGrainedUpdate from './FineGrainedUpdate';
import { getProjectModules } from '../store/rootReducer';
import { fetchProjectModules, changeKey } from '../store/actions';
import { translate } from '../../../helpers/l10n';

class Key extends React.Component {
  static propTypes = {
    component: React.PropTypes.object.isRequired,
    fetchProjectModules: React.PropTypes.func.isRequired,
    changeKey: React.PropTypes.func.isRequired
  };

  state = {
    tab: 'bulk'
  };

  componentDidMount () {
    this.props.fetchProjectModules(this.props.component.key);
  }

  shouldComponentUpdate (nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  handleChangeKey (key, newKey) {
    return this.props.changeKey(key, newKey).then(() => {
      if (key === this.props.component.key) {
        window.location = window.baseUrl +
            '/project/key?id=' + encodeURIComponent(newKey);
      }
    });
  }

  handleChangeTab (tab, e) {
    e.preventDefault();
    e.target.blur();
    this.setState({ tab });
  }

  render () {
    const { component, modules } = this.props;

    const noModules = modules != null && modules.length === 0;
    const hasModules = modules != null && modules.length > 0;

    const { tab } = this.state;

    return (
        <div id="project-key" className="page page-limited">
          <Header/>

          {modules == null && (
              <i className="spinner"/>
          )}

          {noModules && (
              <UpdateForm
                  component={component}
                  onKeyChange={this.handleChangeKey.bind(this)}/>
          )}

          {hasModules && (
              <div>
                <div className="big-spacer-bottom">
                  <ul className="tabs">
                    <li>
                      <a id="update-key-tab-bulk"
                         className={tab === 'bulk' ? 'selected' : ''}
                         href="#"
                         onClick={this.handleChangeTab.bind(this, 'bulk')}>
                        {translate('update_key.bulk_update')}
                      </a>
                    </li>
                    <li>
                      <a id="update-key-tab-fine"
                         className={tab === 'fine' ? 'selected' : ''}
                         href="#"
                         onClick={this.handleChangeTab.bind(this, 'fine')}>
                        {translate('update_key.fine_grained_key_update')}
                      </a>
                    </li>
                  </ul>
                </div>

                {tab === 'bulk' && (
                    <BulkUpdate component={component}/>
                )}

                {tab === 'fine' && (
                    <FineGrainedUpdate
                        component={component}
                        modules={modules}
                        onKeyChange={this.handleChangeKey.bind(this)}/>
                )}
              </div>
          )}
        </div>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  modules: getProjectModules(state, ownProps.component.key)
});

export default connect(
    mapStateToProps,
    { fetchProjectModules, changeKey }
)(Key);
