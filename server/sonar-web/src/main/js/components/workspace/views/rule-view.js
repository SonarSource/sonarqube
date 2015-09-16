define([
  './base-viewer-view',
  '../templates'
], function (BaseView) {

  return BaseView.extend({
    template: Templates['workspace-rule'],

    onRender: function () {
      BaseView.prototype.onRender.apply(this, arguments);
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
    },

    onDestroy: function () {
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    serializeData: function () {
     return _.extend(Marionette.LayoutView.prototype.serializeData.apply(this, arguments), {
       allTags: _.union(this.model.get('sysTags'), this.model.get('tags'))
     });
    }
  });

});
