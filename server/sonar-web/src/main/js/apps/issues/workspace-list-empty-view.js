import Marionette from 'backbone.marionette';

export default Marionette.ItemView.extend({
  className: 'search-navigator-no-results',

  template: function () {
    return window.t('issue_filter.no_issues');
  }
});
