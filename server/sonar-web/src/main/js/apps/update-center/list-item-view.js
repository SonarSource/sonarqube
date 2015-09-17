import $ from 'jquery';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import PluginChangelogView from './plugin-changelog-view';
import './templates';

export default Marionette.ItemView.extend({
  tagName: 'li',
  className: 'panel panel-vertical',
  template: Templates['update-center-plugin'],
  systemTemplate: Templates['update-center-system-update'],

  modelEvents: {
    'change:_hidden': 'toggleDisplay',
    'change': 'onModelChange',
    'request': 'onRequest'
  },

  events: {
    'click .js-changelog': 'onChangelogClick',
    'click .js-install': 'install',
    'click .js-update': 'update',
    'click .js-uninstall': 'uninstall',
    'change .js-terms': 'onTermsChange',
    'click .js-plugin-category': 'onCategoryClick'
  },

  getTemplate: function () {
    return this.model.get('_system') ? this.systemTemplate : this.template;
  },

  onRender: function () {
    this.$el.attr('data-id', this.model.id);
    if (this.model.get('_system')) {
      this.$el.attr('data-system', '');
    }
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy: function () {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onModelChange: function () {
    if (!this.model.hasChanged('_hidden')) {
      this.render();
    }
  },

  onChangelogClick: function (e) {
    e.preventDefault();
    e.stopPropagation();
    $('body').click();
    var index = $(e.currentTarget).data('idx'),
    // if show changelog of update, show details of this update
    // otherwise show changelog of the available release
        update = this.model.has('release') ? this.model.toJSON() : this.model.get('updates')[index],
        popup = new PluginChangelogView({
          triggerEl: $(e.currentTarget),
          model: new Backbone.Model(update)
        });
    popup.render();
  },

  onRequest: function () {
    this.$('.js-actions').addClass('hidden');
    this.$('.js-spinner').removeClass('hidden');
  },

  toggleDisplay: function () {
    this.$el.toggleClass('hidden', this.model.get('_hidden'));
  },

  install: function () {
    this.model.install();
  },

  update: function () {
    this.model.update();
  },

  uninstall: function () {
    this.model.uninstall();
  },

  onTermsChange: function () {
    var isAccepted = this.$('.js-terms').is(':checked');
    this.$('.js-install').prop('disabled', !isAccepted);
  },

  onCategoryClick: function (e) {
    e.preventDefault();
    this.model.trigger('filter', this.model);
  }
});


