/*
 Functions used in resource viewers
 */

/* Source decoration functions */
function highlightUsages(event){
  var isAlreadyHighlighted = false;
  var selectedElementClasses = $j(this).attr('class').split(' ');
  if(selectedElementClasses.indexOf('highlighted') !== -1) {
    isAlreadyHighlighted = true;
  }
  $j('#' + event.data.id + ' span.highlighted').removeClass('highlighted');

  if(!isAlreadyHighlighted) {
    var selectedClass = selectedElementClasses[0];
    $j('#' + event.data.id + ' span.' + selectedClass).addClass('highlighted');
  }
}
