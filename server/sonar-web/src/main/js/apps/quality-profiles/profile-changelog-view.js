import Marionette from 'backbone.marionette';
import Template from './templates/quality-profile-changelog.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'submit #quality-profile-changelog-form': 'onFormSubmit',
    'click .js-show-more-changelog': 'onShowMoreChangelogClick',
    'click .js-hide-changelog': 'onHideChangelogClick'
  },

  onFormSubmit: function (e) {
    e.preventDefault();
    this.model.fetchChangelog(this.getSearchParameters());
  },

  onShowMoreChangelogClick: function (e) {
    e.preventDefault();
    this.model.fetchMoreChangelog();
  },

  onHideChangelogClick: function (e) {
    e.preventDefault();
    this.model.resetChangelog();
  },

  getSearchParameters: function () {
    var form = this.$('#quality-profile-changelog-form');
    return {
      since: form.find('[name="since"]').val(),
      to: form.find('[name="to"]').val()
    };
  }
});


