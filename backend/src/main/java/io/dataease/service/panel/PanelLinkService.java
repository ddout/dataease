package io.dataease.service.panel;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import io.dataease.auth.config.RsaProperties;
import io.dataease.auth.util.JWTUtils;
import io.dataease.auth.util.RsaUtil;
import io.dataease.commons.constants.SysLogConstants;
import io.dataease.commons.utils.AuthUtils;
import io.dataease.commons.utils.CodingUtil;
import io.dataease.commons.utils.DeLogUtils;
import io.dataease.commons.utils.LogUtil;
import io.dataease.commons.utils.ServletUtils;
import io.dataease.controller.request.panel.link.EnablePwdRequest;
import io.dataease.controller.request.panel.link.LinkRequest;
import io.dataease.controller.request.panel.link.OverTimeRequest;
import io.dataease.controller.request.panel.link.PasswordRequest;
import io.dataease.dto.panel.link.GenerateDto;
import io.dataease.plugins.common.base.domain.*;
import io.dataease.plugins.common.base.mapper.PanelGroupMapper;
import io.dataease.plugins.common.base.mapper.PanelLinkMapper;
import io.dataease.plugins.common.base.mapper.PanelLinkMappingMapper;
import io.dataease.plugins.common.base.mapper.SysAuthAppClientMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.dataease.ext.ExtPanelLinkMapper;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Service
public class PanelLinkService {

    private static final String BASEURL = "/link.html?link=";
    private static final String USERPARAM = "&user=";
    private static final String SHORT_URL_PREFIX = "/link/";

    @Value("${server.servlet.context-path:#{null}}")
    private String contextPath;

    @Resource
    private PanelLinkMapper mapper;
    @Resource
    private PanelGroupMapper panelGroupMapper;
    @Resource
    private ExtPanelLinkMapper extPanelLinkMapper;
    @Resource
    private PanelLinkMappingMapper panelLinkMappingMapper;

    @Resource
    private SysAuthAppClientMapper sysAuthAppClientMapper;

    @Transactional
    public void changeValid(LinkRequest request) {
        PanelLink po = new PanelLink();
        po.setResourceId(request.getResourceId());
        po.setValid(request.isValid());
        Long userId = AuthUtils.getUser().getUserId();
        mapper.updateByExampleSelective(po, example(request.getResourceId(), userId));

        PanelLinkMappingExample example = new PanelLinkMappingExample();
        example.createCriteria().andResourceIdEqualTo(request.getResourceId()).andUserIdEqualTo(userId);
        PanelLinkMapping mapping = new PanelLinkMapping();
        mapping.setUuid(CodingUtil.shortUuid());
        panelLinkMappingMapper.updateByExampleSelective(mapping, example);
        PanelGroupWithBLOBs panel = panelGroupMapper.selectByPrimaryKey(request.getResourceId());

        SysLogConstants.OPERATE_TYPE operateType = SysLogConstants.OPERATE_TYPE.CREATELINK;
        if (!request.isValid()) {
            operateType = SysLogConstants.OPERATE_TYPE.DELETELINK;
        }
        DeLogUtils.save(operateType, SysLogConstants.SOURCE_TYPE.PANEL, panel.getId(), panel.getPid(), null, null) ;
    }

    private PanelLinkExample example(String panelLinkId, Long userId) {
        PanelLinkExample example = new PanelLinkExample();
        example.createCriteria().andResourceIdEqualTo(panelLinkId).andUserIdEqualTo(userId);
        return example;
    }

    public void changeEnablePwd(EnablePwdRequest request) {
        PanelLink po = new PanelLink();
        po.setResourceId(request.getResourceId());
        po.setEnablePwd(request.isEnablePwd());
        mapper.updateByExampleSelective(po, example(request.getResourceId(), AuthUtils.getUser().getUserId()));
        PanelGroupWithBLOBs panel = panelGroupMapper.selectByPrimaryKey(request.getResourceId());
        DeLogUtils.save(SysLogConstants.OPERATE_TYPE.MODIFYLINK, SysLogConstants.SOURCE_TYPE.PANEL, panel.getId(), panel.getPid(), null, null) ;
    }

    public void password(PasswordRequest request) {
        PanelLink po = new PanelLink();
        po.setResourceId(request.getResourceId());
        po.setPwd(request.getPassword());
        mapper.updateByExampleSelective(po, example(request.getResourceId(), AuthUtils.getUser().getUserId()));


        PanelGroupWithBLOBs panel = panelGroupMapper.selectByPrimaryKey(request.getResourceId());
        DeLogUtils.save(SysLogConstants.OPERATE_TYPE.MODIFYLINK, SysLogConstants.SOURCE_TYPE.PANEL, panel.getId(), panel.getPid(), null, null) ;
    }

    public void overTime(OverTimeRequest request) {
        request.setUserId(AuthUtils.getUser().getUserId());
        extPanelLinkMapper.updateOverTime(request);
        PanelGroupWithBLOBs panel = panelGroupMapper.selectByPrimaryKey(request.getResourceId());
        DeLogUtils.save(SysLogConstants.OPERATE_TYPE.MODIFYLINK, SysLogConstants.SOURCE_TYPE.PANEL, panel.getId(), panel.getPid(), null, null) ;
    }

    private PanelLink findOne(String resourceId) {
        PanelLinkExample example = new PanelLinkExample();
        example.createCriteria().andResourceIdEqualTo(resourceId).andUserIdIsNull();
        List<PanelLink> list = mapper.selectByExample(example);
        return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
    }

    public PanelLink findOne(String resourceId, Long userId) {
        if (userId == null) {
            return findOne(resourceId);
        }
        List<PanelLink> panelLinks = mapper.selectByExample(example(resourceId, userId));
        if (CollectionUtils.isNotEmpty(panelLinks)) {
            return panelLinks.get(0);
        } else {
            return null;
        }
    }

    @Transactional
    public GenerateDto currentGenerate(String resourceId) {
        PanelLink one = findOne(resourceId, AuthUtils.getUser().getUserId());
        if (ObjectUtils.isEmpty(one)) {
            one = new PanelLink();
            one.setPwd(null);
            one.setResourceId(resourceId);
            one.setValid(false);
            one.setUserId(AuthUtils.getUser().getUserId());
            one.setEnablePwd(false);
            mapper.insert(one);
        }

        PanelLinkMappingExample example = new PanelLinkMappingExample();
        example.createCriteria().andResourceIdEqualTo(resourceId).andUserIdEqualTo(AuthUtils.getUser().getUserId());
        List<PanelLinkMapping> mappings = panelLinkMappingMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(mappings)) {
            PanelLinkMapping mapping = new PanelLinkMapping();
            mapping.setResourceId(resourceId);
            mapping.setUserId(AuthUtils.getUser().getUserId());
            mapping.setUuid(CodingUtil.shortUuid());
            panelLinkMappingMapper.insert(mapping);
        }
        return convertDto(one);
    }

    public void deleteByResourceId(String resourceId) {
        PanelLinkExample example = new PanelLinkExample();
        example.createCriteria().andResourceIdEqualTo(resourceId);
        mapper.deleteByExample(example);

        PanelLinkMappingExample mappingExample = new PanelLinkMappingExample();
        mappingExample.createCriteria().andResourceIdEqualTo(resourceId);
        panelLinkMappingMapper.deleteByExample(mappingExample);
    }

    public String decryptParam(String text) throws Exception {
        return RsaUtil.decryptByPrivateKey(RsaProperties.privateKey, text);
    }

    // 使用公钥加密
    private String encrypt(String sourceValue) {
        try {
            return RsaUtil.encryptByPublicKey(RsaProperties.publicKey, sourceValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String buildLinkParam(PanelLink link) {
        String linkParam = encrypt(link.getResourceId());
        if (link.getUserId() != null) {
            linkParam = linkParam + USERPARAM + encrypt(link.getUserId().toString());
        }
        return linkParam;
    }

    private GenerateDto convertDto(PanelLink link) {
        GenerateDto result = new GenerateDto();
        result.setValid(link.getValid());
        result.setEnablePwd(link.getEnablePwd());
        result.setPwd(link.getPwd());
        result.setUri(BASEURL + buildLinkParam(link));
        result.setOverTime(link.getOverTime());
        return result;
    }

    // 验证请求头部携带的信息 如果正确说明通过密码验证 否则没有通过
    public Boolean validateHeads(PanelLink panelLink) throws Exception {
        HttpServletRequest request = ServletUtils.request();
        String token = request.getHeader("LINK-PWD-TOKEN");
        if (!panelLink.getEnablePwd() || StringUtils.isEmpty(token) || StringUtils.equals("undefined", token)
                || StringUtils.equals("null", token)) {
            String resourceId = panelLink.getResourceId();
            String pwd = "dataease";
            String tk = JWTUtils.signLink(resourceId, panelLink.getUserId(), pwd);
            HttpServletResponse httpServletResponse = ServletUtils.response();
            httpServletResponse.addHeader("Access-Control-Expose-Headers", "LINK-PWD-TOKEN");
            httpServletResponse.setHeader("LINK-PWD-TOKEN", tk);
            return false;
        }
        if (StringUtils.isEmpty(panelLink.getPwd()))
            return false;
        return JWTUtils.verifyLink(token, panelLink.getResourceId(), panelLink.getUserId(), panelLink.getPwd());
    }

    // 验证链接是否过期
    public Boolean isExpire(PanelLink panelLink) {
        if (ObjectUtils.isEmpty(panelLink.getOverTime())) {
            return false;
        }
        return System.currentTimeMillis() > panelLink.getOverTime();
    }

    public boolean validatePwd(PasswordRequest request) throws Exception {
        String password = request.getPassword();
        String resourceId = request.getResourceId();
        PanelLink one = findOne(resourceId, request.getUser());
        String pwd = one.getPwd();
        boolean pass = StringUtils.equals(pwd, password);
        if (pass) {
            String token = JWTUtils.signLink(resourceId, request.getUser(), password);
            HttpServletResponse httpServletResponse = ServletUtils.response();
            httpServletResponse.addHeader("Access-Control-Expose-Headers", "LINK-PWD-TOKEN");
            httpServletResponse.setHeader("LINK-PWD-TOKEN", token);
        }
        return pass;
    }

    /**
     * auto2-code认证,远程接口验证合法性逻辑
     * @param one 被访问资源
     * @param cid 客户端id-clientid
     * @param rcode 客户端传递的临时code
     * @return
     * @throws Exception
     */
    public boolean validateAuth(PanelLink one , String cid, String rcode) throws Exception {
        boolean pass = false;
        //
        try{
            /**
             * TODO 需要增加改cid是否有权限访问本资源的配置校验(即应用的数据看板授权配置及校验)
             */
            /*
                1、查询配置数据中，cid对应的回调地址
                a、用cid去查，同时满足启用、在有效期内两个条件
                b、获取到回调地址
                2、用回调地址请求，拿到验证结果
            */
            //
            SysAuthAppClient sac = sysAuthAppClientMapper.selectEnabledByPrimaryKey(cid);
            if(null == sac){
                return false;
            }
            //
            String callbackUrl = sac.getCallbackUrl();
            if(StringUtils.isEmpty(callbackUrl)){
                return false;
            }
            //替换cid和rcode的变量
//            String url = "https://dev.usemock.com/6375ff3ecc89b05b04a4b136/auth/back/{cid}/{rcode}";
            String url = callbackUrl.replaceAll("\\{cid}",cid).replaceAll("\\{rcode}",rcode);
            //请求
            String result = HttpRequest.get(url).setReadTimeout(3000).execute().body();
            //
            LogUtil.debug(result);
            //返回参数校验
            result = JSONUtil.escape(result);
            JSONObject resultObj = JSONObject.parseObject(result);
            //
            String vResult = resultObj.getString("result");
            String vCode = resultObj.getString("code");
            if("200".equals(vCode) && "ok".equals(vResult)){
                //验证成功，写入header
                pass = true;
                String token = JWTUtils.signLink(one.getResourceId(), one.getUserId(), one.getPwd());
                HttpServletResponse httpServletResponse = ServletUtils.response();
                httpServletResponse.addHeader("Access-Control-Expose-Headers", "LINK-PWD-TOKEN");
                httpServletResponse.setHeader("LINK-PWD-TOKEN", token);
                //
            }
        } catch (Exception e) {
            LogUtil.error(e.getMessage(),e);
        }
        return pass;
    }

    public PanelGroupWithBLOBs resourceInfo(String resourceId) {
        return panelGroupMapper.selectByPrimaryKey(resourceId);
    }

    public String getShortUrl(String resourceId) {
        PanelLinkMappingExample example = new PanelLinkMappingExample();
        example.createCriteria().andResourceIdEqualTo(resourceId).andUserIdEqualTo(AuthUtils.getUser().getUserId());
        List<PanelLinkMapping> mappings = panelLinkMappingMapper.selectByExample(example);
        PanelLinkMapping mapping = mappings.get(0);
        String uuid = mapping.getUuid();
        return contextPath + SHORT_URL_PREFIX + (StringUtils.isBlank(uuid) ? mapping.getId() : uuid);
    }

    public String getUrlByIndex(Long index) {
        PanelLinkMapping mapping = panelLinkMappingMapper.selectByPrimaryKey(index);

        String resourceId = mapping.getResourceId();
        Long userId = mapping.getUserId();
        PanelLink one = findOne(resourceId, userId);
        if (StringUtils.isNotBlank(mapping.getUuid())) {
            one.setResourceId("error-resource-id");
        }
        return convertDto(one).getUri();
    }

    public String getUrlByUuid(String uuid) {
        PanelLinkMappingExample example = new PanelLinkMappingExample();
        example.createCriteria().andUuidEqualTo(uuid);
        List<PanelLinkMapping> mappings = panelLinkMappingMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(mappings)) {
            PanelLink panelLink = new PanelLink();
            panelLink.setResourceId("error-resource-id");
            return BASEURL + buildLinkParam(panelLink);
        }
        PanelLinkMapping mapping = mappings.get(0);
        String resourceId = mapping.getResourceId();
        Long userId = mapping.getUserId();
        PanelLink one = findOne(resourceId, userId);
        return convertDto(one).getUri();
    }
}
