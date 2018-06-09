// -*- mode: js; js-indent-level: 2; -*-

'use strict';

// TO DO:
// 1. mouse click or arrow to a new position should reset the search string
// 2. grey out rather than disable
// 3. look at char, key and code rather than keyCode and which
 
   
function filterSelectResult()
{
  var search = '';
  return function(event)
  {
    var key = event.which || event.keyCode;
    
    /* 32 -> space, 0 -> 45, z -> 90, dash -> 189, underscore -> 189, period -> 190, backspace -> 8 */
    if (key == 32 || (key >=45 && key <= 90) || (key == 189) || (key == 190) || (key == 8))
    {
      if (key == 8)
      {
	if (search.length > 0)
	{
	  search = search.slice(0, -1)
	}
      }
      else
      {
	search += String.fromCharCode(key);
      }

      var si = event.target.length;
      for (var i = 0; i < event.target.length; i++)
      {
	if (event.target.options[i].label.toString().slice(0,(search.length)).toLowerCase() == search.toLowerCase())
	{
	  if (i < si)
	  {
	    si = i
	  }
	  event.target.options[i].disabled = false;
	}
	else
	{
	  event.target.options[i].disabled = true;
	}
      }
      event.target.selectedIndex = si;
    }

    event.returnValue=false;
    event.cancel = true;
  }
}

