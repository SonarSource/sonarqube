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
import Marionette from 'backbone.marionette';
import Template from '../templates/rule/coding-rules-rule-issues.hbs';
import { searchIssues } from '../../../api/issues';
import { getPathUrlAsString, getComponentIssuesUrl, getBaseUrl } from '../../../helpers/urls';

export default Marionette.ItemView.extend({
  template: Template,

  initialize() {
    this.total = null;
    this.projects = [];
    this.loading = true;
    this.mounted = true;
    this.requestIssues().then(
      () => {
        if (this.mounted) {
          this.loading = false;
          this.render();
        }
      },
      () => {
        this.loading = false;
      }
    );
  },

  onDestroy() {
    this.mounted = false;
  },

  requestIssues() {
    const parameters = {
      rules: this.model.id,
      resolved: false,
      ps: 1,
      facets: 'projectUuids'
    };
    const { organization } = this.options.app;
    if (organization) {
      Object.assign(parameters, { organization });
    }
    return searchIssues(parameters).then(r => {
      const projectsFacet = r.facets.find(facet => facet.property === 'projectUuids');
      let projects = projectsFacet != null ? projectsFacet.values : [];
      projects = projects.map(project => {
        const projectBase = r.components.find(component => component.uuid === project.val);
        return {
          ...project,
          name: projectBase != null ? projectBase.longName : '',
          issuesUrl:
            projectBase != null &&
            getPathUrlAsString(
              getComponentIssuesUrl(projectBase.key, {
                resolved: 'false',
                rules: this.model.id
              })
            )
        };
      });
      this.projects = projects;
      this.total = r.total;
    });
  },

  serializeData() {
    const { organization } = this.options.app;
    const pathname = organization ? `/organizations/${organization}/issues` : '/issues';
    const query = `?resolved=false&rules=${encodeURIComponent(this.model.id)}`;
    const totalIssuesUrl = getBaseUrl() + pathname + query;
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      loading: this.loading,
      total: this.total,
      totalIssuesUrl,
      projects: this.projects
    };
  }
});
