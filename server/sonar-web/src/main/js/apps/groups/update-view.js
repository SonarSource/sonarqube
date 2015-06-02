define([
  './form-view'
], function (FormView) {

  return FormView.extend({

    sendRequest: function () {
      var that = this;
      this.model.set({
        name: this.$('#create-group-name').val(),
        description: this.$('#create-group-description').val()
      });
      this.disableForm();
      return this.model.save(null, {
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
