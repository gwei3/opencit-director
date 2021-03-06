/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.director.async.task;

import java.util.List;

import javax.xml.bind.JAXBException;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.director.api.ImageStoreUploadTransferObject;
import com.intel.director.api.ui.ImageStoreUploadFields;
import com.intel.director.api.ui.ImageStoreUploadOrderBy;
import com.intel.director.api.ui.OrderByEnum;
import com.intel.director.common.Constants;
import com.intel.director.common.exception.DirectorException;
import com.intel.director.service.TrustPolicyService;
import com.intel.director.service.impl.TrustPolicyServiceImpl;
import com.intel.director.util.TdaasUtil;
import com.intel.mtwilson.director.db.exception.DbException;

/**
 * Task to upload the policy to image store
 * 
 * @author GS-0681
 */
public class RecreatePolicy extends GenericUpload {
	public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RecreatePolicy.class);

	public RecreatePolicy() throws DirectorException {
		super();
	}

	@Override
	public String getTaskName() {
		return Constants.TASK_NAME_RECREATE_POLICY;
	}

	/**
	 * Entry method for uploading policy
	 */
	@Override
	public boolean run() {
		boolean runFlag = false;
		if (previousTasksCompleted(taskAction.getTask_name())) {
			if (Constants.INCOMPLETE.equalsIgnoreCase(taskAction.getStatus())) {
				updateImageActionState(Constants.IN_PROGRESS, "Started");
				runFlag = runRecreatePolicyTask();
			}
		}
		return runFlag;
	}

	/**
	 * Actual implementation of policy upload task
	 */
	public boolean runRecreatePolicyTask() {
		boolean runFlag = true;

		// try {
		log.info("Inside runRecreatePolicyTask for ::" + imageActionObject.getImage_id());

		com.intel.mtwilson.trustpolicy2.xml.TrustPolicy trustPolicyObj;
		try {
			trustPolicyObj = TdaasUtil.getPolicy(trustPolicy.getTrust_policy());
		} catch (JAXBException e) {
			updateImageActionState(Constants.ERROR, "Error in recreating policy");
			log.error("JAXBException , Recreate task", e);
			return false;
		}

		// / imageLocation = imageInfo.getLocation();
		// Fetch the policy and write to a location. Move to common

		ImageStoreUploadOrderBy imgOrder = new ImageStoreUploadOrderBy();
		imgOrder.setImgStoreUploadFields(ImageStoreUploadFields.DATE);
		imgOrder.setOrderBy(OrderByEnum.DESC);
		boolean regenPolicy = false;
		List<ImageStoreUploadTransferObject> imageUploads;
		try {
			imageUploads = persistService.fetchImageUploads(imgOrder);
		} catch (DbException e) {
			updateImageActionState(Constants.ERROR, "Error in recreating policy");
			log.error("Error in fetchImageUploads , Recreate task", e);
			return false;
		}

		for (ImageStoreUploadTransferObject imageTransfer : imageUploads) {
			if (imageTransfer.getStoreArtifactId() != null) {
				if (imageTransfer.getStoreArtifactId().equals(
						trustPolicyObj.getImage().getImageId())) {
					log.info("Found an image in glance");
					regenPolicy = true;
					break;
				}
			}
		}

		if (regenPolicy) {
			log.info("Regen for image {}", imageActionObject.getImage_id());
			log.info("Image has policy id {}", imageInfo.getTrust_policy_id());

			UUID uuid = new UUID();
			trustPolicyObj.getImage().setImageId(uuid.toString());
			trustPolicyObj.setSignature(null);
			TrustPolicyService trustPolicyService;
			try {
				trustPolicyService = new TrustPolicyServiceImpl(imageInfo.getId());
			} catch (DirectorException e1) {
				log.error("Error init TrustPolicyService", e1);
				return false;
			}
			String policyXml;
			try {
				policyXml = TdaasUtil.convertTrustPolicyToString(trustPolicyObj);
			} catch (JAXBException e) {
				log.error("Error converting policy object to string", e);
				return false;
			}
			try {
				policyXml = trustPolicyService.signTrustPolicy(policyXml);
			} catch (DirectorException e) {
				log.error("Error signing trust policy", e);
				return false;
			}
			trustPolicy.setTrust_policy(policyXml);
			try {
				trustPolicy = trustPolicyService.archiveAndSaveTrustPolicy(policyXml,
						trustPolicy.getCreated_by_user_id());
			} catch (DirectorException e) {
				log.error("Error saving trust policy", e);
				return false;
			}
		}

		updateImageActionState(Constants.COMPLETE, "recreate task completed");

		return runFlag;

	}

}
