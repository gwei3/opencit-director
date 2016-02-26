/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.director.images;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;

import com.intel.dcsg.cpg.validation.RegexPatterns;
import com.intel.dcsg.cpg.validation.ValidationUtil;
import com.intel.director.api.CommonValidations;
import com.intel.director.api.DockerRequestObject;
import com.intel.director.api.GenericResponse;
import com.intel.director.api.ImageInfoResponse;
import com.intel.director.api.ListImageDeploymentsResponse;
import com.intel.director.api.ListImageFormatsResponse;
import com.intel.director.api.ListImageLaunchPolicyResponse;
import com.intel.director.api.MountImageRequest;
import com.intel.director.api.MountImageResponse;
import com.intel.director.api.SearchFilesInImageRequest;
import com.intel.director.api.SearchFilesInImageResponse;
import com.intel.director.api.SearchImagesRequest;
import com.intel.director.api.SearchImagesResponse;
import com.intel.director.api.SshSettingRequest;
import com.intel.director.api.SshSettingResponse;
import com.intel.director.api.TrustDirectorImageUploadRequest;
import com.intel.director.api.TrustDirectorImageUploadResponse;
import com.intel.director.api.TrustPolicy;
import com.intel.director.api.UnmountImageResponse;
import com.intel.director.api.ui.ImageInfo;
import com.intel.director.common.Constants;
import com.intel.director.images.exception.DirectorException;
import com.intel.director.service.ImageService;
import com.intel.director.service.LookupService;
import com.intel.director.service.impl.ImageServiceImpl;
import com.intel.director.service.impl.LookupServiceImpl;
import com.intel.director.service.impl.SettingImpl;
import com.intel.director.util.TdaasUtil;
import com.intel.mtwilson.launcher.ws.ext.V2;
import com.intel.mtwilson.shiro.ShiroUtil;

/**
 * Images related APIs
 * 
 * @author Siddharth
 * 
 */
@V2
@Path("/")
public class Images {

	ImageService imageService = new ImageServiceImpl();
	LookupService lookupService = new LookupServiceImpl();
	SettingImpl settingimpl = new SettingImpl();
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
			.getLogger(Images.class);

	/**
	 * API for uploading image metadata like image format, deployment type(VM,
	 * BareMetal, Docker), image file name, image size ( in bytes ), etc.
	 * Creates image upload metadata with specified parameters and returns
	 * metadata along with image id.
	 * 
	 * @mtwContentTypeReturned JSON
	 * @mtwMethodType POST
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/images
	 * Input: {"image_name":"test.img","image_deployments":"VM","image_format": "qcow2", "image_size":(13631488 }
	 * Output: {"created_by_user_id":"admin","created_date":1446801301639,"edited_by_user_id":"admin",
	 * 			"edited_date":1446801301639,"id":"B79EDFE9-4690-42B7-B4F0-71C53E36368C","image_name":"test.img",
	 * 			"image_format":"qcow2","image_deployments":"VM","status":"In Progress","image_size":407552,
	 * 			"sent":0,"deleted":false,"location":"/mnt/images/"}
	 * 
	 * In Case of error such as image name already exists on the server :
	 * {
	 * 	"status" : "Error",
	 * 	"details" : "Image with Same Name already exists, choose a different name"
	 * }
	 * in case of insufficient or invalid data following response is returned:
	 * 1) Invalid deployment type : XYZ
	 * 	{
	 *   "deleted": false,
	 *   "details": "Invalid deployment type for image",
	 *   "image_upload_status": "Error"
	 * }
	 * 2) Invalid format and deployment type
	 * {
	 *   "deleted": false,
	 *   "details": "Invalid deployment type for image,Inavlid deployment format for image",
	 *   "image_upload_status": "Error"
	 * }
	 * </pre>
	 * 
	 * @param TrustDirectorImageUploadRequest
	 *            object which includes metadata information
	 * @return Response object contains newly created
	 *         image metadata along with image_id
	 * @throws DirectorException
	 */
	@Path("images")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createUploadImageMetadata(
			TrustDirectorImageUploadRequest uploadRequest)
			throws DirectorException {
		TrustDirectorImageUploadResponse uploadImageToTrustDirector;

		String errors = uploadRequest.validate();
		if (StringUtils.isNotBlank(errors)) {
			uploadImageToTrustDirector = new TrustDirectorImageUploadResponse();
			uploadImageToTrustDirector.details = uploadRequest.validate();
			uploadImageToTrustDirector.status = Constants.ERROR;
			uploadImageToTrustDirector.details = errors;
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(uploadImageToTrustDirector).build();
		}
		imageService = new ImageServiceImpl();

		try {
			if (imageService.doesImageNameExist(uploadRequest.image_name)) {
				uploadImageToTrustDirector = new TrustDirectorImageUploadResponse();
				uploadImageToTrustDirector.state = Constants.ERROR;
				uploadImageToTrustDirector.details = "Image with Same Name already exists. <br>Please Enter Image Name ";
				return Response.ok().entity(uploadImageToTrustDirector).build();
			}
			if (Constants.DEPLOYMENT_TYPE_DOCKER.equalsIgnoreCase(uploadRequest.image_deployments) && imageService.doesRepoTagExist(uploadRequest.repository,uploadRequest.tag)) {
				uploadImageToTrustDirector = new TrustDirectorImageUploadResponse();
				uploadImageToTrustDirector.state = Constants.ERROR;
				uploadImageToTrustDirector.details = "Image with Repo And Tag already exists..!!";
				return Response.ok(uploadImageToTrustDirector).build();
			}
			uploadImageToTrustDirector = imageService
					.createUploadImageMetadataImpl(
							uploadRequest.image_deployments,
							uploadRequest.image_format, uploadRequest.image_name,
							uploadRequest.image_size, uploadRequest.repository, uploadRequest.tag);
			uploadImageToTrustDirector.state = Constants.SUCCESS;
			log.info("Successfully uploaded image to location: "
					+ uploadImageToTrustDirector.getLocation());
			return Response.ok().entity(uploadImageToTrustDirector).build();
		} catch (DirectorException e) {
			log.error("Error in Saving Image metadata", e);
			throw new DirectorException("Error in Saving Image metadata", e);
		}

	}

	/**
	 * API for uploading image data for the given image id. Before Uploading
	 * image it is divided in chunks and sent to server one by one. Once the
	 * chunk is received location to save image is retrieved from DB using given
	 * image id and chunk is saved to that location.
	 * 
	 * 
	 * @mtwContentTypeReturned JSON
	 * @mtwMethodType POST
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/rpc/images/content/3DED763F-99BA-4F99-B53B-5A6F6736E1E9
	 * Input: chunk for image upload
	 * Output: {"created_by_user_id":"admin","created_date":1446801301639,"edited_by_user_id":"admin",
	 * 			"edited_date":1446801301639,"id":"B79EDFE9-4690-42B7-B4F0-71C53E36368C","name":"test.img",
	 * 			"image_format":"qcow2","image_deployments":"VM","status":"Complete","image_size":407552,
	 * 			"sent":407552,"deleted":false,"location":"/mnt/images/"}
	 * 
	 * While the image upload is in progress:
	 * {"created_by_user_id":"admin","created_date":1446801301639,"edited_by_user_id":"admin",
	 * 			"edited_date":1446801301639,"id":"B79EDFE9-4690-42B7-B4F0-71C53E36368C","name":"test.img",
	 * 			"image_format":"qcow2","image_deployments":"VM","status":"In Porgress","image_size":407552,
	 * 			"sent":407552,"deleted":false,"location":"/mnt/images/"}
	 * 
	 * </pre>
	 * @param imageId
	 *            - id received as response of https://{IP/HOST_NAME}/v1/images/
	 *            request
	 * @param filInputStream
	 *            - image data sent as chunk
	 * @return TrustDirectorImageUploadResponse object with updated image upload
	 *         metadata
	 * @throws DirectorException
	 */
	@Path("rpc/images/content/{imageId: [0-9a-zA-Z_-]+}")
	@POST
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadImageToTrustDirector(
			@PathParam("imageId") String imageId, InputStream filInputStream)
			throws DirectorException {
		log.info("Uploading image to TDaaS");
		imageService = new ImageServiceImpl();
		TrustDirectorImageUploadResponse uploadImageToTrustDirector =new TrustDirectorImageUploadResponse();;
		if(!ValidationUtil.isValidWithRegex(imageId,RegexPatterns.UUID)){
			uploadImageToTrustDirector.details = "Imaged id is empty or not in uuid format";
			uploadImageToTrustDirector.status = Constants.ERROR;
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(uploadImageToTrustDirector).build();
		}
		try {
			long lStartTime = new Date().getTime();

			uploadImageToTrustDirector = imageService
					.uploadImageToTrustDirector(imageId, filInputStream);
			log.info("Successfully uploaded image to location: "
					+ uploadImageToTrustDirector.getLocation());
			long lEndTime = new Date().getTime();

			long difference = lEndTime - lStartTime;
			log.info("Time taken to upload image to TD: " + difference);
			filInputStream.close();
			Session session = SecurityUtils.getSubject().getSession();
			session.touch();
			return Response.ok(uploadImageToTrustDirector).build();
		} catch (DirectorException | IOException e) {
			log.error("Error while uploading image to Trust Director", e);
			throw new DirectorException("Error in uploading image", e);
		}
	}

	/**
	 * Returns list of images in TD depending on the image deployment type
	 * supplied. This call is made so that grids on the UI for VM and Hosts can
	 * be populated. Each image has an image deployment type : BareMetal or VM.
	 * 
	 * This method gets the list of images based on the deployment type provided
	 * as a query param. Providing the deployment type is optional. If provided
	 * the value should be VM or BareMetal. edited_date field is updated in case
	 * of mounting and delete.image_upload_status can have values Incomplete and
	 * Complete. Since the upload happens in multiple steps, the status is
	 * Incomplete till the sent field is equal to image_size. image_location is
	 * for internal server use and it can't accessed it directly.
	 * 
	 * @mtwContentTypeReturned JSON
	 * @mtwMethodType GET
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/images
	 * Input: deploymentType=VM (Example : https://{IP/HOST_NAME}/v1/images?deploymentType=VM)
	 * 
	 * Output: {
	 * "images": [
	 * {
	 *       "created_by_user_id": "admin",
	 *       "created_date": "2015-12-21",
	 *       "edited_by_user_id": "admin",
	 *       "edited_date": "2015-12-21",
	 *       "id": "465A8B27-7CC8-4A3C-BBBC-26161E3853CD",
	 *       "image_name": "CIR1.img",
	 *       "image_format": "qcow2",
	 *       "image_deployments": "VM",
	 *       "image_size": 13312,
	 *       "sent": 13312,
	 *       "deleted": false,
	 *       "trust_policy_id": "0e41169f-d2f3-4566-96c7-183d699417fb",
	 *       "uploads_count": 0,
	 *       "policy_name": "CIR1.img",
	 *       "image_upload_status": "Complete",
	 *       "image_Location": "/mnt/images/"
	 *     },
	 *     {
	 *       "created_date": "2015-12-17",
	 *       "edited_by_user_id": "admin",
	 *       "edited_date": "2015-12-17",
	 *       "id": "D7952C76-8F37-474A-B054-168ACC2C0802",
	 *       "image_name": "10.35.35.182",
	 *       "image_deployments": "BareMetal",
	 *       "deleted": false,
	 *       "trust_policy_id": "f421e8cf-8d29-40b9-b05f-6b52d549dc81",
	 *       "uploads_count": 0,
	 *       "policy_name": "S1",
	 *       "image_upload_status": "Complete"
	 *     }
	 * }]}
	 * 
	 * </pre>
	 * 
	 * @param deployment_type
	 *            - VM/BareMetal
	 * @return List of image details
	 * @throws DirectorException
	 */
	@Path("images")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getImagesByDeploymentType(
			@QueryParam("deploymentType") String deployment_type)
			throws DirectorException {
		SearchImagesRequest searchImagesRequest = new SearchImagesRequest();
		SearchImagesResponse searchImagesResponse = new SearchImagesResponse();
		if(!CommonValidations.validateImageDeployments(deployment_type)){
			searchImagesResponse.error = "Incorrect deployment_type. Valid types are BareMetal or VM";
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(searchImagesResponse).build();
		}
		
		searchImagesRequest.deploymentType = deployment_type;
		 searchImagesResponse = imageService
				.searchImages(searchImagesRequest);
		return Response.ok(searchImagesResponse).build();

	}

	/**
	 * This method provides image details if image corresponding to the imageid
	 * exists. In case of baremetallive it also returns host and user name along
	 * with other image details. if image corresponding to imageId doesn't exist
	 * it responds with 404 No found
	 * 
	 * 
	 * @mtwContentTypeReturned JSON
	 * @mtwMethodType GET
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/images/465A8B27-7CC8-4A3C-BBBC-26161E3853CD
	 * Input: imageId : 465A8B27-7CC8-4A3C-BBBC-26161E3853CD
	 * Output:
	 * {
	 *       "created_by_user_id": "admin",
	 *       "created_date": "2015-12-21",
	 *       "edited_by_user_id": "admin",
	 *       "edited_date": "2015-12-21",
	 *       "id": "465A8B27-7CC8-4A3C-BBBC-26161E3853CD",
	 *       "image_name": "CIR1.img",
	 *       "image_format": "qcow2",
	 *       "image_deployments": "VM",
	 *       "image_size": 13312,
	 *       "sent": 13312,
	 *       "deleted": false,
	 *       "trust_policy_id": "0e41169f-d2f3-4566-96c7-183d699417fb",
	 *       "uploads_count": 0,
	 *       "policy_name": "CIR1.img",
	 *       "ip_address":"10.35.35.182",
	 *       "username":"root"
	 *       "image_upload_status": "Complete",
	 *       "image_Location": "/mnt/images/"
	 *     }
	 * 
	 * 
	 * 
	 * When image corresponding to imageid do not exist it gives 404 Not Found
	 * </pre>
	 * 
	 * @param imageId
	 *            The id of the image for which the details are requested
	 * @return image details in JSON format
	 * @throws DirectorException
	 */
	@Path("images/{imageId: [0-9a-zA-Z_-]+}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getImageDetails(@PathParam("imageId") String imageId)
			throws DirectorException {
		ImageInfoResponse imageInfoResponse = new ImageInfoResponse();

		if(!ValidationUtil.isValidWithRegex(imageId,RegexPatterns.UUID)){
			imageInfoResponse.error = "Imaged id is empty or not in uuid format";
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(imageInfoResponse).build();
		}
		imageInfoResponse = imageService
				.getImageDetails(imageId);
		if (imageInfoResponse == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		return Response.ok(imageInfoResponse).build();

	}

	/**
	 * Method to mount the image. This call is invoked in all types of
	 * deployment types as we treat SSH hosts as images too. When a host is
	 * added and entry is made in the MW_IMAGE table, but with image_format as
	 * null. For images, the image format is qcow2
	 * 
	 * Mount path for VM and BM image is /mnt/director/<db_image_uuid> . There
	 * would be just one mountpoint per image. If someone tries to mount same
	 * image again, it will throw exception. Once image is mounted,
	 * mw_image->mounted_by_user_id database field will be updated.
	 * 
	 * This method returns a response in all scenarios, even in case of errors
	 * while mounting the image. In the case when the user attempts to mount an
	 * image which is already in use by another user, and error message: Unable
	 * to mount image. Image is already in use by user: <user_name> is thrown
	 * and sent back in the details attribute of the response object and status
	 * as ERROR.
	 * 
	 * In case the case of the user who has mounted the image, because of
	 * inactivity, the session timed out; and the user logs back in. The image
	 * will not be re-mounted.
	 * 
	 * @mtwContentTypeReturned JSON
	 * @mtwMethodType POST
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/rpc/mount-image
	 * Input: {"id" : "465A8B27-7CC8-4A3C-BBBC-26161E3853CD"} 
	 * Output: 
	 * {
	 *   "created_by_user_id": "admin",
	 *   "created_date": 1450636200000,
	 *   "edited_by_user_id": "admin",
	 *   "edited_date": 1450685484685,
	 *   "id": "465A8B27-7CC8-4A3C-BBBC-26161E3853CD",
	 *   "image_name": "CIR1.img",
	 *   "image_format": "qcow2",
	 *   "image_deployments": "VM",
	 *   "image_size": 13312,
	 *   "sent": 13312,
	 *   "mounted_by_user_id": "admin",
	 *   "deleted": false,
	 *   "image_upload_status": "Complete",
	 *   "image_Location": "/mnt/images/"
	 * }
	 * 
	 * 
	 * If the user tries to mount an image which, for some reason, has been removed from the uploaded location, the response will look like :
	 * {"error": "No image found with id: BAA5747D-B2ED-4E7D-A4D5-0256DEE7FBB1"}
	 * 
	 * If user tries to mount deleted image, the response will look like:
	 * {"error":"Cannot launch deleted image"}
	 * 
	 * Mount image with incorrect image id:
	 * {"id":"465A8B27-7CC8-4A3C-BBBC-26161Es3853CD","deleted":false,"error":"Error fetching image:465A8B27-7CC8-4A3C-BBBC-26161Es3853CD"}
	 * </pre>
	 * 
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return MountImageResponse containing the details of the mount process.
	 */
	@Path("rpc/mount-image")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@POST
	public Response mountImage(MountImageRequest mountImage,
			@Context HttpServletRequest httpServletRequest,
			@Context HttpServletResponse httpServletResponse) {
		MountImageResponse mountImageResponse = new MountImageResponse();
		String error=mountImage.validate();
		if (StringUtils.isNotBlank(error)) {
			mountImageResponse.setError(error);
			mountImageResponse.setId(mountImage.id);
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(mountImageResponse).build();
		}
		try {
			ImageInfo fetchImageById = imageService.fetchImageById(mountImage.id);
			if(fetchImageById == null){
				mountImageResponse.setError("Invalid image id provided");
				mountImageResponse.setId(mountImage.id);
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(mountImageResponse).build();

			}
		} catch (DirectorException e1) {
			log.error("Invalid image id", e1);
		}
		log.info("inside mounting image in web service");
		String user = ShiroUtil.subjectUsername();
		log.info("User mounting image : " + user);
		try {
			mountImageResponse = imageService.mountImage(mountImage.id, user);
		} catch (DirectorException e) {
			log.error("Error while Mounting the Image");
			mountImageResponse.error = e.getMessage();
			return Response.ok(mountImageResponse).build();
		}
		return Response.ok(mountImageResponse).build();
	}

	/**
	 * Method to unmount the mounted image.
	 * 
	 * API will first check whether image is mounted by same user or not using
	 * mw_image -> mounted_by_user_id field. If not then, it will throw
	 * exception. Otherwise API should figure out mount point based on image Id
	 * and unmount the image. The default mount path is /mnt/director/UUID
	 * 
	 * As part of the unmount process, the MW_IMAGE.mounted_by_user_id field is
	 * set to NULL again. the unmount process in the service.
	 * 
	 * 
	 * @mtwContentTypeReturned JSON
	 * @mtwMethodType POST
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/rpc/unmount-image
	 * Input: {id : "465A8B27-7CC8-4A3C-BBBC-26161E3853CD"} 
	 * Output: 
	 * {
	 *   "created_by_user_id": "admin",
	 *   "created_date": 1450636200000,
	 *   "edited_by_user_id": "admin",
	 *   "edited_date": 1450685543811,
	 *   "id": "465A8B27-7CC8-4A3C-BBBC-26161E3853CD",
	 *   "image_name": "CIR1.img",
	 *   "image_format": "qcow2",
	 *   "image_deployments": "VM",
	 *   "image_size": 13312,
	 *   "sent": 13312,
	 *   "deleted": false,
	 *   "image_upload_status": "success",
	 *   "image_Location": "/mnt/images/"
	 * }
	 * 
	 * In case of error:
	 * { â€œerrorâ€: â€œerror message â€ }
	 * 
	 * </pre>
	 * 
	 * @param imageId
	 *            Id of the image to be un-mounted
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return UnmountImageResponse containing the details of the unmount
	 * @throws DirectorException
	 */
	@Path("rpc/unmount-image")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@POST
	public Response unMountImage(MountImageRequest unmountimage,
			@Context HttpServletRequest httpServletRequest,
			@Context HttpServletResponse httpServletResponse) {
		String user = ShiroUtil.subjectUsername();
		UnmountImageResponse unmountImageResponse = new UnmountImageResponse();
		String error=unmountimage.validate();
		if (StringUtils.isNotBlank(error)) {
			unmountImageResponse.setError(error);
			unmountImageResponse.setId(unmountimage.id);
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(unmountImageResponse).build();
		}
		try {
			ImageInfo fetchImageById = imageService.fetchImageById(unmountimage.id);
			if(fetchImageById == null){
				unmountImageResponse.setError("Invalid image id provided");
				unmountImageResponse.setId(unmountimage.id);
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(unmountImageResponse).build();

			}
		} catch (DirectorException e1) {
			log.error("Invalid image id", e1);
		}

	
		try {
			unmountImageResponse = imageService.unMountImage(unmountimage.id,
					user);
		} catch (Exception e) {
			unmountImageResponse = new UnmountImageResponse();
			log.error("Error while unmounting image ", e);
			unmountImageResponse.setError(e.getMessage());
			unmountImageResponse.setId(unmountimage.id);
		}
		return Response.ok(unmountImageResponse).build();
	}

	/**
	 * Method called by the tree on Wizard 2/2 screen to find the files in the
	 * mounted image
	 * 
	 * This method is invoked as an ajax call from the tree component of the
	 * policy wizard. All the directory selections, viewing of child nodes,
	 * application of regex is handled by this method.
	 * 
	 * the SearchFilesInImageRequest object contains all the flags to support
	 * above functionalities. For example, in case of regex application, the
	 * dir, include, exclude, include_recursive attributes are utilized.
	 * 
	 * In case of regex reset, dir, reset_regex is utilized.
	 * 
	 * This method expects a policy draft to be existing when the user comes to
	 * this screen. We create a default empty policy draft when a user starts
	 * creating one. We just keep on modifying the Whitelist tag of the xml.
	 * 
	 * The UI library for the tree also sends an "init" parameter when its
	 * loaded the first time. This indicates that the tree needs to pick up he
	 * current selections that might be made by the users and pre-check the tree
	 * items. The init method reads the existing policy draft and creates a list
	 * of files already selected earlier.
	 * 
	 * Depending on the user actions and the corresponding attributed in the
	 * SearchFilesInImageRequest object, the DirectoryAndFileUtil class methods
	 * are invoked to find the files and dirs inside the directory of interest.
	 * 
	 * In cases of regex and "select all" operation where user clicks on the
	 * checkbox next to a directory in order to select all the contents, this
	 * method created a patch as a list of strings and sends it in the
	 * "patch_xml" attribute. Once the UI receives it, it adds it to the current
	 * selections on the UI and then sends back the consolidated patch to the
	 * server
	 * 
	 * 
	 * 
	 * @mtwContentTypeReturned JSON
	 * @mtwMethodType GET
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/images/08EB37D7-2678-495D-B485-59233EB51996/search
	 * Input: QueryPAram : dir=/boot/&recursive=false&files_for_policy=false&init=false&include_recursive=false&reset_regex=false
	 * 
	 * https://10.35.35.133/v1/images/E4770A39-024D-4A2A-989E-7EE1123E8204/search?dir=/&recursive=false&files_for_policy=false&init=true&include_recursive=false&reset_regex=false
	 * 
	 * output: {"tree_content":"<Html containing the nested ul and li tags>", "patch_xml":"<patch><list of add remove tags as per the operation></patch>"}
	 * 
	 * The output tag has the patch_xml set only in case in the following cases of the query parameters:
	 * 1) recursive=true and files_for_policy=true
	 * 2) recursive=true and files_for_policy=false
	 * 3) reset_regex = true
	 * 4) include="<regex expression>" & exclude="<regex expression>" with optional include_regex=true
	 * 
	 * </pre>
	 * 
	 * @param imageId
	 *            Id of the image which is mounted and whose files are being
	 *            browsed
	 * @param uriInfo
	 *            Request containing the options selected by the user on the
	 *            tree. It contains flags like include/exclude flags for regex
	 *            filter, select all flag, init flag for first time load
	 * @return returns HTML representation of the tree and the patch in some
	 *         cases like regex and select all.
	 * 
	 * 
	 * @throws DirectorException
	 */

	@Path("images/{imageId: [0-9a-zA-Z_-]+}/search")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchFilesInImage(
			@PathParam("imageId") String imageId, @Context UriInfo uriInfo) {
		imageService = new ImageServiceImpl();
		SearchFilesInImageRequest searchFilesInImageRequest = new TdaasUtil()
				.mapUriParamsToSearchFilesInImageRequest(uriInfo);
		searchFilesInImageRequest.id = imageId;
		SearchFilesInImageResponse filesInImageResponse = new SearchFilesInImageResponse();
		if(!ValidationUtil.isValidWithRegex(imageId,RegexPatterns.UUID)){
			filesInImageResponse.error = "Imaged id is empty or not in uuid format";
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(filesInImageResponse).build();
		}
		
		try {
			ImageInfo fetchImageById = imageService.fetchImageById(imageId);
			if(fetchImageById == null){
				filesInImageResponse.error = "Invalid image id provided";
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(filesInImageResponse).build();

			}
		} catch (DirectorException e1) {
			log.error("Invalid image id", e1);
		}
		
		String mountPath = TdaasUtil.getMountPath(imageId);
		File f = new File(mountPath);
		if(!f.exists()){
			filesInImageResponse.error = "Image not mounted";
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(filesInImageResponse).build();
		}
		
		try {
			filesInImageResponse = imageService
					.searchFilesInImage(searchFilesInImageRequest);
		} catch (DirectorException e) {
			// TODO Handle Error
			log.error("Error while searching for files in image : " + imageId,
					e);
			try {
				imageService.unMountImage(imageId, null);
			} catch (DirectorException e1) {
				// TODO Handle Error
				log.error("Error while unmounting image  : " + imageId, e);
			}
			filesInImageResponse.error = "Error doing search operation";
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(filesInImageResponse).build();

		}
		String join = StringUtils.join(filesInImageResponse.files, "");
		filesInImageResponse.treeContent = join;

		// return join;
		return Response.ok(filesInImageResponse).build();
	}

	/**
	 * Retrieves list of deployment types - VM and BareMetal are the types
	 * returned as JSON
	 * 
	 * 
	 * @mtwContentTypeReturned JSON
	 * @mtwMethodType GET
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/image-deployments
	 * Input: None
	 * Output: {
	 *   "image_deployments": [
	 *     {
	 *       "name": "VM",
	 *       "display_name": "Virtualized Server"
	 *     },
	 *     {
	 *       "name": "BareMetal",
	 *       "display_name": "Non-Virtualized Server"
	 *     }
	 *   ]
	 * }
	 * </pre>
	 * 
	 * @return list of deployment types
	 */
	@Path("image-deployments")
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ListImageDeploymentsResponse getImageDeployments() {
		return lookupService.getImageDeployments();
	}

	/**
	 * Lookup method to fetch the image formats. Currently we return qcow2, vhd,
	 * vmdk, raw, vdi as JSON
	 * 
	 * @mtwContentTypeReturned JSON
	 * @mtwMethodType GET
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/image-formats
	 * Input: None
	 * Output: {"image_formats": [{"name": "qcow2","display_name": "qcow2"},{"name": "vhd","display_name": "vhd"}
	 * ,{"name": "vmdk","display_name": "vmdk"},{"name": "raw","display_name": "raw"},{"name": "vdi","display_name": "vdi"}]}
	 * </pre>
	 * @return list of image formats
	 */
	@Path("image-formats")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public ListImageFormatsResponse getImageFormats() {
		return lookupService.getImageFormats();
	}

	/**
	 * lookup method to fetch the launch policies. The current launch policies
	 * that are returned are MeasureOnly and MeasureAndEnforce If not
	 * deploymentType is provided, all image launch policies are returned. I the
	 * user wants to fetch launch specific to a deployment type, the possible
	 * values for deployment type are: 1) VM 2) BareMetal
	 * 
	 * 
	 * @return launch policy list
	 * @mtwMethodType GET
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/image-launch-policies
	 * Input: QueryParam String deploymentType=VM
	 * deploymentType can be VM , BareMetal or Docker
	 * Output:
	 * {
	 *   "image_launch_policies": [
	 *     {
	 *       "name": "MeasureOnly",
	 *       "display_name": "Hash Only",
	 *       "image_deployments": [
	 *         "VM",
	 *         "BareMetal"
	 *       ]
	 *     },
	 *     {
	 *       "name": "MeasureAndEnforce",
	 *       "display_name": "Hash and enforce",
	 *       "image_deployments": [
	 *         "VM"
	 *       ]
	 *     },
	 *     {
	 *       "name": "encrypted",
	 *       "display_name": "Encryption",
	 *       "image_deployments": [
	 *         "VM"
	 *       ]
	 *     }
	 *   ]
	 * }
	 * </pre>
	 */
	@Path("image-launch-policies")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getImageLaunchPoliciesList(
			@QueryParam("deploymentType") String deploymentType) {
		ListImageLaunchPolicyResponse imgLaunchPolicy= new ListImageLaunchPolicyResponse();
		if(!CommonValidations.validateImageDeployments(deploymentType)){
			imgLaunchPolicy.error = "Incorrect deployment_type";
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(imgLaunchPolicy).build();
		}
		return Response.ok(lookupService.getImageLaunchPolicies(deploymentType)).build();
	}

	/**
	 * Utility methods
	 * 
	 * @param httpServletRequest
	 * @return
	 */
	protected String getLoginUsername() {
		return ShiroUtil.subjectUsername();

	}

	/**
	 * 
	 * Method lets the user download the policy from the grids page. The user
	 * can visit the grid any time and download the policy. This method looks
	 * into the MW_TRUST_POLICY table and gets the policy string and sends it as
	 * an xml content to the user
	 * 
	 * In case the policy is not found for the image id, HTTP 404 is returned
	 * 
	 * @mtwContentTypeReturned XML
	 * @mtwMethodType GET
	 * @mtwSampleRestCall <pre>
	 *  https://{IP/HOST_NAME}/v1/images/08EB37D7-2678-495D-B485-59233EB51996/downloads/policy
	 * Input: Image id as path param
	 * Output: Content sent as stream
	 * 
	 * </pre>
	 * @mtwContentTypeReturned XML
	 * @mtwMethodType GET
	 * @mtwSampleRestCall <pre>
	 * Input: Image id as path param
	 * Output: Content sent as stream
	 * 
	 * </pre>
	 * @mtwContentTypeReturned XML
	 * @mtwMethodType GET
	 * @mtwSampleRestCall
	 * <pre>
	 * Input: Image id as path param
	 *	Output: Content sent as stream
     *
	 * </pre>
	 * @param imageId
	 *            the image for which the policy is downloaded
	 * @return XML content of the policy
	 * @throws DirectorException
	 */
	@Path("images/{imageId: [0-9a-zA-Z_-]+}/downloads/policy")
	@GET
	@Produces(MediaType.APPLICATION_XML)
	public Response downloadPolicyForImageId(
			@PathParam("imageId") String imageId) {
		TrustPolicy policy = null;
		GenericResponse genericResponse= new GenericResponse();
		if(!ValidationUtil.isValidWithRegex(imageId,RegexPatterns.UUID)){
			genericResponse.error = "Imaged id is empty or not in uuid format";
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(genericResponse).build();
		}
		

		ImageInfo imageInfo=null;
		try {
			imageInfo = imageService.fetchImageById(imageId);
		} catch (DirectorException e1) {
			log.error("Unable to fetch image", e1);
		}
		if(imageInfo == null){
			genericResponse.error = "No image with id : "+imageId+" exists.";
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(genericResponse).build();
		}
		

		
		String trust_policy_id = imageInfo.getTrust_policy_id();
		TrustPolicy trustPolicyByTrustId = imageService.getTrustPolicyByTrustId(trust_policy_id);

		if(trustPolicyByTrustId == null){
			genericResponse.error = "No trust policy exists for image with id : "+imageId+" exists.";
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(genericResponse).build();
		}
		
		

		try {
			policy = imageService.getTrustPolicyByImageId(imageId);
		} catch (DirectorException e) {
			String msg = "Unable to fetch trust policy for image id : "
					+ imageId;
			log.error(msg, e);
			log.info("Returning HTTP 404");
		}
		if (policy == null) {
			return Response.status(Status.NOT_FOUND).build();
		}

		ResponseBuilder response = Response.ok(policy.getTrust_policy());
		response.header("Content-Disposition", "attachment; filename=policy_"
				+ policy.getImgAttributes().getImage_name() + ".xml");
		return response.build();
	}

	/**
	 * 
	 * Method lets the user download the policy and manifest as a tarball from
	 * the grids page. The user can visit the grid any time and download the
	 * policy and manifest as it was created in the wizrd. This method looks
	 * into the MW_TRUST_POLICY table and gets the policy string, creates a
	 * manifest and sends it as an tarball content to the user
	 * 
	 * In case the policy is not found for the image id, HTTP 404 is returned
	 * 
	 * @mtwContentTypeReturned File
	 * @mtwMethodType GET
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/images/08EB37D7-2678-495D-B485-59233EB51996/downloads/policyAndManifest
	 * Input: Image UUID
	 * Output: Content of tarball as stream
	 * </pre>
	 * 
	 * @param imageId
	 *            the image for which the policy and manifest is downloaded
	 * @return TAR ball content of the policy
	 * @throws DirectorException
	 */
	@Path("images/{imageId: [0-9a-zA-Z_-]+}/downloads/policyAndManifest")
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadPolicyAndManifestForImageId(
			@PathParam("imageId") final String imageId) {

		File tarBall;
		GenericResponse genericResponse= new GenericResponse();
		if(!ValidationUtil.isValidWithRegex(imageId,RegexPatterns.UUID)){
			genericResponse.error = "Imaged id is empty or not in uuid format";
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(genericResponse).build();
		}
		

		ImageInfo imageInfo=null;
		try {
			imageInfo = imageService.fetchImageById(imageId);
		} catch (DirectorException e1) {
			log.error("Unable to fetch image", e1);
		}
		if(imageInfo == null){
			genericResponse.error = "No image with id : "+imageId+" exists.";
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(genericResponse).build();
		}
		

		
		String trust_policy_id = imageInfo.getTrust_policy_id();
		TrustPolicy trustPolicyByTrustId = imageService.getTrustPolicyByTrustId(trust_policy_id);

		if(trustPolicyByTrustId == null){
			genericResponse.error = "No trust policy exists for image with id : "+imageId+" exists.";
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(genericResponse).build();
		}
		
		
		
		try {
			tarBall = imageService.createTarballOfPolicyAndManifest(imageId);
		} catch (DirectorException e) {
			log.error("dowload policy and manifest failed", e);
			tarBall = null;
		}
		if (tarBall == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		FileInputStream tarInputStream = null;
		try {
			tarInputStream = new FileInputStream(tarBall) {
				@Override
				public void close() throws IOException {
					log.info("Deleting temporary files "
							+ Constants.TARBALL_PATH + imageId);
					super.close();
					final File dir = new File(Constants.TARBALL_PATH + imageId);
					for (File file : dir.listFiles()) {
						file.delete();
					}
					dir.delete();
				}
			};
		} catch (FileNotFoundException e) {
			log.error("Error while dowloading policy and manifest", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		ResponseBuilder response = Response.ok(tarInputStream);

		response.header("Content-Disposition", "attachment; filename="
				+ tarBall.getName());

		Response downloadResponse = response.build();
		return downloadResponse;
	}

	/**
	 * 
	 * Mark image as deleted. We turn the deleted flag=true in the MW_IMAGE
	 * table
	 * 
	 * @mtwContentTypeReturned JSON
	 * @mtwMethodType DELETE
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/images/08EB37D7-2678-495D-B485-59233EB51996
	 * Input: pass the UUID of the image as path param
	 * Output: {"deleted": true}
	 * In case of error:
	 * {"deleted": false , "error":"Error in deleteImage"}
	 * 
	 * 
	 * </pre>
	 * 
	 * 
	 * @param imageId
	 *            Id of the image to be deleted
	 * @return Response
	 */

	@Path("images/{imageId: [0-9a-zA-Z_-]+}")
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteImage(@PathParam("imageId") String imageId) {
		GenericResponse response = new GenericResponse();
		GenericResponse genericResponse= new GenericResponse();
		if(!ValidationUtil.isValidWithRegex(imageId,RegexPatterns.UUID)){
			genericResponse.error = "Imaged id is empty or not in uuid format";
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(genericResponse).build();
		}
		try {
			if (imageService.fetchImageById(imageId) != null) {
				imageService.deleteImage(imageId);
				response.setDeleted(true);
			} else {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
		} catch (DirectorException e) {
			log.error("Error in deleteImage ", e);
			response.setDeleted(false);
			// /response.setDetails(e.getMessage());
			response.setError("Error in deleteImage");
		}
		return Response.ok(response).build();

	}

	/**
	 * This method adds the host related details provided by the user. The
	 * connection details are used to verify whether connection can be
	 * established with the remote host.
	 * 
	 * 
	 * @mtwContentTypeReturned JSON
	 * @mtwMethodType POST
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/images/host
	 * Input: {"policy_name":"Host_1","ip_address":"10.35.35.182","username":"admin","password":"password","image_id":"","name":"10.35.35.182"}
	 * 
	 * Output: {"deleted":false,"ip_address":"10.35.35.182","username":"root","image_name":"10.35.35.182","image_id":"FAA5AA92-5872-44CD-BBF4-AD3EFB61D7C9"}
	 * 
	 * In case of error:
	 * Input: {"policy_name":"Host_1","ip_address":"","username":"admin","password":"password","image_id":"","name":"10.35.35.182"}
	 * Lets say the user does not provide the IP:
	 * 
	 * {
	 *   "error": "No Ip address provided",
	 *   "deleted": false
	 * }
	 * 
	 * In case of any back end error, the error would contain the error occurred at the backed.
	 * 
	 * </pre>
	 * 
	 * @param sshSettingRequest
	 *            JSON representation of the connection details
	 * @return Response containing the ID of the image created for the host
	 * @throws DirectorException
	 */

	@POST
	@Path("images/host")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addHost(SshSettingRequest sshSettingRequest)
			throws DirectorException {
		SshSettingResponse sshResponse = sshSettingRequest.validate("add");

		if (StringUtils.isNotBlank(sshResponse.getError())) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(sshResponse).build();
		}
		
		
		try {
			sshResponse = imageService.addHost(sshSettingRequest);
		} catch (DirectorException e) {
			log.error("Error while adding shh settings");
			sshResponse.setError(e.getMessage());
		}
		return Response.ok(sshResponse).build();
	}
	
	/**
	 * This method updates the host related details provided by the user. The
	 * connection details are used to verify whether connection can be
	 * established with the remote host. This call acts similar to the POST
	 * call, only difference being it expects an image id for update. If that is
	 * not provided the method returns an error.
	 * 
	 * @mtwContentTypeReturned JSON
	 * @mtwMethodType PUT
	 * @mtwSampleRestCall <pre>
	 * https://{IP/HOST_NAME}/v1/images/host
	 * Input: {"policy_name":"Host_1","ip_address":"10.35.35.182","username":"admin","password":"password","image_id":"FAA5AA92-5872-44CD-BBF4-AD3EFB61D7C9","name":"10.35.35.182"}
	 * 
	 * Output: 
	 * {
	 *   "deleted": false,
	 *   "ip_address": "10.35.35.182",
	 *   "username": "root",
	 *   "image_name": "10.35.35.182",
	 *   "image_id": "FAA5AA92-5872-44CD-BBF4-AD3EFB61D7C9"
	 * }
	 * 
	 * In case of error:
	 * Input: {"policy_name":"Host_1","ip_address":"","username":"admin","password":"password","image_id":"","name":"10.35.35.182"}
	 * Lets say the user does not provide the correct details to connect to the remote host :
	 * 
	 * {
	 *   "error": "Unable to connect to remote host",
	 *   "deleted": false
	 * }
	 * 
	 * In case of any back end error, the error would contain the error occurred at the backed.
	 * 
	 * </pre>
	 * 
	 * @param sshSettingRequest
	 *            JSON representation of the connection details
	 * @return Response containing the ID of the image created for the host
	 * @throws DirectorException
	 */

	@PUT
	@Path("images/host")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateHost(SshSettingRequest sshSettingRequest) {
		SshSettingResponse sshResponse = sshSettingRequest.validate("update");
		if (StringUtils.isNotBlank(sshResponse.getError())) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(sshResponse).build();
		}

		try {
			sshResponse = imageService.updateSshData(sshSettingRequest);
		} catch (DirectorException e) {
			log.error("Error in updateHost");
			sshResponse.setError(e.getMessage());
		}
		return Response.ok(sshResponse).build();

	}
	
	@Path("rpc/docker-save/{image_id: [0-9a-zA-Z_-]+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@POST
	public GenericResponse dockerSave(@PathParam("image_id") String image_id) {
		log.info("performing Docker save  simultaneously");
		String user = getLoginUsername();
		log.info("User perfomig docker save  : " + user);
		GenericResponse monitorStatus = new GenericResponse();
		monitorStatus.status = Constants.SUCCESS;
		try {
			imageService.dockerSave(image_id, user);
		} catch (DirectorException e) {
			log.error("Error while perfomig docker save ");
			monitorStatus.status = Constants.ERROR;
			monitorStatus.details = e.getMessage();
			return monitorStatus;
		}
		return monitorStatus;
	}
	
	@Path("rpc/docker-rmi/{image_id: [0-9a-zA-Z_-]+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@DELETE
	public Response dockerRMI(@PathParam("image_id") String image_id) {
		log.info("performing Docker rmi simultaneously");
		GenericResponse monitorStatus;
		try {
			monitorStatus = imageService.dockerRMI(image_id);
		} catch (DirectorException e) {
			log.error("Error while perfomig docker  rmi");
			monitorStatus = new GenericResponse();
			monitorStatus.error = e.getMessage();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(monitorStatus).build();
		}
		if (monitorStatus == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		return Response.ok(monitorStatus).build();
	}

	@Path("rpc/docker-load")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@POST
	public Response dockerLoad(DockerRequestObject dockerRequestObject) {
		log.info("performing Docker Load ");
		GenericResponse genericResponse = new GenericResponse();
		genericResponse.status = Constants.SUCCESS;
		GenericResponse dockerLoad;
		try {
			dockerLoad = imageService.dockerLoad(dockerRequestObject
					.getImage_id());
		} catch (DirectorException e) {
			log.error("Error while perfomig docker  load");
			genericResponse.error = e.getMessage();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(genericResponse).build();
		}
		if (dockerLoad == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		return Response.ok(genericResponse).build();
	}

	@Path("rpc/docker-tag")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@POST
	public Response dockerTag(DockerRequestObject dockerRequestObject) {
		log.info("performing Docker tag ");
		GenericResponse dockerTag;
		try {
			dockerTag = imageService.dockerTag(
					dockerRequestObject.getImage_id(),
					dockerRequestObject.getRepository(),
					dockerRequestObject.getTag());
		} catch (DirectorException e) {
			log.error("Error while perfomig docker  tag");
			dockerTag = new GenericResponse();
			dockerTag.error = e.getMessage();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(dockerTag).build();
		}
		if (dockerTag == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		return Response.ok(dockerTag).build();
	}
}
