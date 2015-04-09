/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
