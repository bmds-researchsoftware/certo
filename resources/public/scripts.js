// -*- mode: js; js-indent-level: 2; -*-

'use strict';
   
function filterSelectResult()
{
  var search = '';

  return function(event)
  {
    var key = event.which || event.keyCode;

    if (event.type == "keydown" && key != 8)
    {
      return;
    }
    // backspace -> 8
    else if (event.type == "keydown" && key == 8 && search.length > 0)
    {
      search = search.slice(0, -1);
    }
    else if (event.type == "keypress")
    {
      search += String.fromCharCode(key);
    }

    var si = event.target.length;

    var nbspRegExp = new RegExp(String.fromCharCode(160), "g");

    for (var i = 0; i < event.target.length; i++)
    {
      if (event.target.options[i].label.toString().replace(nbspRegExp,'').slice(0,(search.length)).toLowerCase() == search.toLowerCase())
      {
    	if (i <= si)
    	{
    	  si = i
    	}
    	event.target.options[i].classList.remove('dn');	
      }
      else
      {
	if (event.target.options[i].value != '')
	{
	  event.target.options[i].classList.add('dn');
	}
      }
    }

    if (si == event.target.length)
    {
      event.target.selectedIndex = 0;
    }
    else
    {
      event.target.selectedIndex = si;
    }
  }
}
