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
import $ from 'jquery';
import Marionette from 'backbone.marionette';
import Template from '../templates/rule/coding-rules-rule-issues.hbs';
import { getComponentIssuesUrlAsString } from '../../../helpers/urls';

export default Marionette.ItemView.extend({
  template: Template,

  initialize() {
    const that = this;
    this.total = null;
    this.projects = [];
    this.requestIssues().done(() => {
      that.render();
    });
  },

  requestIssues() {
    const url = window.baseUrl + '/api/issues/search';
    const options = {
      rules: this.model.id,
      resolved: false,
      ps: 1,
      facets: 'projectUuids'
    };
    const { organization } = this.options.app;
    if (organization) {
      Object.assign(options, { organization });
    }
    return $.get(url, options).done(r => {
      const projectsFacet = r.facets.find(facet => facet.property === 'projectUuids');
      let projects = projectsFacet != null ? projectsFacet.values : [];
      projects = projects.map(project => {
        const projectBase = r.components.find(component => component.uuid === project.val);
        return {
          ...project,
          name: projectBase != null ? projectBase.longName : '',
          issuesUrl:
            projectBase != null &&
            getComponentIssuesUrlAsString(projectBase.key, {
              resolved: 'false',
              rules: this.model.id
            })
        };
      });
      this.projects = projects;
      this.total = r.total;
    });
  },

  serializeData() {
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      total: this.total,
      projects: this.projects
    };
  }
});
