import _ from 'underscore';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['quality-profile-comparison'],

  events: {
    'submit #quality-profile-comparison-form': 'onFormSubmit'
  },

  onRender: function () {
    this.$('select').select2({
      width: '250px',
      minimumResultsForSearch: 50
    });
  },

  onFormSubmit: function (e) {
    e.preventDefault();
    var withKey = this.$('#quality-profile-comparison-with-key').val();
    this.model.compareWith(withKey);
  },

  getProfilesForComparison: function () {
    var profiles = this.model.collection.toJSON(),
        key = this.model.id,
        language = this.model.get('language');
    return profiles.filter(function (profile) {
      return profile.language === language && key !== profile.key;
    });
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      profiles: this.getProfilesForComparison()
    });
  }
});


