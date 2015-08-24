define([
  'backbone.marionette'
], function (Marionette) {

  return Marionette.ItemView.extend({
    className: 'search-navigator-no-results',

    template: function () {
      return window.t('issue_filter.no_issues');
    }
  });

});
