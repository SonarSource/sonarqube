define([
  './more-actions',
  './measures-overlay',
  './templates'
], function (MoreActionsView, MeasuresOverlay) {

  var $ = jQuery,
      API_FAVORITE = baseUrl + '/api/favourites';

  return Marionette.ItemView.extend({
    template: Templates['source-viewer-header'],

    events: function () {
      return {
        'click .js-favorite': 'toggleFavorite',
        'click .js-actions': 'showMoreActions',
        'click .js-permalink': 'getPermalink'
      };
    },

    toggleFavorite: function () {
      var that = this;
      if (this.model.get('fav')) {
        $.ajax({
          url: API_FAVORITE + '/' + this.model.get('key'),
          type: 'DELETE'
        }).done(function () {
          that.model.set('fav', false);
          that.render();
        });
      }
      else {
        $.ajax({
          url: API_FAVORITE,
          type: 'POST',
          data: {
            key: this.model.get('key')
          }
        }).done(function () {
          that.model.set('fav', true);
          that.render();
        });
      }
    },

    showMoreActions: function (e) {
      e.stopPropagation();
      $('body').click();
      var view = new MoreActionsView({ parent: this });
      view.render().$el.appendTo(this.$el);
    },

    getPermalink: function () {
      var query = 'id=' + encodeURIComponent(this.model.get('key')),
          windowParams = 'resizable=1,scrollbars=1,status=1';
      if (this.options.viewer.highlightedLine) {
        query = query + '&line=' + this.options.viewer.highlightedLine;
      }
      window.open(baseUrl + '/component/index?' + query, this.model.get('name'), windowParams);
    },

    showRawSources: function () {
      var url = baseUrl + '/api/sources/raw?key=' + encodeURIComponent(this.model.get('key')),
          windowParams = 'resizable=1,scrollbars=1,status=1';
      window.open(url, this.model.get('name'), windowParams);
    },

    showMeasures: function () {
      new MeasuresOverlay({
        model: this.model,
        large: true
      }).render();
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        path: this.model.get('path') || this.model.get('longName')
      });
    }
  });

});
