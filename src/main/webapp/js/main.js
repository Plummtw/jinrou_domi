$(document).ready(function() {

  $(function() {
	  $('#tabs').tabs({
  		ajaxOptions: {
  			error: function(xhr, status, index, anchor) {
  				$(anchor.hash).html('頁面讀取失敗');
  			}
  		}
  	});
  });


  /*
  $('#login_button').click(function() {
    $('#login_form').submit();
  	return false;
  });
  */
});  

