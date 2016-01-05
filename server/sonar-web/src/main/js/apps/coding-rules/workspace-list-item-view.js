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
import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import WorkspaceListItemView from '../../components/navigator/workspace-list-item-view';
import ProfileActivationView from './rule/profile-activation-view';
import RuleFilterMixin from './rule/rule-filter-mixin';
import Template from './templates/coding-rules-workspace-list-item.hbs';
import confirmDialog from './confirm-dialog';

export default WorkspaceListItemView.extend(RuleFilterMixin).extend({
  className: 'coding-rule',
  template: Template,

  modelEvents: {
    'change': 'render'
  },

  events: {
    'click': 'selectCurrent',
    'dblclick': 'openRule',
    'click .js-rule': 'openRule',
    'click .js-rule-filter': 'onRuleFilterClick',
    'click .coding-rules-detail-quality-profile-activate': 'activate',
    'click .coding-rules-detail-quality-profile-change': 'change',
    'click .coding-rules-detail-quality-profile-revert': 'revert',
    'click .coding-rules-detail-quality-profile-deactivate': 'deactivate'
  },

  selectCurrent: function () {
    this.options.app.state.set({ selectedIndex: this.model.get('index') });
  },

  openRule: function () {
    this.options.app.controller.showDetails(this.model);
  },

  activate: function () {
    var that = this,
        selectedProfile = this.options.app.state.get('query').qprofile,
        othersQualityProfiles = _.reject(this.options.app.qualityProfiles, function (profile) {
          return profile.key === selectedProfile;
        }),
        activationView = new ProfileActivationView({
          rule: this.model,
          collection: new Backbone.Collection(othersQualityProfiles),
          app: this.options.app
        });
    activationView.on('profileActivated', function (severity, params, profile) {
      var activation = {
        severity: severity,
        inherit: 'NONE',
        params: params,
        qProfile: profile
      };
      that.model.set({ activation: activation });
    });
    activationView.render();
  },

  deactivate: function () {
    var that = this,
        ruleKey = this.model.get('key'),
        activation = this.model.get('activation');
    confirmDialog({
      title: window.t('coding_rules.deactivate'),
      html: window.tp('coding_rules.deactivate.confirm'),
      yesHandler: function () {
        return $.ajax({
          type: 'POST',
          url: baseUrl + '/api/qualityprofiles/deactivate_rule',
          data: {
            profile_key: activation.qProfile,
            rule_key: ruleKey
          }
        }).done(function () {
          that.model.unset('activation');
        });
      }
    });
  },

  serializeData: function () {
    return _.extend(WorkspaceListItemView.prototype.serializeData.apply(this, arguments), {
      tags: _.union(this.model.get('sysTags'), this.model.get('tags')),
      canWrite: this.options.app.canWrite,
      selectedProfile: this.options.app.state.get('query').qprofile
    });
  }
});
