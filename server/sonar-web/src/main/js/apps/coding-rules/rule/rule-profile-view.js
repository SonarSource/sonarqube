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
import { stringify } from 'querystring';
import $ from 'jquery';
import { sortBy } from 'lodash';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import ProfileActivationView from './profile-activation-view';
import Template from '../templates/rule/coding-rules-rule-profile.hbs';
import confirmDialog from '../confirm-dialog';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default Marionette.ItemView.extend({
  tagName: 'tr',
  template: Template,

  modelEvents: {
    change: 'render'
  },

  ui: {
    change: '.coding-rules-detail-quality-profile-change',
    revert: '.coding-rules-detail-quality-profile-revert',
    deactivate: '.coding-rules-detail-quality-profile-deactivate'
  },

  events: {
    'click @ui.change': 'change',
    'click @ui.revert': 'revert',
    'click @ui.deactivate': 'deactivate'
  },

  onRender() {
    this.$('[data-toggle="tooltip"]').tooltip({
      container: 'body'
    });
  },

  change() {
    const that = this;
    const activationView = new ProfileActivationView({
      model: this.model,
      collection: this.model.collection,
      rule: this.options.rule,
      app: this.options.app
    });
    activationView.on('profileActivated', () => {
      that.options.refreshActives();
    });
    activationView.render();
  },

  revert() {
    const that = this;
    const ruleKey = this.options.rule.get('key');
    confirmDialog({
      title: translate('coding_rules.revert_to_parent_definition'),
      html: translateWithParameters(
        'coding_rules.revert_to_parent_definition.confirm',
        this.getParent().name
      ),
      yesLabel: translate('yes'),
      noLabel: translate('cancel'),
      yesHandler() {
        return $.ajax({
          type: 'POST',
          url: window.baseUrl + '/api/qualityprofiles/activate_rule',
          data: {
            profile_key: that.model.get('qProfile'),
            rule_key: ruleKey,
            reset: true
          }
        }).done(() => {
          that.options.refreshActives();
        });
      }
    });
  },

  deactivate() {
    const that = this;
    const ruleKey = this.options.rule.get('key');
    confirmDialog({
      title: translate('coding_rules.deactivate'),
      html: translateWithParameters('coding_rules.deactivate.confirm'),
      yesLabel: translate('yes'),
      noLabel: translate('cancel'),
      yesHandler() {
        return $.ajax({
          type: 'POST',
          url: window.baseUrl + '/api/qualityprofiles/deactivate_rule',
          data: {
            profile_key: that.model.get('qProfile'),
            rule_key: ruleKey
          }
        }).done(() => {
          that.options.refreshActives();
        });
      }
    });
  },

  enableUpdate() {
    return this.ui.update.prop('disabled', false);
  },

  getParent() {
    if (!(this.model.get('inherit') && this.model.get('inherit') !== 'NONE')) {
      return null;
    }
    const myProfile = this.options.app.qualityProfiles.find(
      p => p.key === this.model.get('qProfile')
    );
    if (!myProfile) {
      return null;
    }
    const parentKey = myProfile.parentKey;
    const parent = { ...this.options.app.qualityProfiles.find(p => p.key === parentKey) };
    const parentActiveInfo =
      this.model.collection.findWhere({ qProfile: parentKey }) || new Backbone.Model();
    Object.assign(parent, parentActiveInfo.toJSON());
    return parent;
  },

  enhanceParameters(parent) {
    const params = sortBy(this.model.get('params'), 'key');
    if (!parent) {
      return params;
    }
    return params.map(p => {
      const parentParam = parent.params.find(param => param.key === p.key);
      if (parentParam != null) {
        return { ...p, original: parentParam.value };
      } else {
        return p;
      }
    });
  },

  getProfilePath(language, name) {
    const { organization } = this.options.app;
    const query = stringify({ language, name });
    return organization
      ? `${window.baseUrl}/organizations/${organization}/quality_profiles/show?${query}`
      : `${window.baseUrl}/profiles/show?${query}`;
  },

  serializeData() {
    const parent = this.getParent();

    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      parent,
      actions: this.model.get('actions') || {},
      canWrite: this.options.app.canWrite,
      parameters: this.enhanceParameters(parent),
      templateKey: this.options.rule.get('templateKey'),
      isTemplate: this.options.rule.get('isTemplate'),
      profilePath: this.getProfilePath(this.model.get('language'), this.model.get('name')),
      parentProfilePath: parent && this.getProfilePath(parent.language, parent.name)
    };
  }
});
