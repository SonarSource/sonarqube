/*
 * SonarQube :: Web
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
import _ from 'underscore';
import React from 'react';
import { getSystemInfo, restartAndWait } from '../../api/system';
import Section from './section';
import { translate } from '../../helpers/l10n';

const SECTIONS_ORDER = ['SonarQube', 'Database', 'Plugins', 'System', 'ElasticSearch', 'JvmProperties',
  'ComputeEngine'];

export default React.createClass({
  getInitialState() {
    return { restarting: false };
  },

  componentDidMount() {
    getSystemInfo().then(info => this.setState({ sections: this.parseSections(info) }));
  },

  parseSections (data) {
    let sections = Object.keys(data).map(section => {
      return { name: section, items: this.parseItems(data[section]) };
    });
    return this.orderSections(sections);
  },

  orderSections (sections) {
    return _.sortBy(sections, section => SECTIONS_ORDER.indexOf(section.name));
  },

  parseItems (data) {
    let items = Object.keys(data).map(item => {
      return { name: item, value: data[item] };
    });
    return this.orderItems(items);
  },

  orderItems (items) {
    return _.sortBy(items, 'name');
  },

  handleServerRestart () {
    this.setState({ restarting: true });
    restartAndWait().then(() => {
      document.location.reload();
    });
  },

  render() {
    let sections = null;
    if (this.state.sections) {
      sections = this.state.sections.map(section => {
        return <Section key={section.name} section={section.name} items={section.items}/>;
      });
    }

    return <div className="page">
      <header className="page-header">
        <h1 className="page-title">{translate('system_info.page')}</h1>
        <div className="page-actions">
          <a className="spacer-right" href={window.baseUrl + '/api/system/logs'} id="logs-link">Logs</a>
          <a href={window.baseUrl + '/api/system/info'} id="download-link">Download</a>
          {this.state.restarting ? (
              <i className="spinner"/>
          ) : (
              <button
                  id="restart-server-button"
                  className="big-spacer-left"
                  onClick={this.handleServerRestart}>
                Restart Server
              </button>
          )}
        </div>
      </header>
      {sections}
    </div>;
  }
});
