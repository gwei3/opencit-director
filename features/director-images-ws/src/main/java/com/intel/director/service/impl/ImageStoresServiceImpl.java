package com.intel.director.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.intel.director.api.ConnectorCompositeItem;
import com.intel.director.api.ConnectorKey;
import com.intel.director.api.ConnectorProperties;
import com.intel.director.api.GenericDeleteResponse;
import com.intel.director.api.GenericResponse;
import com.intel.director.api.ImageStoreDetailsTransferObject;
import com.intel.director.api.ImageStoreFilter;
import com.intel.director.api.ImageStoreTransferObject;
import com.intel.director.common.Constants;
import com.intel.director.common.exception.DirectorException;
import com.intel.director.service.ImageStoresService;
import com.intel.director.store.StoreManager;
import com.intel.director.store.StoreManagerFactory;
import com.intel.director.store.exception.StoreException;
import com.intel.director.store.util.ImageStorePasswordUtil;
import com.intel.director.util.I18Util;
import com.intel.mtwilson.director.db.exception.DbException;
import com.intel.mtwilson.director.dbservice.DbServiceImpl;
import com.intel.mtwilson.director.dbservice.IPersistService;

public class ImageStoresServiceImpl implements ImageStoresService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImageStoresServiceImpl.class);

    private static final String PLACE_HOLDER_BUNDLE = "ImageStoreKeysPlaceHolder";
    private IPersistService imagePersistenceManager;

    public ImageStoresServiceImpl() {
	imagePersistenceManager = new DbServiceImpl();
    }

    private ConnectorKey[] propertiesForConnector(String connector) {
	ConnectorKey returnString[] = null;
	switch (connector) {
	case Constants.CONNECTOR_DOCKERHUB:
	    returnString = ConnectorProperties.DOCKER.getProperties();
	    break;
	case Constants.CONNECTOR_GLANCE:
	    returnString = ConnectorProperties.GLANCE.getProperties();
	    break;
	/*
	 * case Constants.CONNECTOR_SWIFT: returnString =
	 * ConnectorProperties.SWIFT.getProperties(); break;
	 */
	}
	return returnString;
    }
    
    
    private ConnectorCompositeItem[] compositeItemsForConnector(String connector) {
    	ConnectorCompositeItem returnString[] = null;
    	switch (connector) {
    	case Constants.CONNECTOR_DOCKERHUB:
    	    returnString = ConnectorProperties.DOCKER.getConnectorCompositeItem();
    	    break;
    	case Constants.CONNECTOR_GLANCE:
    	    returnString = ConnectorProperties.GLANCE.getConnectorCompositeItem();
    	    break;
    	/*
    	 * case Constants.CONNECTOR_SWIFT: returnString =
    	 * ConnectorProperties.SWIFT.getProperties(); break;
    	 */
    	}
    	return returnString;
        }

    @Override
    public ImageStoreTransferObject createImageStore(ImageStoreTransferObject imageStoreTransferObject)
	    throws DirectorException {
	ImageStoreTransferObject savedImageStore;
	Collection<ImageStoreDetailsTransferObject> image_store_details_final = new ArrayList<ImageStoreDetailsTransferObject>();

	ConnectorKey[] propertiesForConnector = propertiesForConnector(imageStoreTransferObject.getConnector());
	ConnectorCompositeItem[] compositeItems=compositeItemsForConnector(imageStoreTransferObject.getConnector());
	List<ConnectorKey> listOfPropertiesForConnector = new ArrayList<ConnectorKey>(
		Arrays.asList(propertiesForConnector));
	///List<ConnectorKey> compositeItemList = new ArrayList<ConnectorKey>();
	Map<String, ConnectorKey> keyToConnectorPropertyMap = new HashMap<>(listOfPropertiesForConnector.size());
	for (ConnectorKey connectorKey : listOfPropertiesForConnector) {
	    keyToConnectorPropertyMap.put(connectorKey.getKey(), connectorKey);
	}
        
        if(compositeItems == null){
            log.debug("Connector composite items list is empty");
            throw new DirectorException("Composite items for Connector returned an empty list");
        }
        
	for(ConnectorCompositeItem item : compositeItems){
		listOfPropertiesForConnector.add(new ConnectorKey(8,item.getKey()));
	}
	boolean updateWithEncryptedPassword = false;
	if (imageStoreTransferObject.getImage_store_details() != null
		&& imageStoreTransferObject.getImage_store_details().size() != 0) {
	    Collection<ImageStoreDetailsTransferObject> image_store_details = imageStoreTransferObject
		    .getImage_store_details();
	    for (ImageStoreDetailsTransferObject imageStoreDetailsTransferObject : image_store_details) {
		String key = imageStoreDetailsTransferObject.getKey();
		if (keyToConnectorPropertyMap.containsKey(key)) {
		    listOfPropertiesForConnector.remove(keyToConnectorPropertyMap.get(key));
		}
		updateWithEncryptedPassword = true;
		image_store_details_final.add(imageStoreDetailsTransferObject);
	    }
	}
	for (ConnectorKey props : listOfPropertiesForConnector) {
	    ImageStoreDetailsTransferObject storeDetailsTransferObject = new ImageStoreDetailsTransferObject();
	    storeDetailsTransferObject.setKey(props.getKey());
	    storeDetailsTransferObject.setValue(null);
	    image_store_details_final.add(storeDetailsTransferObject);
	}
	
	

	imageStoreTransferObject.setImage_store_details(image_store_details_final);

	try {
	    savedImageStore = imagePersistenceManager.saveImageStore(imageStoreTransferObject);
	   
	    // If the user has provided the whole Image Store configuration with
	    // the details,
	    // We want to encrypt the password and save it for which we need the
	    // id of the password field
	    // So once we save the store, we need to update it
	    if (updateWithEncryptedPassword) {
		ImageStoreDetailsTransferObject passwordConfiguration = savedImageStore.fetchPasswordConfiguration();
		ImageStorePasswordUtil imageStorePasswordUtil = new ImageStorePasswordUtil(passwordConfiguration.id);

		if (StringUtils.isNotBlank(passwordConfiguration.getValue())) {
		    String passwordForImageStore = imageStorePasswordUtil
			    .encryptPasswordForImageStore(passwordConfiguration.getValue());
		    passwordConfiguration.setValue(passwordForImageStore);
		}

		imagePersistenceManager.updateImageStore(savedImageStore);

	    }
		Collection<ImageStoreDetailsTransferObject> detailsObjectCorrectList= new ArrayList<ImageStoreDetailsTransferObject>();
	    if (savedImageStore != null) {
	    	///log.debug("createImageStore ,Before removal compositeItems::"+compositeItems);
	    
	    	for (ImageStoreDetailsTransferObject detailsTransferObject : savedImageStore.image_store_details) {
	    		boolean add=true;
	    		for(ConnectorCompositeItem item : compositeItems){
	    			if(item.getKey().equals(detailsTransferObject.getKey())){
	    				item.setValue(detailsTransferObject.getValue());
	    				item.setPlaceholder(I18Util.format(detailsTransferObject.getKey(), PLACE_HOLDER_BUNDLE));
	    				item.setId(detailsTransferObject.getId());
	    				///savedImageStore.image_store_details.remove(detailsTransferObject);	
	    				add=false;
	    			}
	    		}
                        
	    		if(add){
	    			detailsObjectCorrectList.add(detailsTransferObject);
	    		}
				
			
			}
	    	
	      
	    savedImageStore.image_store_details=detailsObjectCorrectList;
	    	log.debug(" createImageStore, After removal compositeItems::"+compositeItems);
		for (ImageStoreDetailsTransferObject detailsTransferObject : savedImageStore.image_store_details) {
			
		    detailsTransferObject.setKeyDisplayValue(I18Util.format(detailsTransferObject.getKey()));
		    detailsTransferObject
			    .setPlaceHolderValue(I18Util.format(detailsTransferObject.getKey(), PLACE_HOLDER_BUNDLE));
		}
		
		savedImageStore.setConnectorCompositeItemsList(Arrays.asList(compositeItems));
		
	    }
	} catch (DbException e) {
	    log.error("Error in creating ImageStore", e);
	    throw new DirectorException("Error in creating ImageStore", e);
	}
	return savedImageStore;
    }

    @Override
    public ImageStoreTransferObject getImageStoreById(String imageStoreId) throws DirectorException {

	ImageStoreTransferObject fetchImageStorebyId;
	try {
	    fetchImageStorebyId = imagePersistenceManager.fetchImageStorebyId(imageStoreId);
	    if (fetchImageStorebyId == null) {
		return null;
	    }

	  
	} catch (DbException e) {
	    log.error("Error in fetching ImageStore :: " + imageStoreId);
	    throw new DirectorException("Error in fetching ImageStore :: " + imageStoreId, e);
	}
	  return populateCompositeItemInStore(fetchImageStorebyId);
    }

    public ImageStoreTransferObject populateCompositeItemInStore(ImageStoreTransferObject imageStore) throws DirectorException{
    	Collection<ImageStoreDetailsTransferObject> detailsObjectCorrectList= new ArrayList<ImageStoreDetailsTransferObject>();
	    ConnectorCompositeItem[] compositeItems=compositeItemsForConnector(imageStore.getConnector());
	    
		for (ImageStoreDetailsTransferObject detailsTransferObject : imageStore.image_store_details) {
    		boolean add=true;
                
                if(compositeItems == null){
                    log.debug("Connector composite items list is empty");
                    throw new DirectorException("Composite items for Connector returned an empty list");
                }
    		for(ConnectorCompositeItem item : compositeItems){
    			if(item.getKey().equals(detailsTransferObject.getKey())){
    				item.setValue(detailsTransferObject.getValue());
    				item.setPlaceholder(I18Util.format(detailsTransferObject.getKey(), PLACE_HOLDER_BUNDLE));
    				item.setId(detailsTransferObject.getId());
    				///savedImageStore.image_store_details.remove(detailsTransferObject);	
    				add=false;
    			}
    		}
    		if(add){
    			detailsObjectCorrectList.add(detailsTransferObject);
    		}
			
		
		}
		imageStore.setImage_store_details(detailsObjectCorrectList);
		imageStore.setConnectorCompositeItemsList(Arrays.asList(compositeItems));
	    for (ImageStoreDetailsTransferObject detailsTransferObject : imageStore.image_store_details) {
		detailsTransferObject.setKeyDisplayValue(I18Util.format(detailsTransferObject.getKey()));
		detailsTransferObject
			.setPlaceHolderValue(I18Util.format(detailsTransferObject.getKey(), PLACE_HOLDER_BUNDLE));
	    }
	
	return imageStore;
    }
    
    
    @Override
    public List<ImageStoreTransferObject> getImageStores(ImageStoreFilter imageStoreFilter) throws DirectorException {
    	List<ImageStoreTransferObject> imageStoreConvertedList = new ArrayList<ImageStoreTransferObject>(); 	
	List<ImageStoreTransferObject> fetchedImageStoreList;
	try {
	    fetchedImageStoreList = imagePersistenceManager.fetchImageStores(imageStoreFilter);
	    
	} catch (DbException e) {
	    log.error("Error in fetching ImageStores", e);
	    throw new DirectorException("Error in fetching ImageStores", e);
	}
	for(ImageStoreTransferObject storeTransferObject : fetchedImageStoreList){
		
		ImageStoreTransferObject obj=populateCompositeItemInStore(storeTransferObject);	
		imageStoreConvertedList.add(obj);
	}
	return imageStoreConvertedList;
    }

    @Override
    public GenericDeleteResponse deleteImageStore(String imageStoreId) throws DirectorException {
	ImageStoreTransferObject fetchImageStorebyId;
	try {
	    fetchImageStorebyId = imagePersistenceManager.fetchImageStorebyId(imageStoreId);

	} catch (DbException e) {
	    log.error("Error in fetching ImageStore @ deleteImageStore", e);
	    throw new DirectorException("Error in fetching ImageStore @ deleteImageStore", e);
	}
	if (fetchImageStorebyId == null) {
	    return null;
	}
	fetchImageStorebyId.setDeleted(true);
	try {
	    imagePersistenceManager.updateImageStore(fetchImageStorebyId);
	} catch (DbException e) {
	    log.error("Error in updating ImageStore @ deleteImageStore", e);
	    throw new DirectorException("Error in updating ImageStore @ deleteImageStore", e);
	}
	return new GenericDeleteResponse();
    }

    @Override
    public ImageStoreTransferObject updateImageStore(ImageStoreTransferObject imageStoreTransferObject)
	    throws DirectorException {
	try {

	    imagePersistenceManager.updateImageStore(imageStoreTransferObject);
	} catch (DbException e) {
	    log.error("Error in Updating ImageStore @ updateImageStore", e);
	    throw new DirectorException("Error in Updating ImageStore @ updateImageStore", e);
	}
	return imageStoreTransferObject;
    }

    @Override
    public boolean doesImageStoreNameExist(String name, String imageStoreId) throws DirectorException {
	List<ImageStoreTransferObject> fetchImageStores;
	try {
	    fetchImageStores = imagePersistenceManager.fetchImageStores(null);
	} catch (DbException e) {
	    log.error("Error in Fetching ImageStore @ doesImageStoreNameExist", e);
	    throw new DirectorException("Error in Fetching ImageStore @ doesImageStoreNameExist", e);
	}
	for (ImageStoreTransferObject imageStoreTO : fetchImageStores) {
	    if (!imageStoreTO.deleted && imageStoreTO.name.equalsIgnoreCase(name)
		    && !imageStoreTO.id.equalsIgnoreCase(imageStoreId)) {
		return true;
	    }
	}
	return false;
    }

    @Override
    public boolean validateConnectorArtifacts(String[] artifact_types, String connector) {
	boolean isValidated = true;
	switch (connector) {
	/*case Constants.CONNECTOR_DOCKERHUB:

	    for (String artifact : artifact_types) {
		Map<String, String> supported_artifacts = ConnectorProperties.DOCKER.getSupported_artifacts();
		if (!supported_artifacts.containsKey(artifact)) {
		    isValidated = false;
		}
	    }
	    return isValidated;*/

	case Constants.CONNECTOR_GLANCE:

	    for (String artifact : artifact_types) {
		Map<String, String> supported_artifacts = ConnectorProperties.GLANCE.getSupported_artifacts();
		if (!supported_artifacts.containsKey(artifact)) {
		    isValidated = false;
		}
	    }
	    return isValidated;

	/*
	 * case Constants.CONNECTOR_SWIFT:
	 * 
	 * for (String artifact : artifact_types) { Map<String, String>
	 * supported_artifacts = ConnectorProperties.SWIFT
	 * .getSupported_artifacts(); if
	 * (!supported_artifacts.containsKey(artifact)) { isValidated = false; }
	 * } return isValidated;
	 */

	default:
	    return false;
	}
    }

    @Override
    public void validateImageStore(ImageStoreTransferObject imageStoreTransferObject) throws DirectorException {
	StoreManager manager;
	try {
	    manager = StoreManagerFactory.getStoreManager(imageStoreTransferObject);
	} catch (StoreException e) {
	    log.error("Error in Initializing ImageStore @ validateImageStore", e);
	    throw new DirectorException(e.getMessage());
	}
	try {
	    GenericResponse validate = manager.validate();
	    if (validate.getError() != null) {
		throw new DirectorException(validate.getError());
	    }
	} catch (StoreException e) {
	    log.error(e.getMessage());
	    throw new DirectorException(e.getMessage());
	}
    }
    
    
    @Override
    public void validateImageStore(String imageStoreId) throws DirectorException {
	StoreManager manager;
	try {
	    manager = StoreManagerFactory.getStoreManager(imageStoreId);
	} catch (StoreException e) {
	    log.error("Error in Initializing ImageStore @ validateImageStore", e);
	    throw new DirectorException(e.getMessage());
	}
	try {
	    GenericResponse validate = manager.validate();
	    if (validate.getError() != null) {
		throw new DirectorException(validate.getError());
	    }
	} catch (StoreException e) {
	    log.error(e.getMessage());
	    throw new DirectorException(e.getMessage());
	}
    }

}