define([
  'templates/nav'
], function () {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    template: Templates['nav-context-navbar'],

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        breadcrumbs: this.options.breadcrumbs.toJSON()
      });
    }
  });

});
