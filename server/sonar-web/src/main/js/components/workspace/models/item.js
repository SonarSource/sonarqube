define(function () {

  return Backbone.Model.extend({

    validate: function () {
      if (!this.has('type')) {
        return 'type is missing';
      }
      if (this.get('type') === 'component' && !this.has('uuid')) {
        return 'uuid is missing';
      }
      if (this.get('type') === 'rule' && !this.has('key')) {
        return 'key is missing';
      }
    },

    isComponent: function () {
      return this.get('type') === 'component';
    },

    isRule: function () {
      return this.get('type') === 'rule';
    },

    destroy: function (options) {
      this.stopListening();
      this.trigger('destroy', this, this.collection, options);
    }
  });

});
