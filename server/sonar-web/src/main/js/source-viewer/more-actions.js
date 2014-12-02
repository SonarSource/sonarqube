define([
  'backbone.marionette',
  'templates/source-viewer'
], function (Marionette, Templates) {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    className: 'source-viewer-header-more-actions',
    template: Templates['source-viewer-more-actions'],

    events: {
      'click .js-measures': 'showMeasures',
      'click .js-new-window': 'openNewWindow',
      'click .js-raw-source': 'showRawSource'
    },

    onRender: function () {
      var that = this;
      $('body').on('click.component-viewer-more-actions', function () {
        $('body').off('click.component-viewer-more-actions');
        that.close();
      });
    },

    showMeasures: function () {
      this.options.parent.showMeasures();
    },

    openNewWindow: function () {
      this.options.parent.getPermalink();
    },

    showRawSource: function () {
      this.options.parent.showRawSources();
    }
  });

});
