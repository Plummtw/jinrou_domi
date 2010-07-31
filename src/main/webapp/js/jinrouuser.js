$(document).ready(function() {
  $('#copy_email2msn').click(function() {
    $('#msn').val($('#email').val());
  });

  $('#reload_captcha').click(function() {
    var img_src = $('#captcha').attr('src');
    var timestamp = new Date().getTime();
    $('#captcha').attr('src',img_src+'?'+timestamp);
  });
});

