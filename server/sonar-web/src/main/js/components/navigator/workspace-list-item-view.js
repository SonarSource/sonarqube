define(function () {

  return Marionette.ItemView.extend({

    initialize: function (options) {
      this.listenTo(options.app.state, 'change:selectedIndex', this.select);
    },

    onRender: function () {
      this.select();
    },

    select: function () {
      var selected = this.model.get('index') === this.options.app.state.get('selectedIndex');
      this.$el.toggleClass('selected', selected);
    },

    selectCurrent: function () {
      this.options.app.state.set({ selectedIndex: this.model.get('index') });
    }

  });

});
