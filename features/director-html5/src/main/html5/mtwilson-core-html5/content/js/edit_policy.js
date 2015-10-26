
var imageFormats = new Array();
var image_policies = new Array();

$.ajax({
	type : "GET",
	url : endpoint + current_image_id + "/getpolicymetadataforimage",
	// accept: "application/json",
	contentType : "application/json",
	headers : {
		'Accept' : 'application/json'
	},
	dataType : "json",
	success : function(data, status, xhr) {
		// / alert("getmetadata data::"+data);
		showImageLaunchPolicies(data);

		/*
		 * imageFormats=data.image_formats;
		 * 
		 * var option=""; for (var i=0;i<imageFormats.length;i++){ option += '<option
		 * value="'+ imageFormats[i] + '">' + imageFormats[i] + '</option>';
		 *  } $('#image_format').append(option);
		 */

	}
});

function EditImageMetaData(data) {

	this.imageid = current_image_id;
	this.image_name = current_image_name;
	this.display_name = current_display_name;

}

function EditImageViewModel(data) {
	var self = this;

	self.editImageMetaData = new EditImageMetaData(data);

	self.editImage = function(loginFormElement) {
		console.log(current_image_id);
		self.editImageMetaData.launch_control_policy = $(
				'input[name=launch_control_policy]:checked').val();
		// /
		// self.editImageMetaData.asset_tag_policy=$('input[name=asset_tag_policy]:checked').val();
		self.editImageMetaData.encrypted = $('input[name=isEncrypted]').is(
				':checked')
		self.editImageMetaData.display_name = $('#display_name').val();
		current_display_name = $('#display_name').val(); 
		$.ajax({
			type : "POST",
			url : endpoint + "trustpoliciesmetadata",
			// accept: "application/json",
			contentType : "application/json",
			headers : {
				'Accept' : 'application/json'
			},
			data : ko.toJSON(self.editImageMetaData), // $("#loginForm").serialize(),
			success : function(data, status, xhr) {

				$.ajax({
					type : "POST",
					url : endpoint + current_image_id + "/mount",
					// accept: "application/json",
					contentType : "application/json",
					headers : {
						'Accept' : 'application/json'
					},
					data : ko.toJSON(self.createImageMetaData), // $("#loginForm").serialize(),
					success : function(data, status, xhr) {
						// /alert("data"+data);
						nextButton();
					}
				});
				// /nextButton();
			}
		});

	}

};

function showImageLaunchPolicies(policydata) {

	$.ajax({
		type : "GET",
		url : endpoint + current_image_id + "/trustpolicymetadata",
		dataType : "json",
		success : function(data, status, xhr) {
			
			current_display_name = data.display_name;
			console.log(current_display_name);
			$("#display_name").val(current_display_name);


			image_policies = data.image_launch_policies;
			addRadios(image_policies);
			$(
					"input[name=launch_control_policy][value='"
							+ policydata.launch_control_policy + "']").attr(
					'checked', 'checked');

			// / $("input[name=asset_tag_policy][value='Trust
			// Only']").attr('checked', 'checked');
			if (policydata.isEncrypted == true) {
				$("input[name=isEncrypted]").prop('checked', 'true');
			}
			mainViewModel.editImageViewModel = new EditImageViewModel(
					policydata);
			// /] ko.cleanNode(mainViewModel);

			ko.applyBindings(mainViewModel, document
					.getElementById("edit_policy_content_step_1"));
		}
	});

};

function addRadios(arr) {

	var temp = "";
	for ( var i = 0; i < arr.length; i++) {

		temp = temp
				+ '<label class="radio-inline"><input type="radio" name="launch_control_policy" value="'
				+ arr[i] + '" >' + arr[i] + '</label>';

	}

	$('#launch_control_policy').html(temp);
};
