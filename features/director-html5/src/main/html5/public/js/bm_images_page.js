var pageInitialized = false;
$(document)
		.ready(
				function() {
					if (pageInitialized == true)
						return;

					$("#bm_images_grid_page")
							.load(
									"/v1/html5/public/director-html5/bm_images_grid_page.html");

					pageInitialized = true;
				});

function createPolicyForBMImage(imageid, imagename) {
	currentFlow = "Create";
	current_image_id = imageid;
	current_trust_policy_draft_id='';
	current_image_name = imagename;
	goToCreatePolicyWizardForBMImage();
}

/*function createPolicy2(imageid, imagename) {
	currentFlow = "Create";
	current_image_id = imageid;
	current_trust_policy_draft_id='';
	current_image_name = imagename;

	goToCreatePolicyWizardForBMLive();
}*/

function goToCreatePolicyWizardForBMLive() {
	$("#bm_images_grid_page").hide();
	$("#create_policy_wizard2_").show();

	var htmlpage = "/v1/html5/public/director-html5/create_policy_wizard_for_hosts.html";

	var isEmpty = !$.trim($("#create_policy_wizard2_").html());

	if (isEmpty == false) {
		$("#create_policy_wizard2_").html("");
	}
	$("#create_policy_wizard2_").load(htmlpage, function() {
		console.log("Load was performed.");
	});

}

function editPolicyForBMImage(imageid, imagename) {

	currentFlow = "Edit";
	current_image_id = imageid;
	current_image_name = imagename;
	current_trust_policy_draft_id='';
	goToEditPolicyWizardForBMImage();
}

function editPolicyForBMLive(hostid, hostname,trust_policy_draft_id) {

	currentFlow = "Edit";
	current_image_id = hostid;
	current_image_name = hostname;
	current_trust_policy_draft_id=trust_policy_draft_id;
	goToEditPolicyWizardForBMLive();
}

function pushPolicyToHost(imageid, imagename, trust_policy_id) {

	currentFlow = "Upload";
	current_image_id = imageid;
	current_image_name = imagename;
	current_trust_policy_id = trust_policy_id;
	goToPushPolicyToHost();
}

function goToPushPolicyToHost() {
	$("#bm_images_grid_page").hide();
	$("#push_to_specific_host").show();
	var isEmpty = !$.trim($("#push_to_specific_host").html());

	if (isEmpty == false) {
		$("#push_to_specific_host").html("");
	}

	$("#push_to_specific_host").load("/v1/html5/public/director-html5/push_to_specific_host.html");
}

function goToCreatePolicyWizardForBMImage() {

	$("#bm_images_grid_page").hide();
	$("#create_policy_wizard_bm_image").show();

	var htmlpage = "/v1/html5/public/director-html5/create_policy_wizard_bm_image.html";
	var isEmpty = !$.trim($("#create_policy_wizard_bm_image").html());

	if (isEmpty == false) {
		$("#create_policy_wizard_bm_image").html("");
	}

	$("#create_policy_wizard_bm_image").load(htmlpage, function() {
		console.log("BM_IMAGE_WIZARD IS LOADED.");
	});

}

function goToEditPolicyWizardForBMImage() {
	$("#bm_images_grid_page").hide();
	$("#edit_policy_wizard_bm_image").show();

	var htmlpage = "/v1/html5/public/director-html5/edit_policy_wizard_bm_image.html";

	var isEmpty = !$.trim($("#edit_policy_wizard_bm_image").html());

	if (isEmpty == false) {
		$("#edit_policy_wizard_bm_image").html("");
	}

	$("#edit_policy_wizard_bm_image").load(htmlpage);

}

function goToEditPolicyWizardForBMLive() {
	$("#bm_images_grid_page").hide();
	$("#edit_policy_wizard_bm_live").show();

	var htmlpage = "/v1/html5/public/director-html5/edit_policy_wizard_bm_live.html";

	var isEmpty = !$.trim($("#edit_policy_wizard_bm_live").html());

	if (isEmpty == false) {
		$("#edit_policy_wizard_bm_live").html("");
	}

	$("#edit_policy_wizard_bm_live").load(htmlpage);

}

// bactToHost...
function backToHostsPage() {
	$("#create_policy_wizard2_").html("");
	$("#create_policy_wizard2_").hide("");
	$("#create_policy_wizard_bm_image").html("");
	$("#create_policy_wizard_bm_image").hide("");
	$("#edit_policy_wizard_bm_live").html("");
	$("#edit_policy_wizard_bm_live").hide("");
	$("#edit_policy_wizard_bm_image").html("");
	$("#edit_policy_wizard_bm_image").hide("");
	$("#bm_images_grid_page").show();
	if(current_image_id != "" && current_image_id !=null)
	{
		var mountimage = {
			"id" : current_image_id
		}
		$.ajax({
			type : "POST",
			url : "/v1/rpc/unmount-image",
			contentType : "application/json",
			headers : {
				'Accept' : 'application/json'
			},
			data : ko.toJSON(self.mountimage),
			success : function(data, status, xhr) {
				current_image_id = "";
				console.log("IMAGE UNMOUNTED BECAUSE OF BACKTOHOSTPAGES");
			}
		});

	}
	current_trust_policy_draft_id='';
	$('body').removeClass("modal-open");
	refreshBMOnlineGrid();
	//refresh_bm_images_Grid();
}

function backToHostsPageWithoutUnmount() {

	$("#create_policy_wizard2_").html("");
	$("#create_policy_wizard2_").hide("");
	$("#create_policy_wizard_bm_image").html("");
	$("#create_policy_wizard_bm_image").hide("");
	$("#edit_policy_wizard_bm_live").html("");
	$("#edit_policy_wizard_bm_live").hide("");
	$("#edit_policy_wizard_bm_image").html("");
	$("#edit_policy_wizard_bm_image").hide("");
	$("#bm_images_grid_page").show();
	$('body').removeClass("modal-open");
current_image_id='';
current_trust_policy_draft_id='';
	refreshBMOnlineGrid();
	//refresh_bm_images_Grid();
}

function backButtonImagesBM() {
	var activeMasterStepsDiv;
	var active_wizard_page;
	if ($('#create_policy_wizard_bm_image').is(':hidden')) {
		activeMasterStepsDiv = "editPolicyStepsBMImage";
		active_wizard_page = "edit_policy_bm_image";

	} else if ($('#edit_policy_wizard_bm_image').is(':hidden')) {
		activeMasterStepsDiv = "createPolicyStepsBMImage";
		active_wizard_page = "create_policy_bm_image";

	}

	var divid = $('#' + activeMasterStepsDiv).find("li .selected").attr('id');

	var n = divid.lastIndexOf("_");

	var stepNum = divid.substring(n + 1);

	var previousStepNum = eval(stepNum) - 1;

	$("#" + active_wizard_page + "_content_step_" + stepNum).hide();
	$("#" + active_wizard_page + "_step_" + stepNum).removeClass("selected");
	$("#" + active_wizard_page + "_step_" + stepNum).addClass("done");
	$("#" + active_wizard_page + "_content_step_" + previousStepNum).show();

	$("#" + active_wizard_page + "_step_" + previousStepNum)
			.removeClass("done");
	$("#" + active_wizard_page + "_step_" + previousStepNum).addClass(
			"selected");

}

function backButtonLiveBM() {
	var activeMasterStepsDiv;
	var active_wizard_page;
	if ($('#create_policy_wizard2_').is(':hidden')) {
		activeMasterStepsDiv = "editPolicyStepsBMLive";
		active_wizard_page = "edit_policy_bm_live";

	} else if ($('#edit_policy_wizard_bm_live').is(':hidden')) {
		activeMasterStepsDiv = "createPolicyStepsForHosts";
		active_wizard_page = "create_policy_for_hosts";

	}
	//activeMasterStepsDiv = "createPolicyStepsForHosts";
	//active_wizard_page = "create_policy_for_hosts";

	var divid = $('#' + activeMasterStepsDiv).find("li .selected").attr('id');

	var n = divid.lastIndexOf("_");

	var stepNum = divid.substring(n + 1);

	var previousStepNum = eval(stepNum) - 1;

	$("#" + active_wizard_page + "_content_step_" + stepNum).hide();
	$("#" + active_wizard_page + "_step_" + stepNum).removeClass("selected");
	$("#" + active_wizard_page + "_step_" + stepNum).addClass("done");
	$("#" + active_wizard_page + "_content_step_" + previousStepNum).show();

	$("#" + active_wizard_page + "_step_" + previousStepNum)
			.removeClass("done");
	$("#" + active_wizard_page + "_step_" + previousStepNum).addClass(
			"selected");

}

function nextButtonLiveBM() {

	var activeMasterStepsDiv;
	var active_wizard_page;
	if ($('#create_policy_wizard2_').is(':hidden')) {
		activeMasterStepsDiv = "editPolicyStepsBMLive";
		active_wizard_page = "edit_policy_bm_live";

	} else if ($('#edit_policy_wizard_bm_live').is(':hidden')) {
		activeMasterStepsDiv = "createPolicyStepsForHosts";
		active_wizard_page = "create_policy_for_hosts";

	}

	var divid = $('#' + activeMasterStepsDiv).find("li .selected").attr('id');

	var n = divid.lastIndexOf("_");

	var stepNum = divid.substring(n + 1);

	var nextStepNum = eval(stepNum) + 1;

	$("#" + active_wizard_page + "_content_step_" + stepNum).hide();
	$("#" + active_wizard_page + "_content_step_" + nextStepNum).show();
	var html_url = $("#" + active_wizard_page + "_step_" + nextStepNum).attr(
			'href');
	var htmlpage = "/v1/html5/public/director-html5/"
			+ html_url.substring(1);

	var isEmpty = !$.trim($(
			"#" + active_wizard_page + "_content_step_" + nextStepNum).html());

	if (isEmpty == true) {
		$("#" + active_wizard_page + "_content_step_" + nextStepNum).load(
				htmlpage);
	}else{
        $("#director_loading_icon").hide();
        $("#loading_icon_container").hide();
        $("#director_loading_icon").html("");
    }

	$("#" + active_wizard_page + "_step_" + nextStepNum).addClass("selected");
	$("#" + active_wizard_page + "_step_" + stepNum).removeClass("selected");
	$("#" + active_wizard_page + "_step_" + stepNum).addClass("done");
	$("#" + active_wizard_page + "_step_" + nextStepNum)
			.removeClass("disabled");
	$("#" + active_wizard_page + "_step_" + nextStepNum).removeClass("done");
	$("#" + active_wizard_page + "_step_" + nextStepNum).addClass("selected");
}


function nextButtonImagesBM() {

	var activeMasterStepsDiv;
	var active_wizard_page;
	if ($('#create_policy_wizard_bm_image').is(':hidden')) {
		activeMasterStepsDiv = "editPolicyStepsBMImage";
		active_wizard_page = "edit_policy_bm_image";

	} else if ($('#edit_policy_wizard_bm_image').is(':hidden')) {
		activeMasterStepsDiv = "createPolicyStepsBMImage";
		active_wizard_page = "create_policy_bm_image";

	}

	var divid = $('#' + activeMasterStepsDiv).find("li .selected").attr('id');

	console.log(divid);
	var n = divid.lastIndexOf("_");

	var stepNum = divid.substring(n + 1);

	var nextStepNum = eval(stepNum) + 1;

	$("#" + active_wizard_page + "_content_step_" + stepNum).hide();
	$("#" + active_wizard_page + "_content_step_" + nextStepNum).show();
	var html_url = $("#" + active_wizard_page + "_step_" + nextStepNum).attr(
			'href');
	var htmlpage = "/v1/html5/public/director-html5/"
			+ html_url.substring(1);

	var isEmpty = !$.trim($(
			"#" + active_wizard_page + "_content_step_" + nextStepNum).html());

	if (isEmpty == true) {
		$("#" + active_wizard_page + "_content_step_" + nextStepNum).load(
				htmlpage);
	}

	$("#" + active_wizard_page + "_step_" + nextStepNum).addClass("selected");
	$("#" + active_wizard_page + "_step_" + stepNum).removeClass("selected");
	$("#" + active_wizard_page + "_step_" + stepNum).addClass("done");
	$("#" + active_wizard_page + "_step_" + nextStepNum)
			.removeClass("disabled");
	$("#" + active_wizard_page + "_step_" + nextStepNum).removeClass("done");
	$("#" + active_wizard_page + "_step_" + nextStepNum).addClass("selected");
}

function downloadPolicyFromLastPage() {
	downloadPolicyAndManifest(current_image_id,null);
}
function downloadImageFromLastPage() {
	downloadImage(current_image_id);
}

function deletePolicyBM(imageid) {
	$.ajax({
		type : "DELETE",
		url : "/v1/images/" + imageid,
		dataType : "text",
		success : function(result) {
			refreshBMOnlineGrid();
		}
	});

	
}



