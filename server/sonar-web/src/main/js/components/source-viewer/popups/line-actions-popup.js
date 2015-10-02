import Popup from '../../common/popup';
import ManualIssueView from '../../issue/manual-issue-view';
import Template from '../templates/source-viewer-line-options-popup.hbs';

export default Popup.extend({
  template: Template,

  events: {
    'click .js-get-permalink': 'getPermalink',
    'click .js-add-manual-issue': 'addManualIssue'
  },

  getPermalink: function (e) {
    e.preventDefault();
    var url = baseUrl + '/component/index?id=' +
            (encodeURIComponent(this.model.key())) + '&line=' + this.options.line,
        windowParams = 'resizable=1,scrollbars=1,status=1';
    window.open(url, this.model.get('name'), windowParams);
  },

  addManualIssue: function (e) {
    e.preventDefault();
    var that = this,
        line = this.options.line,
        component = this.model.key(),
        manualIssueView = new ManualIssueView({
          line: line,
          component: component
        });
    manualIssueView.render().$el.appendTo(this.options.row.find('.source-line-code'));
    manualIssueView.on('add', function (issue) {
      that.trigger('onManualIssueAdded', issue);
    });
  }
});


