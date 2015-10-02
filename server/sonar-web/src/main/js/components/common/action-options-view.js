import $ from 'jquery';
import PopupView from './popup';

export default PopupView.extend({
  className: 'bubble-popup bubble-popup-menu',
  keyScope: 'action-options',

  ui: {
    options: '.menu > li > a'
  },

  events: function () {
    return {
      'click @ui.options': 'selectOption',
      'mouseenter @ui.options': 'activateOptionByPointer'
    };
  },

  initialize: function () {
    this.bindShortcuts();
  },

  onRender: function () {
    PopupView.prototype.onRender.apply(this, arguments);
    this.selectInitialOption();
  },

  getOptions: function () {
    return this.$('.menu > li > a');
  },

  getActiveOption: function () {
    return this.getOptions().filter('.active');
  },

  makeActive: function (option) {
    if (option.length > 0) {
      this.getOptions().removeClass('active').tooltip('hide');
      option.addClass('active').tooltip('show');
    }
  },

  selectInitialOption: function () {
    this.makeActive(this.getOptions().first());
  },

  selectNextOption: function () {
    this.makeActive(this.getActiveOption().parent().nextAll('li:not(.divider)').first().children('a'));
    return false;
  },

  selectPreviousOption: function () {
    this.makeActive(this.getActiveOption().parent().prevAll('li:not(.divider)').first().children('a'));
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
      return that.destroy();
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

  onDestroy: function () {
    PopupView.prototype.onDestroy.apply(this, arguments);
    this.unbindShortcuts();
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
    $('.tooltip').remove();
  },

  selectOption: function (e) {
    e.preventDefault();
    this.destroy();
  },

  selectActiveOption: function () {
    this.getActiveOption().click();
  }
});


