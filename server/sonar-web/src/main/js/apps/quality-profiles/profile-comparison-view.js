import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Template from './templates/quality-profile-comparison.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'submit #quality-profile-comparison-form': 'onFormSubmit',
    'click .js-hide-comparison': 'onHideComparisonClick'
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

  onHideComparisonClick: function (e) {
    e.preventDefault();
    this.model.resetComparison();
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


