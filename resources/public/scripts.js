// -*- mode: js; js-indent-level: 2; -*-

'use strict';

function filterSelectResult(search_id, select_id)
{
  var selectMinWidthSet = false;

  return function(event)
  {
    var search = document.getElementById(search_id).value;
    var select = document.getElementById(select_id);
    if (!selectMinWidthSet)
    {
      select.style.minWidth = select.offsetWidth.toString() + "px";
      selectMinWidthSet = true;
    }
    var si = select.length;
    var nbspRegExp = new RegExp(String.fromCharCode(160)+'+', 'g');
    for (var i = 0; i < select.length; i++)
    {
      if (select.options[i].label.toString().replace(nbspRegExp,' ').toLowerCase().includes(search.toLowerCase()))
      {
      	select.options[i].classList.remove('dn');
    	if (i <= si)
      	{
      	  si = i;
      	  select.selectedIndex = si;
      	}
      }
      else
      {
      	if (select.options[i].label != '')
      	{
      	  select.options[i].classList.add('dn');
      	}
      }
    }
  }
}

