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

function closeIssueForm(elt) {
  var issueElt = $j(elt).closest('[data-issue-key]');
  var actionsElt = issueElt.find('.code-issue-actions');
  var formElt = issueElt.find('.code-issue-form');

  formElt.addClass('hidden');
  actionsElt.removeClass('hidden');
  return false;
}

function postIssueForm(elt) {
  var formElt = $j(elt).closest('form');
  formElt.find('.loading').removeClass('hidden');
  formElt.find(':submit').prop('disabled', true);
  $j.ajax({
      type: "POST",
      url: baseUrl + '/issue/do_action',
      data: formElt.serialize()}
  ).success(function (htmlResponse) {
      var issueElt = formElt.closest('[data-issue-key]');
      var replaced = $j(htmlResponse);
      issueElt.replaceWith(replaced);

      // re-enable the links opening modal popups
      replaced.find('.open-modal').modal();
    }
  ).fail(function (jqXHR, textStatus) {
      closeIssueForm(elt);
      alert(textStatus);
    });
  return false;
}

function doIssueAction(elt, action, parameters) {
  var issueElt = $j(elt).closest('[data-issue-key]');
  var issueKey = issueElt.attr('data-issue-key');
  parameters['issue']=issueKey;

  $j.ajax({
      type: "POST",
      url: baseUrl + '/issue/do_action/' + action,
      data: parameters
    }
  ).success(function (htmlResponse) {
      var replaced = $j(htmlResponse);
      issueElt.replaceWith(replaced);
      // re-enable the links opening modal popups
      replaced.find('.open-modal').modal();
    }
  ).fail(function (jqXHR, textStatus) {
      closeIssueForm(elt);
      alert(textStatus);
    });
  return false;
}

function assignIssueToMe(elt) {
  var parameters = {'me': true};
  return doIssueAction(elt, 'assign', parameters)
}

function doIssueTransition(elt, transition) {
  var parameters = {'transition': transition};
  return doIssueAction(elt, 'transition', parameters)
}

