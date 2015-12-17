import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Issue from './models/issue';
import Template from './templates/manual-issue.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'submit .js-manual-issue-form': 'formSubmit',
    'click .js-cancel': 'cancel'
  },

  initialize: function () {
    var that = this;
    this.rules = [];
    $.get(baseUrl + '/api/rules/search?repositories=manual&f=name&ps=9999999').done(function (r) {
      that.rules = r.rules;
      that.render();
    });
  },

  onRender: function () {
    this.delegateEvents();
    this.$('[name=rule]').select2({
      width: '250px',
      minimumResultsForSearch: 10
    });
    if (this.rules.length > 0) {
      this.$('[name=rule]').select2('open');
    }
    if (key != null) {
      this.key = key.getScope();
      key.setScope('');
    }
  },

  onDestroy: function () {
    if (key != null && this.key != null) {
      key.setScope(this.key);
    }
  },

  formSubmit: function (e) {
    var that = this;
    e.preventDefault();
    var issue = new Issue({
      component: this.options.component,
      line: this.options.line,
      message: this.$('[name="message"]').val(),
      rule: this.$('[name="rule"]').val()
    });
    issue.save().done(function () {
      that.addIssue(issue);
    });
  },

  addIssue: function (issue) {
    var that = this;
    return issue.fetch().done(function () {
      that.trigger('add', issue);
      that.destroy();
    });
  },

  cancel: function (e) {
    e.preventDefault();
    this.destroy();
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      rules: _.sortBy(this.rules, 'name')
    });
  }
});


