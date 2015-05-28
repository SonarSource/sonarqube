define([
  './templates'
], function () {

  var $ = jQuery,
      API_ISSUE = baseUrl + '/api/issues/show',
      API_ADD_MANUAL_ISSUE = baseUrl + '/api/issues/create';

  return Marionette.ItemView.extend({
    template: Templates['manual-issue'],

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

    onClose: function () {
      if (key != null && this.key != null) {
        key.setScope(this.key);
      }
    },

    showSpinner: function () {
      this.$('.js-submit').hide();
      this.$('.js-spinner').show();
    },

    hideSpinner: function () {
      this.$('.js-submit').show();
      this.$('.js-spinner').hide();
    },

    validateFields: function () {
      var message = this.$('[name=message]');
      if (!message.val()) {
        message.addClass('invalid').focus();
        return false;
      }
      return true;
    },

    formSubmit: function (e) {
      var that = this;
      e.preventDefault();
      if (!this.validateFields()) {
        return;
      }
      this.showSpinner();
      var data = $(e.currentTarget).serialize();
      $.post(API_ADD_MANUAL_ISSUE, data)
          .done(function (r) {
            if (typeof r === 'string') {
              r = JSON.parse(r);
            }
            that.addIssue(r.issue.key);
          }).fail(function (r) {
            that.hideSpinner();
            if (r.responseJSON && r.responseJSON.errors) {
              that.showError(_.pluck(r.responseJSON.errors, 'msg').join('. '));
            }
          });
    },

    addIssue: function (key) {
      var that = this;
      return $.get(API_ISSUE, { key: key }).done(function (r) {
        that.trigger('add', r.issue);
        that.close();
      });
    },

    showError: function (msg) {
      this.$('.code-issue-errors').removeClass('hidden').text(msg);
    },

    cancel: function (e) {
      e.preventDefault();
      this.close();
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        line: this.options.line,
        component: this.options.component,
        rules: _.sortBy(this.rules, 'name')
      });
    }
  });

});
