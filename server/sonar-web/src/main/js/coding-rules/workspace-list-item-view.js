define([
  'components/navigator/workspace-list-item-view',
  'coding-rules/rule/profile-activation-view',
  'templates/coding-rules'
], function (WorkspaceListItemView, ProfileActivationView) {

  return WorkspaceListItemView.extend({
    className: 'coding-rule',
    template: Templates['coding-rules-workspace-list-item'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click': 'selectCurrent',
      'click .js-rule': 'openRule',
      'click .coding-rules-detail-quality-profile-activate': 'activate',
      'click .coding-rules-detail-quality-profile-deactivate': 'deactivate'
    },

    selectCurrent: function () {
      this.options.app.state.set({ selectedIndex: this.model.get('index') });
    },

    openRule: function () {
      this.options.app.controller.showDetails(this.model);
    },

    getActiveProfile: function () {
      return this.model.get('activeProfile');
    },

    activate: function () {
      var activeProfile = this.options.app.state.get('query').qprofile,
          othersQualityProfiles = _.reject(this.options.app.qualityProfiles, function (profile) {
            return profile.key === activeProfile;
          });

      new ProfileActivationView({
        rule: this.model,
        collection: new Backbone.Collection(othersQualityProfiles),
        app: this.options.app,
        fromList: true
      }).render();
    },

    deactivate: function () {
      var that = this,
          ruleKey = this.model.get('key'),
          myProfile = this.model.get('activeProfile');
      window.confirmDialog({
        title: t('coding_rules.deactivate'),
        html: tp('coding_rules.deactivate.confirm'),
        yesHandler: function () {
          var p = window.process.addBackgroundProcess();
          return jQuery.ajax({
            type: 'POST',
            url: baseUrl + '/api/qualityprofiles/deactivate_rule',
            data: {
              profile_key: myProfile.qProfile,
              rule_key: ruleKey
            }
          }).done(function () {
            window.process.finishBackgroundProcess(p);
            var newProfile = _.extend({}, myProfile, { severity: undefined });
            that.model.set({ activeProfile: newProfile });
          });
        }
      });
    },

    serializeData: function () {
      var activeProfile = this.getActiveProfile(),
          selectedProfile = this.options.app.state.get('query').qprofile;
      if (selectedProfile != null && activeProfile == null) {
        activeProfile = selectedProfile;
      }
      return _.extend(WorkspaceListItemView.prototype.serializeData.apply(this, arguments), {
        tags: _.union(this.model.get('sysTags'), this.model.get('tags')),
        canWrite: this.options.app.canWrite,
        activeProfile: activeProfile
      });
    }
  });

});
