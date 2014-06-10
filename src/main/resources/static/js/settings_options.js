$(function () {
    var $form = $("[name=settings_options_form]");
    $form.find('.save-btn').click(function () {
        var description = $form.find('[name=description]').val();
        var defaultBranch = $form.find('[name=default_branch]').val();
        var isPrivate = $form.find('[name=is_private]').val() === 'private';
        $.post(updateSettingsOptionsUrl, {
            description: description,
            default_branch: defaultBranch,
            is_private: isPrivate
        }, function (json) {
            if (!json.success) {
                alert(json.data);
                return;
            }
            alert("Update settings options successfully!");
            window.location.reload();
        }, 'json');
    });
});