package com.intel.director.api;

import java.util.Calendar;

public class SshSettingInfo extends AuditFields {

    public String ipAddress;
    public String username;
    public SshPassword sshPassword;
    public String name;
    public String id;
    public SshKey ssh_key_id;
    public ImageAttributes image;

    public ImageAttributes getImage() {
	return image;
    }

    public void setImage(ImageAttributes image) {
	this.image = image;
    }

    public SshPassword getSshPassword() {
	return sshPassword;
    }

    public void setSshPassword(SshPassword sshPassword) {
	this.sshPassword = sshPassword;
    }

    public SshKey getSsh_key_id() {
	return ssh_key_id;
    }

    public void setSsh_key_id(SshKey ssh_key_id) {
	this.ssh_key_id = ssh_key_id;
    }

    public SshSettingInfo() {

    }

    public SshSettingInfo(String created_by_user_id, Calendar created_date, String edited_by_user_id,
	    Calendar edited_date, String ipAddress, String username, SshPassword sshPassword, String name,
	    SshKey ssh_key_id, ImageAttributes image) {
	super(created_by_user_id, created_date, edited_by_user_id, edited_date);
	this.ipAddress = ipAddress;
	this.username = username;
	this.sshPassword = sshPassword;
	this.name = name;
	this.ssh_key_id = ssh_key_id;
	this.image = image;
    }

    public String getId() {
	return id;
    }

    public void setId(String id) {
	this.id = id;
    }

    public SshKey getSshKeyId() {
	return ssh_key_id;
    }

    public void setSshKeyId(SshKey ssh_key_id) {
	this.ssh_key_id = ssh_key_id;
    }

    public String getUsername() {
	return username;
    }

    public void setUsername(String username) {
	this.username = username;
    }

    public SshPassword getPassword() {
	return sshPassword;
    }

    public void setPassword(SshPassword ssh_password_id) {
	this.sshPassword = ssh_password_id;
    }

    public String getIpAddress() {
	return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
	this.ipAddress = ipAddress;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    @Override
    public String toString() {
	return "SshSettingInfo [id=" + id + ",username=" + username + ", name=" + name
		+ ",ipAddress=" + ipAddress + "]";
    }

}
