define(function () {

  var $ = jQuery;

  function createFrame () {
    var uuid = _.uniqueId('upload-form-');
    return $('<iframe></iframe>')
        .prop('frameborder', 0)
        .prop('width', 0)
        .prop('height', 0)
        .prop('id', uuid)
        .prop('name', uuid)
        .css('display', 'none');
  }

  return function (options) {
    var deferred = new $.Deferred(),
        body = $('body'),
        frame = createFrame(),
        parent = options.form.parent(),
        clonedForm = options.form.detach();

    clonedForm
        .prop('target', frame.prop('id'))
        .appendTo(frame);

    frame.appendTo(body);

    frame.on('load', function () {
      var result = this.contentWindow.document.body.textContent;
      try {
        var js = JSON.parse(result);
        deferred.resolve(js);
      } catch (e) {
        deferred.resolve(result);
      }
      clonedForm.detach().appendTo(parent);
      frame.off('load').remove();
    });

    clonedForm.submit();

    return deferred.promise();
  };

});
