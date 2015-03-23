define(function () {

  return Backbone.Model.extend({
    defaults: function () {
      return {
        qualityGateStatus: 'ERROR'
      };
    }
  });

});
