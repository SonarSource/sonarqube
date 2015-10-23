import _ from 'underscore';
import React from 'react';
import { getSystemInfo } from '../../api/system';
import Section from './section';

const SECTIONS_ORDER = ['SonarQube', 'Database', 'Plugins', 'System', 'ElasticSearch', 'JvmProperties',
  'ComputeEngine'];

export default React.createClass({
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

  render() {
    let sections = null;
    if (this.state && this.state.sections) {
      sections = this.state.sections.map(section => {
        return <Section key={section.name} section={section.name} items={section.items}/>;
      });
    }

    return <div className="page">
      <header className="page-header">
        <h1 className="page-title">{window.t('system_info.page')}</h1>
        <div className="page-actions">
          <a className="spacer-right" href={window.baseUrl + '/api/system/logs'} id="logs-link">Logs</a>
          <a href={window.baseUrl + '/api/system/info'} id="download-link">Download</a>
        </div>
      </header>
      {sections}
    </div>;
  }
});
