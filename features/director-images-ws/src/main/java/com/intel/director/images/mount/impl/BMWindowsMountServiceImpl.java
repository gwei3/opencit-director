package com.intel.director.images.mount.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.intel.director.api.ImageAttributes;
import com.intel.director.api.SshSettingInfo;
import com.intel.director.common.DirectorUtil;
import com.intel.director.common.FileUtilityOperation;
import com.intel.director.common.MountImage;
import com.intel.director.common.exception.DirectorException;
import com.intel.mtwilson.director.db.exception.DbException;
import com.intel.mtwilson.director.dbservice.DbServiceImpl;
import com.intel.mtwilson.director.dbservice.IPersistService;

public class BMWindowsMountServiceImpl extends MountServiceImpl {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BMLinuxMountServiceImpl.class);

	public BMWindowsMountServiceImpl(ImageAttributes imageInfo) {
		super(imageInfo);
	}

	@Override
	public int mount() throws DirectorException {
		IPersistService persistService = new DbServiceImpl();
		SshSettingInfo info;
		try {
			info = persistService.fetchSshByImageId(imageInfo.id);
		} catch (DbException e) {
			String msg = "Error fetching ssh settings for BM image";
			log.error(msg, e);
			throw new DirectorException(msg, e);
		}

		String[] partitions = imageInfo.getPartition().split(",");
		List<String> successfulMounts = new ArrayList<String>();
		for (String partition : partitions) {
			String mountDir = mountPath + File.separator + partition.trim();
			File file = new File(mountDir);
			if (!file.exists()) {
				try {
					DirectorUtil.callExec("mkdir -p " + mountDir);
				} catch (IOException e) {
					String msg = "Error creating mount directory " + mountDir;
					log.error(msg);
					throw new DirectorException(msg);
				}
			}
			int mountWindowsRemoteSystem = MountImage.mountWindowsRemoteSystem(info.getIpAddress(), info.getUsername(),
					info.getSshPassword().getKey(), mountDir, partition, new String("0444"), new String("0444"));
			if (mountWindowsRemoteSystem != 0) {
				log.error("Error mounting partition " + mountDir);
				new FileUtilityOperation().deleteFileOrDirectory(file);
			} else {
				successfulMounts.add(mountDir);
			}
		}
		if (successfulMounts.size() == 0) {
			throw new DirectorException("Unable to mount any partition from remote host");
		}
		return 0;
	}

	@Override
	public int unmount() throws DirectorException {
		String[] partitions = imageInfo.getPartition().split(",");
		for (String partition : partitions) {
			int unmountWindowsRemoteSystem = MountImage
					.unmountWindowsRemoteSystem(mountPath + File.separator + partition);
			if (unmountWindowsRemoteSystem != 0) {
				log.error("Error unmounting partition  " + partition);
			}
		}
		return 0;
	}

}
