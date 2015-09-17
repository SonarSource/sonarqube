import _ from 'underscore';
import Marionette from 'backbone.marionette';
import ProfileView from './rule-profile-view';
import ProfileActivationView from './profile-activation-view';
import '../templates';

  export default Marionette.CompositeView.extend({
    template: Templates['coding-rules-rule-profiles'],
    childView: ProfileView,
    childViewContainer: '#coding-rules-detail-quality-profiles',

    childViewOptions: function () {
      return {
        app: this.options.app,
        rule: this.model,
        refreshActives: this.refreshActives.bind(this)
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
      activationView.on('profileActivated', function (severity, params, profile) {
        if (that.options.app.state.get('query').qprofile === profile) {
          var activation = {
            severity: severity,
            inherit: 'NONE',
            params: params,
            qProfile: profile
          };
          that.model.set({ activation: activation });
        }
        that.refreshActives();
      });
      activationView.render();
    },

    refreshActives: function () {
      var that = this;
      this.options.app.controller.getRuleDetails(this.model).done(function (data) {
        that.collection.reset(that.model.getInactiveProfiles(data.actives, that.options.app.qualityProfiles));
      });
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite
      });
    }
  });


