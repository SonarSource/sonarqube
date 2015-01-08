define([
  'coding-rules/rule/rule-profile-view',
  'coding-rules/rule/profile-activation-view',
  'templates/coding-rules'
], function (ProfileView, ProfileActivationView) {

  return Marionette.CompositeView.extend({
    template: Templates['coding-rules-rule-profiles'],
    itemView: ProfileView,
    itemViewContainer: '#coding-rules-detail-quality-profiles',

    itemViewOptions: function () {
      return {
        app: this.options.app,
        rule: this.model
      };
    },

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click #coding-rules-quality-profile-activate': 'activate'
    },

    onRender: function () {
      var isManual = this.model.get('isManual'),
          qualityProfilesVisible = !isManual;

      if (qualityProfilesVisible) {
        if (this.model.get('isTemplate')) {
          qualityProfilesVisible = this.collection.length > 0;
        }
        else {
          qualityProfilesVisible = (this.options.app.canWrite || this.collection.length > 0);
        }
      }

      this.$el.toggleClass('hidden', !qualityProfilesVisible);
    },

    activate: function () {
      new ProfileActivationView({
        rule: this.model,
        collection: this.collection,
        app: this.options.app
      }).render();
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite
      });
    }
  });

});
