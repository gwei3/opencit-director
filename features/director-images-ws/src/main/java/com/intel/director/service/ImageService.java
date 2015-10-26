package com.intel.director.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;

import com.intel.director.api.CreateTrustPolicyMetaDataRequest;
import com.intel.director.api.CreateTrustPolicyMetaDataResponse;
import com.intel.director.api.ImageActionObject;
import com.intel.director.api.ImageListResponse;
import com.intel.director.api.ImageStoreResponse;
import com.intel.director.api.ImageStoreUploadRequest;
import com.intel.director.api.MountImageResponse;
import com.intel.director.api.PolicyToMountedImageRequest;
import com.intel.director.api.PolicyToMountedImageResponse;
import com.intel.director.api.SearchFilesInImageRequest;
import com.intel.director.api.SearchFilesInImageResponse;
import com.intel.director.api.SearchImagesRequest;
import com.intel.director.api.SearchImagesResponse;
import com.intel.director.api.TrustDirectorImageUploadResponse;
import com.intel.director.api.TrustPolicy;
import com.intel.director.api.TrustPolicyDraft;
import com.intel.director.api.TrustPolicyDraftEditRequest;
import com.intel.director.api.UnmountImageResponse;
import com.intel.director.api.ui.ImageInfo;
import com.intel.director.images.exception.DirectorException;
import com.intel.mtwilson.director.db.exception.DbException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * this interface serves the purpose of wrapping all the methods that the web
 * service would need. This service implementation would internally call the
 * DAO, the XML generation class etc.
 * 
 * @author Siddharth
 */
public interface ImageService {

	
	public MountImageResponse mountImage(String imageId, String user)
			throws DirectorException;

	public UnmountImageResponse unMountImage(String imageId, String user)
			throws DirectorException;

	public TrustDirectorImageUploadResponse uploadImageToTrustDirectorSingle(
			String image_deployments, String image_format,
			HttpServletRequest request) throws DirectorException;


	public SearchImagesResponse searchImages(
			SearchImagesRequest searchImagesRequest) throws DbException;

	public SearchFilesInImageResponse searchFilesInImage(
			SearchFilesInImageRequest searchFilesInImageRequest) throws DirectorException;;

	public ImageStoreResponse uploadImageToImageStore(
			ImageStoreUploadRequest imageStoreUploadRequest)
			throws DirectorException;

	public String getTrustPolicyForImage(String imageId);

	public void editTrustPolicyDraft(
			TrustPolicyDraftEditRequest trustPolicyDraftEditRequest);

	public CreateTrustPolicyMetaDataResponse saveTrustPolicyMetaData(
			CreateTrustPolicyMetaDataRequest createTrustPolicyMetaDataRequest)
			throws DirectorException;

	public CreateTrustPolicyMetaDataRequest getPolicyMetadata(String draftid)
			throws DirectorException;

	public ImageListResponse getImagesForVM(List<ImageInfo> images) throws DirectorException;
	
	public CreateTrustPolicyMetaDataRequest getPolicyMetadataForImage(
			String image_id) throws DirectorException;

	public String createTrustPolicy(String image_id)
			throws DirectorException, JAXBException;

	public TrustPolicy getTrustPolicyByTrustId(String trustId);

	public PolicyToMountedImageResponse pushPolicyToMountedImage(
			PolicyToMountedImageRequest policyToMountedImageRequest)
			throws DirectorException;


	public TrustPolicyDraft createPolicyDraftFromPolicy(String imageId, String image_action_id) throws DirectorException;

	public String getDisplayNameForImage(String image_id) throws DirectorException;

	ImageListResponse getImagesForBareMetal(List<ImageInfo> images)
			throws DirectorException;

	public ImageListResponse getBareMetalLive(List<ImageInfo> images) throws DirectorException;

	public ImageListResponse getImages(List<ImageInfo> images, String deployment_type) throws DirectorException;

	public String getFilepathForImage(String imageId, boolean isModified) throws DbException;

	public TrustPolicy getTrustPolicyByImageId(String imageId) throws DbException;

}