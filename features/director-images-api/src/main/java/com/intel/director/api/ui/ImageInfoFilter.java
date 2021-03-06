package com.intel.director.api.ui;

import java.util.Calendar;

public class ImageInfoFilter extends ImageInfo {

    protected Calendar from_created_date;
    protected Calendar to_created_date;
    protected SearchImageByPolicyCriteria policyCriteria;
    protected SearchImageByUploadCriteria uploadCriteria;

    public ImageInfoFilter() {
        super();
    }

    public SearchImageByPolicyCriteria getPolicyCriteria() {
        return policyCriteria;
    }

    public void setPolicyCriteria(SearchImageByPolicyCriteria policyCriteria) {
        this.policyCriteria = policyCriteria;
    }

    public SearchImageByUploadCriteria getUploadCriteria() {
        return uploadCriteria;
    }

    public void setUploadCriteria(SearchImageByUploadCriteria uploadCriteria) {
        this.uploadCriteria = uploadCriteria;
    }

    public Calendar getFrom_created_date() {
        return from_created_date;
    }

    public void setFrom_created_date(Calendar from_created_date) {
        this.from_created_date = from_created_date;
    }

    public Calendar getTo_created_date() {
        return to_created_date;
    }

    public void setTo_created_date(Calendar to_created_date) {
        this.to_created_date = to_created_date;
    }

	@Override
	public String toString() {
		return "ImageInfoFilter [from_created_date=" + from_created_date
				+ ", to_created_date=" + to_created_date + ", policyCriteria="
				+ policyCriteria + ", uploadCriteria=" + uploadCriteria
				+ ", trust_policy_id=" + trust_policy_id
				+ ", trust_policy_name=" + trust_policy_name
				+ ", trust_policy_draft_name=" + trust_policy_draft_name
				+ ", trust_policy_draft_id=" + trust_policy_draft_id
				+ ", image_uploads_count=" + image_uploads_count
				+ ", policy_uploads_count=" + policy_uploads_count
				+ ", policy_name=" + policy_name + ", id=" + id
				+ ", image_name=" + image_name + ", image_format="
				+ image_format + ", image_deployments=" + image_deployments
				+ ", status=" + status + ", image_size=" + image_size
				+ ", sent=" + sent + ", mounted_by_user_id="
				+ mounted_by_user_id + ", deleted=" + deleted + ", location="
				+ location + ", repository=" + repository + ", tag=" + tag
				+ ", created_by_user_id=" + created_by_user_id
				+ ", created_date=" + created_date + ", edited_by_user_id="
				+ edited_by_user_id + ", edited_date=" + edited_date + "]";
	}



}
