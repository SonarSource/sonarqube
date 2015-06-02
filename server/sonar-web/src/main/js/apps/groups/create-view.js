define([
  './group',
  './form-view'
], function (Group, FormView) {

  return FormView.extend({

    sendRequest: function () {
      var that = this,
          group = new Group({
            name: this.$('#create-group-name').val(),
            description: this.$('#create-group-description').val()
          });
      this.disableForm();
      return group.save(null, {
        statusCode: {
          // do not show global error
          400: null
        }
      }).done(function () {
        that.collection.refresh();
        that.close();
      }).fail(function (jqXHR) {
        that.enableForm();
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      });
    }
  });

});
