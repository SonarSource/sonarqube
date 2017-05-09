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
import React from 'react';
import Helmet from 'react-helmet';
import { sortBy } from 'lodash';
import { getSystemInfo } from '../../api/system';
import Section from './section';
import { translate } from '../../helpers/l10n';
import RestartModal from '../../components/RestartModal';

const SECTIONS_ORDER = [
  'SonarQube',
  'Database',
  'System',
  'Elasticsearch State',
  'Elasticsearch',
  'Compute Engine Tasks',
  'Compute Engine State',
  'Compute Engine Database Connection',
  'JvmProperties'
];

export default class Main extends React.PureComponent {
  componentDidMount() {
    getSystemInfo().then(info => this.setState({ sections: this.parseSections(info) }));
  }

  parseSections = data => {
    const sections = Object.keys(data).map(section => {
      return { name: section, items: this.parseItems(data[section]) };
    });
    return this.orderSections(sections);
  };

  orderSections = sections => sortBy(sections, section => SECTIONS_ORDER.indexOf(section.name));

  parseItems = data => {
    const items = Object.keys(data).map(item => {
      return { name: item, value: data[item] };
    });
    return this.orderItems(items);
  };

  orderItems = items => sortBy(items, 'name');

  handleServerRestart = () => {
    new RestartModal().render();
  };

  render() {
    let sections = null;
    if (this.state && this.state.sections) {
      sections = this.state.sections
        .filter(section => SECTIONS_ORDER.indexOf(section.name) >= 0)
        .map(section => (
          <Section key={section.name} section={section.name} items={section.items} />
        ));
    }

    return (
      <div className="page">
        <Helmet title={translate('system_info.page')} />
        <header className="page-header">
          <h1 className="page-title">{translate('system_info.page')}</h1>
          <div className="page-actions">
            <a href={window.baseUrl + '/api/system/info'} id="download-link">Download</a>
            <div className="display-inline-block dropdown big-spacer-left">
              <button data-toggle="dropdown">Logs <i className="icon-dropdown" /></button>
              <ul className="dropdown-menu">
                <li>
                  <a href={window.baseUrl + '/api/system/logs?process=app'} id="logs-link">
                    Main Process
                  </a>
                </li>
                <li>
                  <a href={window.baseUrl + '/api/system/logs?process=ce'} id="ce-logs-link">
                    Compute Engine
                  </a>
                </li>
                <li>
                  <a href={window.baseUrl + '/api/system/logs?process=es'} id="es-logs-link">
                    Elasticsearch
                  </a>
                </li>
                <li>
                  <a href={window.baseUrl + '/api/system/logs?process=web'} id="web-logs-link">
                    Web Server
                  </a>
                </li>
              </ul>
            </div>
            <button
              id="restart-server-button"
              className="big-spacer-left"
              onClick={this.handleServerRestart}>
              Restart Server
            </button>
          </div>
        </header>
        {sections}
      </div>
    );
  }
}
