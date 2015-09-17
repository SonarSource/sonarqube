import $ from 'jquery';
import _ from 'underscore';
import Controller from 'components/navigator/controller';
import Rule from './models/rule';
import RuleDetailsView from './rule-details-view';

export default Controller.extend({
  pageSize: 200,
  ruleFields: [
    'name', 'lang', 'langName', 'sysTags', 'tags', 'status', 'severity',
    'debtChar', 'debtCharName', 'debtSubChar', 'debtSubCharName'
  ],


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
  },

  activateCurrent: function () {
    var rule = this.app.list.at(this.app.state.get('selectedIndex'));
    var ruleView = this.app.workspaceListView.children.findByModel(rule);
    ruleView.$('.coding-rules-detail-quality-profile-activate').click();
  },

  deactivateCurrent: function () {
    var rule = this.app.list.at(this.app.state.get('selectedIndex'));
    var ruleView = this.app.workspaceListView.children.findByModel(rule);
    ruleView.$('.coding-rules-detail-quality-profile-deactivate').click();
  }

});


