package io.minio.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class UserInfo {

    public static final String STATUS_ENABLED = "enabled";
    public static final String STATUS_DISABLED = "disabled";

    @JsonProperty("secretKey")
    private String secretKey;

    @JsonProperty("policyName")
    private String policyName;

    @JsonProperty("memberOf")
    private List<String> memberOf;

    @JsonProperty("status")
    private String status;


    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public List<String> getMemberOf() {
        return memberOf;
    }

    public void setMemberOf(List<String> memberOf) {
        this.memberOf = memberOf;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
