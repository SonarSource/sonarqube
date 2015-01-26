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
          p = window.process.addBackgroundProcess(),
          url = baseUrl + '/favourites/toggle/' + this.model.get('contextId'),
          isContextFavorite = this.model.get('isContextFavorite');
      this.model.set({ isContextFavorite: !isContextFavorite });
      return $.post(url).done(function () {
        window.process.finishBackgroundProcess(p);
      }).fail(function () {
        that.model.set({ isContextFavorite: isContextFavorite });
        window.process.failBackgroundProcess(p);
      });
    },

    serializeData: function () {
      return _.extend(Marionette.Layout.prototype.serializeData.apply(this, arguments), {
        canManageContextDashboards: window.SS.user != null
      });
    }
  });

});
