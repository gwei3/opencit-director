INSERT INTO changelog (ID, APPLIED_AT, DESCRIPTION) VALUES (20120101000553,NOW(),'Windows Drive Letter Addition');

ALTER TABLE MW_IMAGE ADD COLUMN DRIVES VARCHAR(36);

CREATE OR REPLACE VIEW mw_image_info_view AS 
 SELECT mwimg.id,
    mwimg.name,
    mwimg.content_length,
    mwimg.image_format,
    mwimg.image_deployments,
    mwimg.created_by_user_id,
    mwimg.created_date,
    mwimg.edited_date,
    mwimg.edited_by_user_id,
    mwimg.deleted,
    mwimg.location,
    mwimg.mounted_by_user_id,
    mwimg.sent,
    mwimg.status,
	mwimg.tmp_location,
	mwimg.upload_variables_md5,
    mwtrustpolicy.id AS trust_policy_id,
    mwtrustpolicy.name AS trust_policy_name,
    mwpolicydraft.id AS trust_policy_draft_id,
    mwtrustpolicy.edited_by_user_id AS trust_policy_edited_by_user_id,
    mwtrustpolicy.edited_date AS trust_policy_edited_date,
    mwtrustpolicy.created_date AS trust_policy_created_date,
    mwpolicydraft.name AS trust_policy_draft_name,
    mwpolicydraft.edited_by_user_id AS trust_policy_draft_edited_by_user_id,
    mwpolicydraft.edited_date AS trust_policy_draft_edited_date,
    ( SELECT count(*) AS count
           FROM mw_image_upload mwimageupload
          WHERE mwimageupload.image_id::text = mwimg.id::text) AS image_upload_count,
	 ( SELECT count(*) AS count
           FROM mw_policy_upload mwpolicyupload
          WHERE mwpolicyupload.policy_id::text = mwtrustpolicy.id::text) AS policy_upload_count	  ,
	mwimg.repository,
	mwimg.tag,
	mwimg.drives
   FROM mw_image mwimg
	LEFT JOIN mw_trust_policy_info_view mwtrustpolicy ON mwimg.id::text = mwtrustpolicy.image_id::text
    LEFT JOIN mw_trust_policy_draft mwpolicydraft ON mwpolicydraft.id::text = mwimg.trust_policy_draft_id::text;

ALTER TABLE mw_image_info_view
  OWNER TO postgres;
  
INSERT INTO mw_policy_template(id, name, deployment_type, content, active, deployment_type_identifier,policy_type) VALUES ('3', 'Bare Metal (W)', 'BareMetal', '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Manifest DigestAlg="sha256" xmlns="mtwilson:trustdirector:manifest:1.2" ><Dir Path="/C:/Program Files (x86)/Intel/Tbootxm" Include="" FilterType="" Exclude=""/><Dir Path="/C:/Program Files (x86)/Intel/Trustagent" Include="" FilterType="" Exclude=""/><Dir Path="/C:/Program Files (x86)/Intel/Tbootxm/bin" Include="" FilterType="" Exclude=""/><Dir Path="/C:/Program Files (x86)/Intel/Trustagent/bin" Include="" FilterType="" Exclude=""/><Dir Path="/C:/Program Files (x86)/Intel/Trustagent/bootdriver" Include="" FilterType="" Exclude=""/><Dir Path="/C:/Program Files (x86)/Intel/Trustagent/env.d" Include="" FilterType="" Exclude=""/><Dir Path="/C:/Program Files (x86)/Intel/Trustagent/hypertext" Include="" FilterType="" Exclude=""/><Dir Path="/C:/Program Files (x86)/Intel/Trustagent/java" Include="" FilterType="" Exclude=""/><Dir Path="/C:/Program Files (x86)/Intel/Trustagent/var" Include="" FilterType="" Exclude=""/><Dir Path="/C:/Program Files (x86)/Intel/Trustagent/hypertext/WEB-INF" Include="" FilterType="" Exclude=""/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/uninst.exe"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/bin/mtwilson-openstack-node-uninstall.ps1"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/bin/patch-util-win.cmd"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/bin/setup.ps1"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-policyagent-hooks/2014.1/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-policyagent-hooks/2014.1.3/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-policyagent-hooks/2014.1.4/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-policyagent-hooks/2014.1.5/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-policyagent-hooks/2014.1.5-0ubuntu1.2/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-policyagent-hooks/2014.2/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-policyagent-hooks/2014.2.3/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-policyagent-hooks/2015.1.1/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-policyagent-hooks/2015.1.2/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-policyagent-hooks/nt_13.0.0/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-policyagent-hooks/nt_2015.1.0/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-vm-attestation/2014.1/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-vm-attestation/2014.1.3/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-vm-attestation/2014.1.4/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-vm-attestation/2014.1.5/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-vm-attestation/2014.2/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-vm-attestation/2014.2.3/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-vm-attestation/2015.1.1/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-vm-attestation/nt_13.0.0/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Openstack-Extensions/repository/mtwilson-openstack-vm-attestation/nt_2015.1.0/distribution-location.patch"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/uninst.exe"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/BitLocker.exe"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/policyagent-init"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/policyagent.py"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/commons/parse.py"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/commons/parse.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/commons/process_trust_policy.py"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/commons/process_trust_policy.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/commons/utils.py"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/commons/utils.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/commons/__init__.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/encryption/crypt.py"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/encryption/crypt.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/encryption/win_crypt.py"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/encryption/win_crypt.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/encryption/__init__.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/invocation/measure_vm.py"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/invocation/measure_vm.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/invocation/stream.py"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/invocation/stream.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/invocation/vrtm_invoke.py"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/invocation/vrtm_invoke.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/invocation/__init__.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/trustpolicy/trust_policy_retrieval.py"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/trustpolicy/trust_policy_retrieval.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/trustpolicy/trust_store_glance_image_tar.py"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/trustpolicy/trust_store_glance_image_tar.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/trustpolicy/trust_store_swift.py"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/bin/trustpolicy/__init__.pyc"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/logs/bitlockersetup.log"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/scripts/bitlocker_drive_setup.ps1"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/scripts/free_bitlocker_drive.ps1"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/scripts/ps_utility.ps1"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/scripts/unlock_bitlocker_drive.ps1"/><File Path="/C:/Program Files (x86)/Intel/Policyagent/scripts/update_property.ps1"/><File Path="/C:/Program Files (x86)/Intel/Tbootxm/uninst.exe"/><File Path="/C:/Program Files (x86)/Intel/Tbootxm/bin/tbootxm_bootdriver.cat"/><File Path="/C:/Program Files (x86)/Intel/Tbootxm/bin/tbootxm_bootdriver.inf"/><File Path="/C:/Program Files (x86)/Intel/Tbootxm/bin/tbootxm_bootdriver.sys"/><File Path="/C:/Program Files (x86)/Intel/Tbootxm/bin/verifier.exe"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/feature.xml"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java.security"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/readme.txt"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/TAicon.ico"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/trustagent.env"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/TrustAgent.exe"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/TrustAgentTray.exe"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/Uninstall.exe"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/vcredist_x64.exe"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/version"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bin/activiationblob.txt"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bin/agenthandler.cmd"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bin/bindkeyattestation"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bin/getvmmver.cmd"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bin/signkeyattestation"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bin/tagent.cmd"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bin/tasetup.cmd"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bin/taupgrade.cmd"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bin/TpmAtt.dll"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bin/TPMTool.exe"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bin/tpm_signdata.exe"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bin/tpm_unbindaeskey.exe"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bootdriver/citbootdriver.inf"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bootdriver/citbootdriver.sys"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bootdriver/citbootdriversetup.exe"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/bootdriver/WdfCoinstaller01011.dll"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/env.d/java"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/env.d/trustagent"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/env.d/trustagent.version"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/hypertext/index.html"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/hypertext/WEB-INF/web.xml"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/activation-1.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/antlr-2.7.7.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/antlr-runtime-3.5.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/aopalliance-repackaged-2.2.0.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/asm-all-repackaged-2.2.0-b21.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/aspectjrt-1.7.4.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/aspectjtools-1.7.4.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/aspectjweaver-1.6.12.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/bcmail-jdk16-1.46.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/bcprov-jdk15-1.46.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/bcprov-jdk16-1.46.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/cglib-2.2.0-b21.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/commons-beanutils-1.9.2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/commons-codec-1.9.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/commons-collections-3.2.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/commons-compress-1.9.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/commons-configuration-1.9.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/commons-exec-1.2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/commons-httpclient-3.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/commons-io-2.4.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/commons-lang-2.6.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/commons-lang3-3.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/commons-logging-1.1.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/commons-math3-3.2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/commons-pool-1.6.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/esapi-2.0GA.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/FastInfoset-1.2.12.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/guava-14.0.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/hk2-api-2.2.0.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/hk2-locator-2.2.0-b21.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/hk2-utils-2.2.0.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/httpclient-4.3.4.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/httpcore-4.3.2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/istack-commons-runtime-2.16.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jackson-annotations-2.5.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jackson-core-2.5.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jackson-databind-2.5.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jackson-dataformat-xml-2.5.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jackson-dataformat-yaml-2.5.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jackson-jaxrs-base-2.5.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jackson-jaxrs-json-provider-2.5.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jackson-jaxrs-xml-provider-2.5.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jackson-module-jaxb-annotations-2.5.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/javax.annotation-api-1.2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/javax.inject-1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/javax.inject-2.2.0-b21.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/javax.servlet-api-3.1.0.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/javax.ws.rs-api-2.0.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jaxb-api-2.2.7.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jaxb-core-2.2.7-b63.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jaxb-impl-2.2.7-b63.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jcl-over-slf4j-1.6.6.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jersey-client-2.4.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jersey-common-2.4.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jersey-container-servlet-2.4.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jersey-container-servlet-core-2.4.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jersey-media-multipart-2.4.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jersey-server-2.4.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jetty-deploy-9.0.0.v20130308.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jetty-http-9.1.0.RC2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jetty-io-9.1.0.RC2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jetty-security-9.1.0.RC2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jetty-server-9.1.0.RC2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jetty-servlet-9.1.0.RC2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jetty-util-9.1.0.RC2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jetty-webapp-9.1.0.RC2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jetty-webapp-logging-9.0.0.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jetty-xml-9.1.0.RC2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/joda-time-2.3.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jsr173_api-1.0.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/jul-to-slf4j-1.6.6.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/log4j-over-slf4j-1.6.6.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/logback-classic-1.1.3.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/logback-core-1.1.3.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mailapi-1.4.3.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mimepull-1.9.3.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-configuration-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-core-setup-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-crypto-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-extensions-cache-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-http-security-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-http-servlets-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-launcher-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-launcher-api-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-localization-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-password-vault-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-privacyca-client-jaxrs2-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-privacyca-model-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-privacyca-niarl-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-privacyca-tpm-endorsement-client-jaxrs2-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-privacyca-tpm-endorsement-model-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-repository-api-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-rpc-model-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-setup-ext-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-shiro-file-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-shiro-util-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-trustagent-attestation-client-jaxrs2-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-trustagent-attestation-model-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-trustagent-configuration-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-trustagent-console-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-trustagent-model-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-trustagent-privacyca-niarl-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-trustagent-setup-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-trustagent-tpm-tools-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-trustagent-vmquote-xml-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-trustagent-vrtmclient-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-trustagent-ws-v2-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-authz-token-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-classpath-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-codec-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-collection-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-configuration-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-console-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-crypto-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-crypto-jca-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-crypto-key-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-crypto-password-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-exec-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-extensions-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-http-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-i18n-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-io-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-jackson-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-jaxrs2-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-jaxrs2-client-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-jaxrs2-server-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-jersey2-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-jetty9-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-locale-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-net-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-patch-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-pem-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-performance-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-pipe-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-rfc822-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-shiro-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-text-transform-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-tls-policy-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-tree-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-validation-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-util-xml-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-version-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-version-ws-v2-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/mtwilson-webservice-util-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/not-yet-commons-ssl-0.3.9.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/opensaml-2.5.1-1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/openws-1.4.2-1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/osgi-resource-locator-1.0.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/serializer-2.7.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/servlet-api-2.5.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/shiro-aspectj-1.2.2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/shiro-core-1.2.2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/shiro-web-1.2.2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/slf4j-api-1.7.12.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/snakeyaml-1.12.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/ST4-4.0.7.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/stax-api-1.0-2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/stax2-api-3.1.4.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/stringtemplate-3.2.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/trustagent-zip-3.1-SNAPSHOT.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/validation-api-1.1.0.Final.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/velocity-1.5.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/woodstox-core-asl-4.2.0.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/xalan-2.7.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/xercesImpl-2.10.0.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/xml-apis-1.4.01.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/xml-resolver-1.2.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/xmlpull-1.1.3.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/xmlsec-1.4.3.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/xmltooling-1.4.1.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/xpp3_min-1.1.4c.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/java/xstream-1.4.7.jar"/><File Path="/C:/Program Files (x86)/Intel/Trustagent/logs/install.log"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/uninst.exe"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/bin/libxml2.dll"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/bin/log4cpp.dll"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/bin/pthreadVC2.dll"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/bin/verifier.exe"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/bin/vrtmchannel.dll"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/bin/vrtmcore.exe"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/bin/vrtmservice.exe"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/prerequisites/Ext2Fsd-0.62.exe"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/prerequisites/vcredist_10.exe"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/prerequisites/vcredist_13.exe"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/scripts/Mount-EXTVM.ps1"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/scripts/mount_vm_image.sh"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/scripts/preheat-guestmount.sh"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/scripts/vrtm.cmd"/><File Path="/C:/Program Files (x86)/Intel/Vrtm/scripts/vrtmhandler.cmd"/></Manifest>', true, 'W', 'Manifest'); 
 