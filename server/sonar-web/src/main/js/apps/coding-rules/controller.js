/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
define([
  'components/navigator/controller',
  './models/rule',
  './rule-details-view'
], function (Controller, Rule, RuleDetailsView) {

  var $ = jQuery;

  return Controller.extend({
    pageSize: 200,
    ruleFields: ['name', 'lang', 'langName', 'sysTags', 'tags', 'status', 'severity',
                 'debtChar', 'debtCharName', 'debtSubChar', 'debtSubCharName'],


    _searchParameters: function () {
      var fields = this.ruleFields.slice(),
          profile = this.app.state.get('query').qprofile;
      if (profile != null) {
        fields.push('actives');
        fields.push('params');
        fields.push('isTemplate');
        fields.push('severity');
      }
      var params = {
        p: this.app.state.get('page'),
        ps: this.pageSize,
        facets: this._facetsFromServer().join(),
        f: fields.join()
      };
      if (this.app.state.get('query').q == null) {
        _.extend(params, { s: 'name', asc: true });
      }
      return params;
    },

    fetchList: function (firstPage) {
      firstPage = firstPage == null ? true : firstPage;
      if (firstPage) {
        this.app.state.set({ selectedIndex: 0, page: 1 }, { silent: true });
      }

      this.hideDetails(firstPage);

      var that = this,
          url = baseUrl + '/api/rules/search',
          options = _.extend(this._searchParameters(), this.app.state.get('query'));
      return $.get(url, options).done(function (r) {
        var rules = that.app.list.parseRules(r);
        if (firstPage) {
          that.app.list.reset(rules);
        } else {
          that.app.list.add(rules);
        }
        that.app.list.setIndex();
        that.app.list.addExtraAttributes(that.app.languages, that.app.repositories);
        that.app.facets.reset(that._allFacets());
        that.app.facets.add(r.facets, { merge: true });
        that.enableFacets(that._enabledFacets());
        that.app.state.set({
          page: r.p,
          pageSize: r.ps,
          total: r.total,
          maxResultsReached: r.p * r.ps >= r.total
        });
        if (firstPage && that.isRulePermalink()) {
          that.showDetails(that.app.list.first());
        }
      });
    },

    isRulePermalink: function () {
      var query = this.app.state.get('query');
      return query.rule_key != null && this.app.list.length === 1;
    },

    requestFacet: function (id) {
      var url = baseUrl + '/api/rules/search',
          facet = this.app.facets.get(id),
          options = _.extend({ facets: id, ps: 1 }, this.app.state.get('query'));
      return $.get(url, options).done(function (r) {
        var facetData = _.findWhere(r.facets, { property: id });
        if (facetData) {
          facet.set(facetData);
        }
      });
    },

    parseQuery: function () {
      var q = Controller.prototype.parseQuery.apply(this, arguments);
      delete q.asc;
      delete q.s;
      return q;
    },

    getRuleDetails: function (rule) {
      var that = this,
          url = baseUrl + '/api/rules/show',
          options = {
            key: rule.id,
            actives: true
          };
      return $.get(url, options).done(function (data) {
        rule.set(data.rule);
        rule.addExtraAttributes(that.app.repositories);
      });
    },

    showDetails: function (rule) {
      var that = this,
          ruleModel = typeof rule === 'string' ? new Rule({ key: rule }) : rule;
      this.app.layout.workspaceDetailsRegion.reset();
      this.getRuleDetails(ruleModel).done(function (data) {
        key.setScope('details');
        that.app.workspaceListView.unbindScrollEvents();
        that.app.state.set({ rule: ruleModel });
        that.app.workspaceDetailsView = new RuleDetailsView({
          app: that.app,
          model: ruleModel,
          actives: data.actives
        });
        that.app.layout.showDetails();
        that.app.layout.workspaceDetailsRegion.show(that.app.workspaceDetailsView);
      });
    },

    showDetailsForSelected: function () {
      var rule = this.app.list.at(this.app.state.get('selectedIndex'));
      this.showDetails(rule);
    },

    hideDetails: function (firstPage) {
      key.setScope('list');
      this.app.state.unset('rule');
      this.app.layout.workspaceDetailsRegion.reset();
      this.app.layout.hideDetails();
      this.app.workspaceListView.bindScrollEvents();
      if (firstPage) {
        this.app.workspaceListView.scrollTo();
      }
    }

  });

});
