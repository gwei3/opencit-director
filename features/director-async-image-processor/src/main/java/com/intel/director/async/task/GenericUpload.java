package com.intel.director.async.task;

import java.util.Calendar;

import org.apache.commons.lang.StringUtils;

import com.intel.director.api.ImageActionObject;
import com.intel.director.api.ImageActionTask;
import com.intel.director.api.ImageAttributes;
import com.intel.director.api.ImageStoreUploadResponse;
import com.intel.director.api.ImageStoreUploadTransferObject;
import com.intel.director.api.PolicyUploadTransferObject;
import com.intel.director.api.StoreResponse;
import com.intel.director.api.TrustPolicy;
import com.intel.director.api.ui.ImageInfo;
import com.intel.director.common.Constants;
import com.intel.director.common.DirectorUtil;
import com.intel.director.common.exception.DirectorException;
import com.intel.director.store.StoreManager;
import com.intel.director.store.StoreManagerFactory;
import com.intel.director.store.exception.StoreException;
import com.intel.mtwilson.director.db.exception.DbException;

/**
 * Superclass for all upload tasks
 * 
 * @author Siddharth
 * 
 */
public abstract class GenericUpload extends ImageActionAsync {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
			.getLogger(GenericUpload.class);
	public String storeId = null;
///	private ImageActionService imageActionService = new ImageActionImpl();

	public GenericUpload() throws DirectorException {
		super();
	}

	public GenericUpload(String imageStoreId) throws DirectorException {
		super();
		this.storeId = imageStoreId;
	}

	@Override
	public boolean run() {
		
		try {
			imageInfo = persistService.fetchImageById(imageActionObject
					.getImage_id());
		} catch (DbException e1) {
			return false;
		}
		if (imageInfo == null) {
			return false;
		}
		boolean runFlag = true;
		// Select the image store for upload
		StoreManager imageStoreManager;
		try {
			imageStoreManager = StoreManagerFactory
					.getStoreManager(taskAction.getStoreId());
		} catch (StoreException e1) {
			updateImageActionState(Constants.ERROR, e1.getMessage());
			log.error("Erorr in getting imagestore manager",e1);		
			return false;
		}
		ImageActionTask imageActionTask = getImageActionTaskFromArray();
		customProperties.put(ImageActionObject.class.getName(), imageActionObject);
		try {
			imageStoreManager.addCustomProperties(customProperties);
		} catch (StoreException e1) {
			log.error("Erorr in addCustomProperties imagestore manager",e1);		
			updateImageActionState(Constants.ERROR, e1.getMessage());
			return false;
		}
		// Upload is an async task
		if (imageActionTask != null && imageActionTask.getUri() == null) {
			try {
				imageStoreManager.upload();
			} catch (StoreException e) {
				log.error("Error uploading the artifact to store id {}",
						taskAction.getStoreId(), e);				
				if(e.getMessage().startsWith(Constants.ARTIFACT_ID+":")){
					log.info("Updating mw image uploads with error");
					ImageStoreUploadResponse imageStoreUploadResponse = new ImageStoreUploadResponse();
					imageStoreUploadResponse.setStatus(Constants.ERROR);
					imageStoreUploadResponse.setId(e.getMessage().substring(e.getMessage().indexOf(":")+1));
					try {
						updateUploadTable(imageStoreUploadResponse);
					} catch (DirectorException e1) {
						log.error("Error updating uploads table", e1);
						return false;
					}

				}
				return false;
			}
			log.info("Upload process started");
		}
		StoreResponse storeResponse;
		try {
			 storeResponse=(StoreResponse)imageStoreManager.fetchDetails();
		} catch (StoreException e) {
			log.error("Erorr in fetchDetails imagestoremanager",e);
			updateImageActionState(Constants.ERROR, e.getMessage());
			return false;
		}
		updateImageActionState(Constants.COMPLETE,getTaskName()+" complete");
		try {
			updateUploadTable(storeResponse);
		} catch (DirectorException e) {
			log.error("Error in updating uploads table",e);
			updateImageActionState(Constants.ERROR, "Error in updating uploads table");
			return false;
		}
		

		return runFlag;
	}

	private void updateUploadTable(StoreResponse storeResponse) throws DirectorException {
		if (getTaskName().equals(Constants.TASK_NAME_UPLOAD_POLICY)) {
			updatePolicyUploads(storeResponse);
		} else {
			updateImageUploads(storeResponse);
		}
	}

	private void updateImageUploads(StoreResponse storeResponse)
			throws DirectorException {
		// /
		// log.info("updating image uploads table for image id {}",imageActionObject.getImage_id());
		ImageStoreUploadTransferObject imageUploadTransferObject = new ImageStoreUploadTransferObject();
		imageUploadTransferObject.setStatus(storeResponse.getStatus());
		ImageAttributes imageAttr = new ImageAttributes();
		ImageInfo image;
		try {
			image = persistService.fetchImageById(imageInfo.id);
		} catch (DbException e) {
			log.error("Error fetching image", e);
			throw new DirectorException(e);
		}
		if (image == null) {
			log.error("No Image Found with id " + imageInfo.id);
			throw new DirectorException("No Image Found with id " + imageInfo.id);
		}
		String trustPolicyId = image.getTrust_policy_id();

		String dekUrl = DirectorUtil.fetchDekUrl(trustPolicy);

		String imageId = imageInfo.getId();
		String glanceId = storeResponse.getId();

		if (StringUtils.isNotBlank(trustPolicyId)) {
			TrustPolicy trustPolicy2;
			try {
				trustPolicy2 = persistService.fetchPolicyById(trustPolicyId);
			} catch (DbException e) {
				log.error("Error fetching policy", e);
				throw new DirectorException(e);
			}
			dekUrl = DirectorUtil.fetchDekUrl(trustPolicy2);
		}
		
		String storeArtifactName=(String)customProperties.get(Constants.NAME);
		imageAttr.setId(imageId);
		imageUploadTransferObject.setImg(imageAttr);
		imageUploadTransferObject.setDate(Calendar.getInstance());
		imageUploadTransferObject.setStoreId(taskAction.getStoreId());
		imageUploadTransferObject.setStoreArtifactName(storeArtifactName);
		log.info(
				"updating image uploads table for image id {}",
				imageActionObject.getImage_id()
						+ " imageUri::"
						+ ((ImageStoreUploadResponse) storeResponse)
								.getImage_uri());
		imageUploadTransferObject.setImage_uri(storeResponse.getUri());
		imageUploadTransferObject.setStoreArtifactId(glanceId);
		imageUploadTransferObject.setActionId(imageActionObject.getId());
		String uploadVariableMD5 = DirectorUtil.computeUploadVar(glanceId,
				dekUrl);
		imageUploadTransferObject.setUploadVariableMD5(uploadVariableMD5);
		updateImage(uploadVariableMD5);
		try {
			persistService.saveImageUpload(imageUploadTransferObject);
			log.info("updated data for image {} uploaded",
					imageActionObject.getImage_id());
		} catch (DbException e1) {
			log.error("Error updating ImageUploads table", e1);
			throw new DirectorException(
					"GenericUploadTask, error saving imageupload data", e1);
		}

	}

	private void updatePolicyUploads(StoreResponse storeResponse) throws DirectorException {
		PolicyUploadTransferObject policyUploadTranserObject=new PolicyUploadTransferObject();
		policyUploadTranserObject.setStatus(storeResponse.getStatus());
		policyUploadTranserObject.setTrust_policy(trustPolicy);
		policyUploadTranserObject.setStoreId(taskAction.getStoreId());
		policyUploadTranserObject.setDate(Calendar.getInstance());
		log.info("updating policy uploads table for image id {}",imageActionObject.getImage_id()+" policyId "+trustPolicy.getId()+" and diplay name::"+trustPolicy.getDisplay_name());
		String glanceId= storeResponse.getId();
		String dekUrl=DirectorUtil.fetchDekUrl(trustPolicy);
		policyUploadTranserObject.setStoreArtifactId(glanceId);
		String uploadVariableMD5=DirectorUtil.computeUploadVar(glanceId, dekUrl);
		policyUploadTranserObject.setUploadVariableMD5(uploadVariableMD5);
		policyUploadTranserObject.setPolicy_uri(storeResponse.getUri());
	///	updateImage(uploadVariableMD5);
		try {
			persistService.savePolicyUpload(policyUploadTranserObject);
		} catch (DbException e) {
			log.error("Error updating Upload Policy table", e);
			throw new DirectorException("GenericUploadTask, error saving policyUpload data in policyuploadtable",e);
			
		}
		
	}
	
	private void updateImage(String uploadVariableMD5) throws DirectorException{
		imageInfo.setUploadVariableMD5(uploadVariableMD5);
		try {
			persistService.updateImage(imageInfo);
		} catch (DbException e) {
			log.error("Error updating image " + imageInfo, e);
			throw new DirectorException("GenericUploadTask, error uploading uploadvariablemd5 in image table",e);
			
			
		}
	}

}
