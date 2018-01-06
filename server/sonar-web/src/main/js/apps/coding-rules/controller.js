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
import key from 'keymaster';
import Controller from '../../components/navigator/controller';
import Rule from './models/rule';
import RuleDetailsView from './rule-details-view';
import { searchRules, getRuleDetails } from '../../api/rules';

export default Controller.extend({
  pageSize: 200,
  ruleFields: [
    'name',
    'lang',
    'langName',
    'sysTags',
    'tags',
    'status',
    'severity',
    'isTemplate',
    'templateKey'
  ],

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

    const options = { ...this._searchParameters(), ...this.app.state.get('query') };
    return searchRules(options).then(
      r => {
        const rules = this.app.list.parseRules(r);
        if (firstPage) {
          this.app.list.reset(rules);
        } else {
          this.app.list.add(rules);
        }
        this.app.list.setIndex();
        this.app.list.addExtraAttributes(this.app.repositories);
        this.app.facets.reset(this._allFacets());
        this.app.facets.add(r.facets, { merge: true });
        this.enableFacets(this._enabledFacets());
        this.app.state.set({
          page: r.p,
          pageSize: r.ps,
          total: r.total,
          maxResultsReached: r.p * r.ps >= r.total
        });
        if (firstPage && this.isRulePermalink()) {
          this.showDetails(this.app.list.first());
        }
      },
      () => {
        this.app.state.set({ maxResultsReached: true });
      }
    );
  },

  isRulePermalink() {
    const query = this.app.state.get('query');
    return query.rule_key != null && this.app.list.length === 1;
  },

  requestFacet(id) {
    const facet = this.app.facets.get(id);
    const options = { facets: id, ps: 1, ...this.app.state.get('query') };
    return searchRules(options).then(r => {
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
    const parameters = { key: rule.id, actives: true, organization: this.app.organization };
    return getRuleDetails(parameters).then(r => {
      rule.set(r.rule);
      rule.addExtraAttributes(this.app.repositories);
      return r;
    });
  },

  showDetails(rule) {
    const that = this;
    const ruleModel = typeof rule === 'string' ? new Rule({ key: rule }) : rule;
    this.app.layout.workspaceDetailsRegion.reset();
    this.getRuleDetails(ruleModel).then(
      r => {
        key.setScope('details');
        that.app.workspaceListView.unbindScrollEvents();
        that.app.state.set({ rule: ruleModel });
        that.app.workspaceDetailsView = new RuleDetailsView({
          app: that.app,
          model: ruleModel,
          actives: r.actives
        });
        that.app.layout.showDetails();
        that.app.layout.workspaceDetailsRegion.show(that.app.workspaceDetailsView);
      },
      () => {}
    );
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
  },

  updateActivation(rule, actives) {
    const selectedProfile = this.options.app.state.get('query').qprofile;
    if (selectedProfile) {
      const profile = (actives || []).find(activation => activation.qProfile === selectedProfile);
      const listRule = this.app.list.get(rule.id);
      if (profile && listRule) {
        listRule.set('activation', {
          ...listRule.get('activation'),
          inherit: profile.inherit,
          severity: profile.severity
        });
      }
    }
  }
});
