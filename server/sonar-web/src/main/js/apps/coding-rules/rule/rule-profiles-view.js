define([
  'backbone.marionette',
  './rule-profile-view',
  './profile-activation-view',
  '../templates'
], function (Marionette, ProfileView, ProfileActivationView) {

  return Marionette.CompositeView.extend({
    template: Templates['coding-rules-rule-profiles'],
    childView: ProfileView,
    childViewContainer: '#coding-rules-detail-quality-profiles',

    childViewOptions: function () {
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
      var that = this,
          activationView = new ProfileActivationView({
            rule: this.model,
            collection: this.collection,
            app: this.options.app
          });
      activationView.on('profileActivated', function () {
        that.options.app.controller.showDetails(that.model);
      });
      activationView.render();
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite
      });
    }
  });

});
