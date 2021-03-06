var imageFormats = new Array();
var image_policies = new Array();

$(document).ready(function() {
    fetchImageLaunchPolicies();
	fetchImaheHashAlgo("VM","hashtype_vm");
});

function CreateImageMetaData(data) {

    this.image_id = current_image_id;

    this.image_name = current_image_name;
    this.display_name = current_display_name;
}

function CreateImageViewModel() {
    var self = this;
    $("input[name=isEncrypted]").val(false);

    self.createImageMetaData = new CreateImageMetaData({});

    self.createImage = function(loginFormElement) {
        $("#createVMPolicyNext").prop('disabled', true);
        self.createImageMetaData.launch_control_policy = $('input[name=launch_control_policy]:checked').val();
        // self.createImageMetaData.asset_tag_policy=$('input[name=asset_tag_policy]:checked').val();
        self.createImageMetaData.encrypted = $('input[name=isEncrypted]').is(
            ':checked');

        self.createImageMetaData.display_name = $('#display_name').val();
        current_display_name = $('#display_name').val();
		showLoading();
        $.ajax({
            type: "POST",
            url: "/v1/trust-policy-drafts",
            contentType: "application/json",
            headers: {
                'Accept': 'application/json'
            },
            data: ko.toJSON(self.createImageMetaData), // $("#loginForm").serialize(),

            success: function(data, status, xhr) {
            	data=htmlEncode(data);
                if (data.error) {
					hideLoading();
                    $('#for_mount').hide();
                    $('#default').show();
                    $('#error_modal_body_vm_1').text(htmlEncode(data.error));
                    $("#error_modal_vm_1").modal({
                        backdrop: "static"
                    });
                    $('body').removeClass("modal-open");
                    $("#createVMPolicyNext").prop('disabled', false);
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
                    data: JSON.stringify(mountimage),
                    success: function(data, status, xhr) {
                    	data=htmlEncode(data);
                        $("#createVMPolicyNext").prop('disabled', false);
                        if (data.error) {
							hideLoading();
                            $('#default').hide();
                            $('#for_mount').show();
                            $('#error_modal_body_vm_1').text(htmlEncode(data.error));
                            $("#error_modal_vm_1").modal({
                                backdrop: "static"
                            });
                            $('body').removeClass("modal-open");
                            return;

                        }
						
                        nextButton();
                    }
                });
            },
            error: function(data, status, xhr) {
            	data=htmlEncode(data);
                console.log(data);
				hideLoading();
                $('#for_mount').hide();
                $('#default').show();
                $('#error_modal_body_vm_1').text("");
                var obj = jQuery.parseJSON(data.responseText);

                $('#error_modal_body_vm_1').text(htmlEncode(obj.error));
                $("#error_modal_vm_1").modal({
                    backdrop: "static"
                });
                $('body').removeClass("modal-open");
                $("#createVMPolicyNext").prop('disabled', false);
                return;
            }
        });

    }

};

function fetchImageLaunchPolicies() {

    $("#display_name").val(current_image_name);
    $.ajax({
        type: "GET",
        url: "/v1/image-launch-policies?deploymentType=VM",
        dataType: "json",
        success: function(data, status, xhr) {
        	data=htmlEncode(data);
            image_policies = data.image_launch_policies;
            addRadios(image_policies);
            mainViewModel.createImageViewModel = new CreateImageViewModel();

            $("input[name=launch_control_policy][value='MeasureOnly']").attr('checked', 'checked');
            ko.applyBindings(mainViewModel, document.getElementById("create_policy_content_step_1"));
        }
    });

};

function addRadios(arr) {

    var temp = "";
    for (var i = 0; i < arr.length; i++) {
        if (arr[i].name == 'encrypted') {
            continue;
        }
        temp = temp + '<label class="radio-inline"><input type="radio" name="launch_control_policy" id="create_policy_' + arr[i].name + '" value="' + arr[i].name + '">' + arr[i].display_name + '</label>';

    }

    $('#launch_control_policy').html(temp);
    $.ajax({
        type: 'GET',
        url: '/v1/setting/kms',
        contentType: "application/json",
        success: function(data) {
        	data=htmlEncode(data);
            if (data.kms_endpoint_url == "" || data.kms_login_basic_username == "" || data.kms_tls_policy_certificate_sha256 == "" ||
                data.kms_endpoint_url == null || data.kms_login_basic_username == null || data.kms_tls_policy_certificate_sha256 == null ||
                data.kms_endpoint_url == undefined || data.kms_login_basic_username == undefined || data.kms_tls_policy_certificate_sha256 == undefined) {
                $('#encryptRow').hide();
            } else {
                $('#encryptRow').show();
            }

        }
    });
};