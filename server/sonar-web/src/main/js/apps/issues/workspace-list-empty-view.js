import Marionette from 'backbone.marionette';

export default Marionette.ItemView.extend({
  className: 'search-navigator-no-results',

  template: function () {
    return t('issue_filter.no_issues');
  }
});


