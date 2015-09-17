import CustomMeasure from './custom-measure';
import FormView from './form-view';

export default FormView.extend({

  sendRequest: function () {
    var that = this,
        customMeasure = new CustomMeasure({
          metricId: this.$('#create-custom-measure-metric').val(),
          value: this.$('#create-custom-measure-value').val(),
          description: this.$('#create-custom-measure-description').val(),
          projectId: this.options.projectId
        });
    this.disableForm();
    return customMeasure.save(null, {
      statusCode: {
        // do not show global error
        400: null
      }
    }).done(function () {
      that.collection.refresh();
      that.destroy();
    }).fail(function (jqXHR) {
      that.enableForm();
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
    });
  }
});


