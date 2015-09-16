define(['./item'], function (Item) {

  var STORAGE_KEY = 'sonarqube-workspace';

  return Backbone.Collection.extend({
    model: Item,

    initialize: function () {
      this.on('remove', this.save);
    },

    save: function () {
      var dump = JSON.stringify(this.toJSON());
      window.localStorage.setItem(STORAGE_KEY, dump);
    },

    load: function () {
      var dump = window.localStorage.getItem(STORAGE_KEY);
      if (dump != null) {
        try {
          var parsed = JSON.parse(dump);
          this.reset(parsed);
        } catch (err) {
          // fail silently
        }
      }
    },

    has: function (model) {
      var forComponent = model.isComponent() && this.findWhere({ uuid: model.get('uuid') }) != null,
          forRule = model.isRule() && this.findWhere({ key: model.get('key') }) != null;
      return forComponent || forRule;
    },

    add2: function (model) {
      var tryModel = model.isComponent() ?
          this.findWhere({ uuid: model.get('uuid') }) :
          this.findWhere({ key: model.get('key') });
      return tryModel != null ? tryModel : this.add(model);
    }
  });

});
