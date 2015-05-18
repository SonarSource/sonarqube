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
  './profile-activation-view',
  '../templates'
], function (ProfileActivationView) {

  return Marionette.ItemView.extend({
    tagName: 'tr',
    template: Templates['coding-rules-rule-profile'],

    modelEvents: {
      'change': 'render'
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

    onRender: function () {
      this.$('[data-toggle="tooltip"]').tooltip({
        container: 'body'
      });
    },

    change: function () {
      var that = this,
          activationView = new ProfileActivationView({
            model: this.model,
            collection: this.model.collection,
            rule: this.options.rule,
            app: this.options.app
          });
      activationView.on('profileActivated', function () {
        that.options.app.controller.showDetails(that.options.rule);
      });
      activationView.render();
    },

    revert: function () {
      var that = this,
          ruleKey = this.options.rule.get('key');
      window.confirmDialog({
        title: t('coding_rules.revert_to_parent_definition'),
        html: tp('coding_rules.revert_to_parent_definition.confirm', this.getParent().name),
        yesHandler: function () {
          return jQuery.ajax({
            type: 'POST',
            url: baseUrl + '/api/qualityprofiles/activate_rule',
            data: {
              profile_key: that.model.get('qProfile'),
              rule_key: ruleKey,
              reset: true
            }
          }).done(function () {
            that.options.app.controller.showDetails(that.options.rule);
          });
        }
      });
    },

    deactivate: function () {
      var that = this,
          ruleKey = this.options.rule.get('key');
      window.confirmDialog({
        title: t('coding_rules.deactivate'),
        html: tp('coding_rules.deactivate.confirm'),
        yesHandler: function () {
          return jQuery.ajax({
            type: 'POST',
            url: baseUrl + '/api/qualityprofiles/deactivate_rule',
            data: {
              profile_key: that.model.get('qProfile'),
              rule_key: ruleKey
            }
          }).done(function () {
            that.options.app.controller.showDetails(that.options.rule);
          });
        }
      });
    },

    enableUpdate: function () {
      return this.ui.update.prop('disabled', false);
    },

    getParent: function () {
      if (!(this.model.get('inherit') && this.model.get('inherit') !== 'NONE')) {
        return null;
      }
      var myProfile = _.findWhere(this.options.app.qualityProfiles, {
            key: this.model.get('qProfile')
          }),
          parentKey = myProfile.parentKey,
          parent = _.extend({}, _.findWhere(this.options.app.qualityProfiles, {
            key: parentKey
          })),
          parentActiveInfo = this.model.collection.findWhere({ qProfile: parentKey }) || new Backbone.Model();
      _.extend(parent, parentActiveInfo.toJSON());
      return parent;
    },

    enhanceParameters: function () {
      var parent = this.getParent(),
          params = _.sortBy(this.model.get('params'), 'key');
      if (!parent) {
        return params;
      }
      return params.map(function (p) {
        var parentParam = _.findWhere(parent.params, { key: p.key });
        if (parentParam != null) {
          return _.extend(p, {
            original: _.findWhere(parent.params, { key: p.key }).value
          });
        } else {
          return p;
        }
      });
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite,
        parent: this.getParent(),
        parameters: this.enhanceParameters(),
        templateKey: this.options.rule.get('templateKey'),
        isTemplate: this.options.rule.get('isTemplate')
      });
    }
  });

});
