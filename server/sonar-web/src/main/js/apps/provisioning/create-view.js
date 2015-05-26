define([
  './project',
  './form-view'
], function (Project, FormView) {

  return FormView.extend({

    sendRequest: function () {
      var that = this,
          project = new Project({
            name: this.$('#create-project-name').val(),
            branch: this.$('#create-project-branch').val(),
            key: this.$('#create-project-key').val()
          });
      this.disableForm();
      return project.save(null, {
        statusCode: {
          // do not show global error
          400: null
        }
      }).done(function () {
        that.collection.refresh();
        that.close();
      }).fail(function (jqXHR) {
        that.enableForm();
        that.showErrors([{ msg: jqXHR.responseJSON.err_msg }]);
      });
    }
  });

});
