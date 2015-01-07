define(function () {

  return Marionette.CollectionView.extend({

    initialize: function () {
      this.resetSelectedIndex();
      this.listenTo(this.collection, 'reset', this.resetSelectedIndex);
    },

    resetSelectedIndex: function () {
      this.selectedIndex = 0;
    },

    onRender: function () {
      this.selectCurrent();
    },

    submitCurrent: function () {
      var view = this.children.findByIndex(this.selectedIndex);
      if (view != null) {
        view.submit();
      }
    },

    selectCurrent: function () {
      this.selectItem(this.selectedIndex);
    },

    selectNext: function () {
      if (this.selectedIndex < this.collection.length - 1) {
        this.deselectItem(this.selectedIndex);
        this.selectedIndex++;
        this.selectItem(this.selectedIndex);
      }
    },

    selectPrev: function () {
      if (this.selectedIndex > 0) {
        this.deselectItem(this.selectedIndex);
        this.selectedIndex--;
        this.selectItem(this.selectedIndex);
      }
    },

    selectItem: function (index) {
      if (index >= 0 && index < this.collection.length) {
        var view = this.children.findByIndex(index);
        if (view != null) {
          view.select();
        }
      }
    },

    deselectItem: function (index) {
      var view = this.children.findByIndex(index);
      if (view != null) {
        view.deselect();
      }
    }
  });

});
