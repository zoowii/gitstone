$(function () {
    $(".transfer-ownership-btn").click(function () {
        var ownerName = $(".transfer-to-field").val();
        if (!ownerName || ownerName.length < 1) {
            alert("New owner name can't be empty");
            return;
        }
        alert('This feature is not supported yet now!');
    });
    $(".delete-repo-btn").click(function () {
        if (!window.confirm("Are you sure to delete this repository?")) {
            return;
        }
        $.post(window.delRepoUrl, {
        }, function (json) {
            if (!json.success) {
                alert(json.data);
                return;
            }
            alert('Delete successfully!');
            window.location.href = window.indexUrl;
        }, 'json');
    });
});