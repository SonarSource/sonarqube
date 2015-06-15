define([
  './templates'
], function () {

  return Marionette.ItemView.extend({
    template: Templates['maintenance-main'],

    events: {
      'click #start-migration': 'onStartMigrationClick'
    },

    initialize: function () {
      this.requestOptions = { type: 'GET', url: baseUrl + '/api/system/status' };
      setInterval(function () {
        this.refresh();
      }.bind(this), 5000);
    },

    refresh: function () {
      return Backbone.ajax(this.requestOptions).done(function (r) {
        if (r.status === 'DB_MIGRATION_RUNNING' && this.options.setup) {
          // we are at setup page and migration is running
          // so, let's switch to the migration status WS
          return this.startMigration();
        }
        this.model.set(r);
        this.render();
      }.bind(this));
    },

    onStartMigrationClick: function (e) {
      e.preventDefault();
      this.startMigration();
    },

    startMigration: function () {
      this.requestOptions = { type: 'POST', url: baseUrl + '/api/system/migrate_db' };
      return this.refresh();
    },

    serializeData: function () {
      return _.extend(this._super(), { setup: this.options.setup });
    }
  });

});
