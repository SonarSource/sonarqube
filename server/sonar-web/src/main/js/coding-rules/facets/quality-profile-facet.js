define([
  'coding-rules/facets/base-facet',
  'templates/coding-rules'
], function (BaseFacet, Templates) {

  var $ = jQuery;

  return BaseFacet.extend({
    template: Templates['coding-rules-quality-profile-facet'],

    events: function () {
      return _.extend(BaseFacet.prototype.events.apply(this, arguments), {
        'click .js-active': 'setActivation',
        'click .js-inactive': 'unsetActivation'
      });
    },

    getValues: function () {
      var that = this,
          values = this.options.app.qualityProfiles.map(function (profile) {
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
      this.options.app.state.updateFilter({ activation: 'false' });
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

});
