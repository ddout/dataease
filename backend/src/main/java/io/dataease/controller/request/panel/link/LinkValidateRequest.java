package io.dataease.controller.request.panel.link;

import lombok.Data;

import java.io.Serializable;

@Data
public class LinkValidateRequest implements Serializable {

    private String link;
    private String user;
    //auto2-code认证
    private String cid;
    private String rcode;
}
