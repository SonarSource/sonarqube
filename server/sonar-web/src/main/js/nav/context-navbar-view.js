define([
  'templates/nav'
], function () {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    template: Templates['nav-context-navbar'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click .js-favorite': 'onFavoriteClick'
    },

    onRender: function () {
      this.$('[data-toggle="tooltip"]').tooltip({
        container: 'body'
      });
    },

    onFavoriteClick: function () {
      var that = this,
          url = baseUrl + '/favourites/toggle/' + this.model.get('contextId'),
          isContextFavorite = this.model.get('isContextFavorite');
      this.model.set({ isContextFavorite: !isContextFavorite });
      return $.post(url).fail(function () {
        that.model.set({ isContextFavorite: isContextFavorite });
      });
    },

    serializeData: function () {
      return _.extend(Marionette.Layout.prototype.serializeData.apply(this, arguments), {
        canManageContextDashboards: window.SS.user != null
      });
    }
  });

});
