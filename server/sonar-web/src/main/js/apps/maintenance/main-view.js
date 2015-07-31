define([
  './templates'
], function () {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    template: Templates['maintenance-main'],

    events: {
      'click #start-migration': 'startMigration'
    },

    initialize: function () {
      var that = this;
      this.requestOptions = {
        type: 'GET',
        url: baseUrl + '/api/system/' + (this.options.setup ? 'db_migration_status' : 'status')
      };
      setInterval(function () {
        that.refresh();
      }, 5000);
    },

    refresh: function () {
      var that = this;
      return Backbone.ajax(this.requestOptions).done(function (r) {
        that.model.set(r);
        that.render();
      });
    },

    startMigration: function () {
      var that = this;
      Backbone.ajax({
        url: baseUrl + '/api/system/migrate_db',
        type: 'POST'
      }).done(function (r) {
        that.model.set(r);
        that.render();
      })
    },

    onRender: function () {
      $('.page-simple').toggleClass('panel-warning', this.model.get('state') === 'MIGRATION_REQUIRED');
    },

    serializeData: function () {
      return _.extend(this._super(), { setup: this.options.setup });
    }
  });

});
