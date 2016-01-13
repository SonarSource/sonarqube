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
import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Template from '../templates/rule/coding-rules-rule-issues.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  initialize: function () {
    var that = this;
    this.total = null;
    this.projects = [];
    this.requestIssues().done(function () {
      that.render();
    });
  },

  requestIssues: function () {
    var that = this,
        url = baseUrl + '/api/issues/search',
        options = {
          rules: this.model.id,
          resolved: false,
          ps: 1,
          facets: 'projectUuids'
        };
    return $.get(url, options).done(function (r) {
      var projectsFacet = _.findWhere(r.facets, { property: 'projectUuids' }),
          projects = projectsFacet != null ? projectsFacet.values : [];
      projects = projects.map(function (project) {
        var projectBase = _.findWhere(r.components, { uuid: project.val });
        return _.extend(project, {
          name: projectBase != null ? projectBase.longName : ''
        });
      });
      that.projects = projects;
      that.total = r.total;
    });
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      total: this.total,
      projects: this.projects,
      baseSearchUrl: baseUrl + '/issues/search#resolved=false|rules=' + encodeURIComponent(this.model.id)
    });
  }
});


