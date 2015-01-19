define([
      'coding-rules/models/rules',
      'coding-rules/rule/rule-meta-view',
      'coding-rules/rule/rule-description-view',
      'coding-rules/rule/rule-parameters-view',
      'coding-rules/rule/rule-profiles-view',
      'coding-rules/rule/custom-rules-view',
      'coding-rules/rule/manual-rule-creation-view',
      'coding-rules/rule/rule-issues-view',
      'templates/coding-rules'
    ],
    function (Rules,
              MetaView,
              DescView,
              ParamView,
              ProfilesView,
              CustomRulesView,
              ManualRuleCreationView,
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
          'click .js-delete': 'deleteRule'
        },

        initialize: function () {
          this.bindShortcuts();
          this.customRules = new Rules();
          if (this.model.get('isTemplate')) {
            this.fetchCustomRules();
          }
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
            that.options.app.controller.showDetailsForSelected();
            return false;
          });
          key('down', 'details', function () {
            that.options.app.controller.selectNext();
            that.options.app.controller.showDetailsForSelected();
            return false;
          });
          key('left', 'details', function () {
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

        deleteRule: function () {
          var that = this,
              ruleType = this.model.has('templateKey') ? 'custom' : 'manual';
          window.confirmDialog({
            title: t('delete'),
            html: tp('coding_rules.delete.' + ruleType + '.confirm', this.model.get('name')),
            yesHandler: function () {
              var p = window.process.addBackgroundProcess(),
                  url = baseUrl + '/api/rules/delete',
                  options = { key: that.model.id };
              $.post(url, options).done(function () {
                that.options.app.controller.fetchList();
                window.process.finishBackgroundProcess(p);
              }).fail(function () {
                window.process.failBackgroundProcess(p);
              });
            }
          });
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
            subCharacteristic: this.options.app.getSubCharacteristicName(this.model.get('debtSubChar')),
            allTags: _.union(this.model.get('sysTags'), this.model.get('tags'))
          });
        }
      });

    });
