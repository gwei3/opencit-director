var imageFormats = new Array();
var image_policies = new Array();

$.ajax({
    type: "GET",
    url: "/v1/trust-policy-drafts/?imageId=" + current_image_id + "&imageArchive=false",
    contentType: "application/json",
    headers: {
        'Accept': 'application/json'
    },
    dataType: "json",
    success: function(data, status, xhr) {
        showImageLaunchPoliciesDocker(data);
    }
});

function EditDockerImageMetaData(data) {

    this.image_id = current_image_id;
    this.image_name = current_image_name;
    this.display_name = current_display_name;

}

function EditDockerImageViewModel(data) {
    var self = this;

    self.editDockerImageMetaData = new EditDockerImageMetaData(data);

    self.editDockerImage = function(loginFormElement) {
        console.log(current_image_id);
        self.editDockerImageMetaData.launch_control_policy = $('input[name=launch_control_policy]:checked').val();
        self.editDockerImageMetaData.encrypted = false;
        self.editDockerImageMetaData.display_name = $('#display_name').val();
        current_display_name = $('#display_name').val();

        $.ajax({
            type: "POST",
            url: "/v1/trust-policy-drafts",
            // accept: "application/json",
            contentType: "application/json",
            headers: {
                'Accept': 'application/json'
            },
            data: ko.toJSON(self.editDockerImageMetaData), // $("#loginForm").serialize(),
            success: function(data, status, xhr) {

                if (data.status == "Error") {
                    $('#for_mount_edit_docker').hide();
                    $('#default_edit_docker').show();
                    $('#error_modal_body_edit_docker_1').text(data.details);
                    $("#error_modal_edit_docker_1").modal({
                        backdrop: "static"
                    });
                    $("#editDockerPolicyNext").prop('disabled', false);
                    return;
                }
                current_trust_policy_draft_id = data.id;
                var mountimage = {
                    "id": current_image_id
                }
                $.ajax({
                    type: "POST",
                    url: "/v1/rpc/mount-image",
                    // accept: "application/json",
                    contentType: "application/json",
                    headers: {
                        'Accept': 'application/json'
                    },
                    data: JSON.stringify(mountimage), // $("#loginForm").serialize(),
                    success: function(data, status, xhr) {
                        $("#editDockerPolicyNext").prop('disabled', false);
                        if (data.status == "Error") {
                            $('#for_mount_edit_docker').show();
                            $('#default_edit_docker').hide();
                            $('#error_modal_body_edit_docker_1').text(data.details);
                            $("#error_modal_edit_docker_1").modal({
                                backdrop: "static"
                            });
                            return;
                        }
                        nextButtonDocker();
                    }
                });
                
            }
        });

    }

};

function showImageLaunchPoliciesDocker(policydata) {
    current_display_name = policydata.display_name;
    $("#display_name").val(current_display_name);
    $.ajax({
        type: "GET",
        url: "/v1/image-launch-policies?deploymentType=Docker",
        dataType: "json",
        success: function(data, status, xhr) {
            image_policies = data.image_launch_policies;
            addRadios(image_policies);
            $("input[name=launch_control_policy][value='" + policydata.launch_control_policy + "']").attr('checked', 'checked');

            if (policydata.encrypted == true) {
                $("input[name=isEncrypted]").prop('checked', 'true');
            }
            mainViewModel.editDockerImageViewModel = new EditDockerImageViewModel(
                policydata);

            ko.applyBindings(mainViewModel, document
                .getElementById("edit_policy_docker_content_step_1"));
        }
    });

};

function addRadios(arr) {

    var temp = "";
    for (var i = 0; i < arr.length; i++) {
        if (arr[i].name == 'encrypted') {
            continue;
        }
        temp = temp + '<label class="radio-inline"><input type="radio" name="launch_control_policy" value="' + arr[i].name + '" >' + arr[i].value + '</label>';
    }

    $('#launch_control_policy').html(temp);
};