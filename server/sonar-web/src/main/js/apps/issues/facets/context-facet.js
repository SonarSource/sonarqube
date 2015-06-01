define([
  './base-facet',
  '../templates'
], function (BaseFacet) {

  return BaseFacet.extend({
    template: Templates['issues-context-facet'],

    serializeData: function () {
      return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
        state: this.options.app.state.toJSON()
      });
    }
  });

});
