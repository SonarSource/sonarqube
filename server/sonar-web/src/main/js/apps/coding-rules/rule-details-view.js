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
      './models/rules',
      './rule/rule-meta-view',
      './rule/rule-description-view',
      './rule/rule-parameters-view',
      './rule/rule-profiles-view',
      './rule/custom-rules-view',
      './rule/manual-rule-creation-view',
      './rule/custom-rule-creation-view',
      './rule/rule-issues-view',
      './templates'
    ],
    function (Rules,
              MetaView,
              DescView,
              ParamView,
              ProfilesView,
              CustomRulesView,
              ManualRuleCreationView,
              CustomRuleCreationView,
              IssuesView) {

      var $ = jQuery;

      return Marionette.Layout.extend({
        className: 'coding-rule-details',
        template: Templates['coding-rules-rule-details'],

        regions: {
          metaRegion: '.js-rule-meta',
          descRegion: '.js-rule-description',
          paramRegion: '.js-rule-parameters',
          profilesRegion: '.js-rule-profiles',
          customRulesRegion: '.js-rule-custom-rules',
          issuesRegion: '.js-rule-issues'
        },

        events: {
          'click .js-edit-manual': 'editManualRule',
          'click .js-edit-custom': 'editCustomRule',
          'click .js-delete': 'deleteRule'
        },

        initialize: function () {
          this.bindShortcuts();
          this.customRules = new Rules();
          if (this.model.get('isTemplate')) {
            this.fetchCustomRules();
          }
          this.listenTo(this.options.app.state, 'change:selectedIndex', this.select);
        },

        onRender: function () {
          this.metaRegion.show(new MetaView({
            app: this.options.app,
            model: this.model
          }));
          this.descRegion.show(new DescView({
            app: this.options.app,
            model: this.model
          }));
          this.paramRegion.show(new ParamView({
            app: this.options.app,
            model: this.model
          }));
          this.profilesRegion.show(new ProfilesView({
            app: this.options.app,
            model: this.model,
            collection: new Backbone.Collection(this.getQualityProfiles())
          }));
          this.customRulesRegion.show(new CustomRulesView({
            app: this.options.app,
            model: this.model,
            collection: this.customRules
          }));
          this.issuesRegion.show(new IssuesView({
            app: this.options.app,
            model: this.model
          }));
          this.$el.scrollParent().scrollTop(0);
        },

        onClose: function () {
          this.unbindShortcuts();
        },

        fetchCustomRules: function () {
          var that = this,
              url = baseUrl + '/api/rules/search',
              options = {
                template_key: this.model.get('key'),
                f: 'name,severity,params'
              };
          return $.get(url, options).done(function (data) {
            that.customRules.reset(data.rules);
          });
        },

        getQualityProfiles: function () {
          var that = this;
          return this.options.actives.map(function (profile) {
            var profileBase = _.findWhere(that.options.app.qualityProfiles, { key: profile.qProfile });
            if (profileBase != null) {
              _.extend(profile, profileBase);
            }
            return profile;
          });
        },

        bindShortcuts: function () {
          var that = this;
          key('up', 'details', function () {
            that.options.app.controller.selectPrev();
            return false;
          });
          key('down', 'details', function () {
            that.options.app.controller.selectNext();
            return false;
          });
          key('left, backspace', 'details', function () {
            that.options.app.controller.hideDetails();
            return false;
          });
        },

        unbindShortcuts: function () {
          key.deleteScope('details');
        },

        editManualRule: function () {
          new ManualRuleCreationView({
            app: this.options.app,
            model: this.model
          }).render();
        },

        editCustomRule: function () {
          new CustomRuleCreationView({
            app: this.options.app,
            model: this.model
          }).render();
        },

        deleteRule: function () {
          var that = this,
              ruleType = this.model.has('templateKey') ? 'custom' : 'manual';
          window.confirmDialog({
            title: t('delete'),
            html: tp('coding_rules.delete.' + ruleType + '.confirm', this.model.get('name')),
            yesHandler: function () {
              var url = baseUrl + '/api/rules/delete',
                  options = { key: that.model.id };
              $.post(url, options).done(function () {
                that.options.app.controller.fetchList();
              });
            }
          });
        },

        select: function () {
          var selected = this.options.app.state.get('selectedIndex'),
              selectedRule = this.options.app.list.at(selected);
          this.options.app.controller.showDetails(selectedRule);
        },

        serializeData: function () {
          var isManual = this.model.get('isManual'),
              isCustom = this.model.has('templateKey'),
              isEditable = this.options.app.canWrite && (isManual || isCustom),
              qualityProfilesVisible = !isManual;

          if (qualityProfilesVisible) {
            if (this.model.get('isTemplate')) {
              qualityProfilesVisible = !_.isEmpty(this.options.actives);
            }
            else {
              qualityProfilesVisible = (this.options.app.canWrite || !_.isEmpty(this.options.actives));
            }
          }

          return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
            isEditable: isEditable,
            canWrite: this.options.app.canWrite,
            qualityProfilesVisible: qualityProfilesVisible,
            allTags: _.union(this.model.get('sysTags'), this.model.get('tags'))
          });
        }
      });

    });
