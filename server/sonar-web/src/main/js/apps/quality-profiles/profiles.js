define([
  'backbone',
  './profile'
], function (Backbone, Profile) {

  return Backbone.Collection.extend({
    model: Profile,
    url: baseUrl + '/api/qualityprofiles/search',
    comparator: 'key',

    parse: function (r) {
      return r.profiles;
    },

    updateForLanguage: function (language) {
      this.fetch({
        data: {
          language: language
        },
        merge: true,
        reset: false,
        remove: false
      });
    }
  });

});
