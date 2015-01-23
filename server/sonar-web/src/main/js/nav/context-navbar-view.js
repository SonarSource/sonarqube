define([
  'templates/nav'
], function () {

  return Marionette.ItemView.extend({
    template: Templates['nav-context-navbar'],

    onRender: function () {
      this.$('[data-toggle="tooltip"]').tooltip({
        container: 'body'
      });
    }
  });

});
