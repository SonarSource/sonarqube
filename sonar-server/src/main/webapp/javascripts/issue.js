function issueForm(actionType, elt) {
  var issueElt = $j(elt).closest('[data-issue-key]');
  var issueKey = issueElt.attr('data-issue-key');
  var actionsElt = issueElt.find('.code-issue-actions');
  var formElt = issueElt.find('.code-issue-form');

  actionsElt.addClass('hidden');
  formElt.html("<img src='" + baseUrl + "/images/loading-small.gif'>").removeClass('hidden');

  $j.ajax(baseUrl + "/issue/action_form/" + actionType + "?issue=" + issueKey)
    .done(function (msg) {
      formElt.html(msg);
      var focusField = formElt.find('[autofocus]');
      if (focusField != null) {
        focusField.focus();
      }
    })
    .fail(function (jqXHR, textStatus) {
      alert(textStatus);
    });
  return false;
}

function cancelIssueForm(elt) {
  var issueElt = $j(elt).closest('[data-issue-key]');
  var actionsElt = issueElt.find('.code-issue-actions');
  var formElt = issueElt.find('.code-issue-form');

  formElt.addClass('hidden');
  actionsElt.removeClass('hidden');
  return false;
}

function doIssueAction(elt) {
  var formElt = $j(elt).closest('form');
  formElt.find('.loading').removeClass('hidden');
  formElt.find(':submit').prop('disabled', true);
  $j.ajax({
      type: "POST",
      url: baseUrl + '/issue/do_action',
      data: formElt.serialize()}
  ).success(function (htmlResponse) {
      var issueElt = formElt.closest('[data-issue-key]');
      issueElt.html(htmlResponse);
      // re-enable the links opening modal popups
      issueElt.find('.open-modal').modal();
    }
  ).fail(function (jqXHR, textStatus) {
      cancelIssueForm(elt);
      alert(textStatus);
    });
  return false;
}

function assignIssueToMe(elt) {
  var issueElt = $j(elt).closest('[data-issue-key]');
  var issueKey = issueElt.attr('data-issue-key');
  $j.ajax({
      type: "POST",
      url: baseUrl + '/issue/do_action/assign?me=true&issue=' + issueKey
    }
  ).success(function (htmlResponse) {
      issueElt.html(htmlResponse);
      // re-enable the links opening modal popups
      issueElt.find('.open-modal').modal();
    }
  ).fail(function (jqXHR, textStatus) {
      cancelIssueForm(elt);
      alert(textStatus);
    });
  return false;
}

