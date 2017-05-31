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
import { union } from 'lodash';
import Backbone from 'backbone';
import WorkspaceListItemView from '../../components/navigator/workspace-list-item-view';
import ProfileActivationView from './rule/profile-activation-view';
import RuleFilterMixin from './rule/rule-filter-mixin';
import Template from './templates/coding-rules-workspace-list-item.hbs';
import confirmDialog from './confirm-dialog';
import { translate, translateWithParameters } from '../../helpers/l10n';

export default WorkspaceListItemView.extend(RuleFilterMixin).extend({
  className: 'coding-rule',
  template: Template,

  modelEvents: {
    change: 'render'
  },

  events: {
    click: 'selectCurrent',
    dblclick: 'openRule',
    'click .js-rule': 'openRule',
    'click .js-rule-filter': 'onRuleFilterClick',
    'click .coding-rules-detail-quality-profile-activate': 'activate',
    'click .coding-rules-detail-quality-profile-change': 'change',
    'click .coding-rules-detail-quality-profile-revert': 'revert',
    'click .coding-rules-detail-quality-profile-deactivate': 'deactivate'
  },

  onRender() {
    WorkspaceListItemView.prototype.onRender.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip({
      container: 'body'
    });
  },

  onDestroy() {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  selectCurrent() {
    this.options.app.state.set({ selectedIndex: this.model.get('index') });
  },

  openRule() {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
    this.options.app.controller.showDetails(this.model);
  },

  activate() {
    const that = this;
    const selectedProfile = this.options.app.state.get('query').qprofile;
    const othersQualityProfiles = this.options.app.qualityProfiles.filter(
      profile => profile.key !== selectedProfile
    );
    const activationView = new ProfileActivationView({
      rule: this.model,
      collection: new Backbone.Collection(othersQualityProfiles),
      app: this.options.app
    });
    activationView.on('profileActivated', (severity, params, profile) => {
      const activation = {
        severity,
        params,
        inherit: 'NONE',
        qProfile: profile
      };
      that.model.set({ activation });
    });
    activationView.render();
  },

  deactivate() {
    const that = this;
    const ruleKey = this.model.get('key');
    const activation = this.model.get('activation');
    confirmDialog({
      title: translate('coding_rules.deactivate'),
      html: translateWithParameters('coding_rules.deactivate.confirm'),
      yesHandler() {
        return $.ajax({
          type: 'POST',
          url: window.baseUrl + '/api/qualityprofiles/deactivate_rule',
          data: {
            profile_key: activation.qProfile,
            rule_key: ruleKey
          }
        }).done(() => {
          that.model.unset('activation');
        });
      }
    });
  },

  serializeData() {
    const selectedProfileKey = this.options.app.state.get('query').qprofile;
    const selectedProfile =
      selectedProfileKey &&
      this.options.app.qualityProfiles.find(profile => profile.key === selectedProfileKey);
    const isSelectedProfileBuiltIn = selectedProfile != null && selectedProfile.isBuiltIn;

    return {
      ...WorkspaceListItemView.prototype.serializeData.apply(this, arguments),
      tags: union(this.model.get('sysTags'), this.model.get('tags')),
      canWrite: this.options.app.canWrite,
      selectedProfile: selectedProfileKey,
      isSelectedProfileBuiltIn
    };
  }
});
