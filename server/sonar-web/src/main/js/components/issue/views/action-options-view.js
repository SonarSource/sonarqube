define([
  'components/common/popup'
], function (PopupView) {

  var $ = jQuery;

  return PopupView.extend({
    keyScope: 'issue-action-options',

    ui: {
      options: '.issue-action-option'
    },

    events: function () {
      return {
        'click .issue-action-option': 'selectOption',
        'mouseenter .issue-action-option': 'activateOptionByPointer'
      };
    },

    initialize: function () {
      this.bindShortcuts();
    },

    onRender: function () {
      PopupView.prototype.onRender.apply(this, arguments);
      this.selectInitialOption();
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
    },

    getOptions: function () {
      return this.$('.issue-action-option');
    },

    getActiveOption: function () {
      return this.getOptions().filter('.active');
    },

    makeActive: function (option) {
      if (option.length > 0) {
        this.getOptions().removeClass('active');
        option.addClass('active');
      }
    },

    selectInitialOption: function () {
      this.makeActive(this.getOptions().first());
    },

    selectNextOption: function () {
      this.makeActive(this.getActiveOption().nextAll('.issue-action-option').first());
      return false;
    },

    selectPreviousOption: function () {
      this.makeActive(this.getActiveOption().prevAll('.issue-action-option').first());
      return false;
    },

    activateOptionByPointer: function (e) {
      this.makeActive($(e.currentTarget));
    },

    bindShortcuts: function () {
      var that = this;
      this.currentKeyScope = key.getScope();
      key.setScope(this.keyScope);
      key('down', this.keyScope, function () {
        return that.selectNextOption();
      });
      key('up', this.keyScope, function () {
        return that.selectPreviousOption();
      });
      key('return', this.keyScope, function () {
        return that.selectActiveOption();
      });
      key('escape', this.keyScope, function () {
        return that.close();
      });
      key('backspace', this.keyScope, function () {
        return false;
      });
      key('shift+tab', this.keyScope, function () {
        return false;
      });
    },

    unbindShortcuts: function () {
      key.unbind('down', this.keyScope);
      key.unbind('up', this.keyScope);
      key.unbind('return', this.keyScope);
      key.unbind('escape', this.keyScope);
      key.unbind('backspace', this.keyScope);
      key.unbind('tab', this.keyScope);
      key.unbind('shift+tab', this.keyScope);
      key.setScope(this.currentKeyScope);
    },

    onClose: function () {
      PopupView.prototype.onClose.apply(this, arguments);
      this.unbindShortcuts();
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
      $('.tooltip').remove();
    },

    selectOption: function (e) {
      e.preventDefault();
      this.close();
    },

    selectActiveOption: function () {
      this.getActiveOption().click();
    }
  });

});
