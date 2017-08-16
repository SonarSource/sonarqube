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
import key from 'keymaster';
import Controller from '../../components/navigator/controller';
import Rule from './models/rule';
import RuleDetailsView from './rule-details-view';
import throwGlobalError from '../../app/utils/throwGlobalError';

export default Controller.extend({
  pageSize: 200,
  ruleFields: ['name', 'lang', 'langName', 'sysTags', 'tags', 'status', 'severity'],

  _searchParameters() {
    const fields = this.ruleFields.slice();
    const profile = this.app.state.get('query').qprofile;
    if (profile != null) {
      fields.push('actives');
      fields.push('params');
      fields.push('isTemplate');
      fields.push('severity');
    }
    const params = {
      p: this.app.state.get('page'),
      ps: this.pageSize,
      facets: this._facetsFromServer().join(),
      f: fields.join()
    };
    if (this.app.state.get('query').q == null) {
      Object.assign(params, { s: 'name', asc: true });
    }
    return params;
  },

  fetchList(firstPage) {
    firstPage = firstPage == null ? true : firstPage;
    if (firstPage) {
      this.app.state.set({ selectedIndex: 0, page: 1 }, { silent: true });
    }

    this.hideDetails(firstPage);

    const that = this;
    const url = window.baseUrl + '/api/rules/search';
    const options = { ...this._searchParameters(), ...this.app.state.get('query') };
    return $.get(url, options)
      .done(r => {
        const rules = that.app.list.parseRules(r);
        if (firstPage) {
          that.app.list.reset(rules);
        } else {
          that.app.list.add(rules);
        }
        that.app.list.setIndex();
        that.app.list.addExtraAttributes(that.app.repositories);
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
      })
      .fail(error => {
        this.app.state.set({ maxResultsReached: true });
        throwGlobalError(error);
      });
  },

  isRulePermalink() {
    const query = this.app.state.get('query');
    return query.rule_key != null && this.app.list.length === 1;
  },

  requestFacet(id) {
    const url = window.baseUrl + '/api/rules/search';
    const facet = this.app.facets.get(id);
    const options = { facets: id, ps: 1, ...this.app.state.get('query') };
    return $.get(url, options).done(r => {
      const facetData = r.facets.find(facet => facet.property === id);
      if (facetData) {
        facet.set(facetData);
      }
    });
  },

  parseQuery() {
    const q = Controller.prototype.parseQuery.apply(this, arguments);
    delete q.asc;
    delete q.s;
    return q;
  },

  getRuleDetails(rule) {
    const that = this;
    const url = window.baseUrl + '/api/rules/show';
    const options = {
      key: rule.id,
      actives: true
    };
    if (this.app.organization) {
      options.organization = this.app.organization;
    }
    return $.get(url, options).done(data => {
      rule.set(data.rule);
      rule.addExtraAttributes(that.app.repositories);
    });
  },

  showDetails(rule) {
    const that = this;
    const ruleModel = typeof rule === 'string' ? new Rule({ key: rule }) : rule;
    this.app.layout.workspaceDetailsRegion.reset();
    this.getRuleDetails(ruleModel).done(data => {
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

  showDetailsForSelected() {
    const rule = this.app.list.at(this.app.state.get('selectedIndex'));
    this.showDetails(rule);
  },

  hideDetails(firstPage) {
    key.setScope('list');
    this.app.state.unset('rule');
    this.app.layout.workspaceDetailsRegion.reset();
    this.app.layout.hideDetails();
    this.app.workspaceListView.bindScrollEvents();
    if (firstPage) {
      this.app.workspaceListView.scrollTo();
    }
  },

  activateCurrent() {
    if (this.app.layout.detailsShow()) {
      this.app.workspaceDetailsView.$('#coding-rules-quality-profile-activate').click();
    } else {
      const rule = this.app.list.at(this.app.state.get('selectedIndex'));
      const ruleView = this.app.workspaceListView.children.findByModel(rule);
      ruleView.$('.coding-rules-detail-quality-profile-activate').click();
    }
  },

  deactivateCurrent() {
    if (!this.app.layout.detailsShow()) {
      const rule = this.app.list.at(this.app.state.get('selectedIndex'));
      const ruleView = this.app.workspaceListView.children.findByModel(rule);
      ruleView.$('.coding-rules-detail-quality-profile-deactivate').click();
    }
  }
});
