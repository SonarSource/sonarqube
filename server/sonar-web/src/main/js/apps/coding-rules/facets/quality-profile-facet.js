import $ from 'jquery';
import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['coding-rules-quality-profile-facet'],

  events: function () {
    return _.extend(BaseFacet.prototype.events.apply(this, arguments), {
      'click .js-active': 'setActivation',
      'click .js-inactive': 'unsetActivation'
    });
  },

  getValues: function () {
    var that = this,
        languagesQuery = this.options.app.state.get('query').languages,
        languages = languagesQuery != null ? languagesQuery.split(',') : [],
        lang = languages.length === 1 ? languages[0] : null,
        values = this.options.app.qualityProfiles
            .filter(function (profile) {
              return lang != null ? profile.lang === lang : true;
            })
            .map(function (profile) {
              return {
                label: profile.name,
                extra: that.options.app.languages[profile.lang],
                val: profile.key
              };
            });
    return _.sortBy(values, 'label');
  },

  toggleFacet: function (e) {
    var obj = {},
        property = this.model.get('property');
    if ($(e.currentTarget).is('.active')) {
      obj.activation = null;
      obj[property] = null;
    } else {
      obj.activation = true;
      obj[property] = $(e.currentTarget).data('value');
    }
    this.options.app.state.updateFilter(obj);
  },

  setActivation: function (e) {
    e.stopPropagation();
    this.options.app.state.updateFilter({ activation: 'true' });
  },

  unsetActivation: function (e) {
    e.stopPropagation();
    this.options.app.state.updateFilter({ activation: 'false', active_severities: null });
  },

  getToggled: function () {
    var activation = this.options.app.state.get('query').activation;
    return activation === 'true' || activation === true;
  },

  disable: function () {
    var obj = { activation: null },
        property = this.model.get('property');
    obj[property] = null;
    this.options.app.state.updateFilter(obj);
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      values: this.getValues(),
      toggled: this.getToggled()
    });
  }
});


