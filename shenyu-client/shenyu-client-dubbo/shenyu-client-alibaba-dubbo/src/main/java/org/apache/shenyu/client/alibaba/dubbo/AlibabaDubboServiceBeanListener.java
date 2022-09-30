/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.client.alibaba.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.config.spring.ServiceBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.client.core.client.AbstractContextRefreshedEventListener;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.apache.shenyu.client.dubbo.common.dto.DubboRpcExt;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.common.exception.ShenyuException;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.common.utils.IpUtils;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The Alibaba Dubbo ServiceBean Listener.
 */
//@SuppressWarnings("all")
public class AlibabaDubboServiceBeanListener extends AbstractContextRefreshedEventListener<ServiceBean, ShenyuDubboClient> {

    /**
     * Instantiates a new context refreshed event listener.
     *
     * @param clientConfig                   the shenyu dubbo client config
     * @param shenyuClientRegisterRepository the shenyuClientRegisterRepository
     */
    public AlibabaDubboServiceBeanListener(final PropertiesConfig clientConfig,
                                           final ShenyuClientRegisterRepository shenyuClientRegisterRepository) {
        super(clientConfig, shenyuClientRegisterRepository);
    }

    @Override
    protected Map<String, ServiceBean> getBeans(final ApplicationContext context) {
        return context.getBeansOfType(ServiceBean.class);
    }

    @Override
    protected Class<?> getCorrectedClass(final ServiceBean bean) {
        Object refProxy = bean.getRef();
        Class<?> clazz = refProxy.getClass();
        if (AopUtils.isAopProxy(refProxy)) {
            clazz = AopUtils.getTargetClass(refProxy);
        }
        return clazz;
    }

    @Override
    protected URIRegisterDTO buildURIRegisterDTO(final ApplicationContext context,
                                                 final Map<String, ServiceBean> beans) {
        return beans.entrySet().stream().findFirst().map(entry -> {
            final ServiceBean<?> bean = entry.getValue();
            return URIRegisterDTO.builder()
                    .contextPath(getContextPath())
                    .appName(buildAppName(bean))
                    .rpcType(RpcTypeEnum.DUBBO.getName())
                    .host(buildHost())
                    .port(buildPort(bean))
                    .build();
        }).orElse(null);
    }

    private String buildAppName(final ServiceBean<?> serviceBean) {
        String appName = this.getAppName();
        return StringUtils.isBlank(appName) ? serviceBean.getApplication().getName() : appName;
    }

    private String buildHost() {
        final String host = this.getHost();
        return IpUtils.isCompleteHost(host) ? host : IpUtils.getHost(host);
    }

    private int buildPort(final ServiceBean<?> serviceBean) {
        final String port = this.getPort();
        return StringUtils.isBlank(port) || "-1".equals(port) ? serviceBean.getProtocol().getPort() : Integer.parseInt(port);
    }

    @Override
    protected String buildApiSuperPath(final Class<?> clazz,
                                       final ShenyuDubboClient beanShenyuClient) {
        if (Objects.nonNull(beanShenyuClient) && !StringUtils.isBlank(beanShenyuClient.path())) {
            return beanShenyuClient.path();
        }
        return "";
    }

    @Override
    protected Class<ShenyuDubboClient> getAnnotationType() {
        return ShenyuDubboClient.class;
    }

    @Override
    protected void handleClass(final Class<?> clazz,
                               final ServiceBean bean,
                               @NonNull final ShenyuDubboClient beanShenyuClient,
                               final String superPath) {
        Method[] methods = ReflectionUtils.getDeclaredMethods(clazz);
        for (Method method : methods) {
            getPublisher().publishEvent(buildMetaDataDTO(bean, beanShenyuClient, buildApiPath(method, superPath, null), clazz, method));
        }
    }

    @Override
    protected String buildApiPath(final Method method,
                                  final String superPath,
                                  final ShenyuDubboClient methodShenyuClient) {
        final String contextPath = this.getContextPath();
        return superPath.contains("*") ? pathJoin(contextPath, superPath.replace("*", ""), method.getName())
                : pathJoin(contextPath, superPath, methodShenyuClient.path());
    }

    @Override
    protected MetaDataRegisterDTO buildMetaDataDTO(final ServiceBean bean,
                                                   final ShenyuDubboClient shenyuClient,
                                                   final String path,
                                                   final Class<?> clazz,
                                                   final Method method) {
        String appName = buildAppName(bean);
        String desc = shenyuClient.desc();
        String serviceName = bean.getInterface();
        String configRuleName = shenyuClient.ruleName();
        String ruleName = ("".equals(configRuleName)) ? path : configRuleName;
        String methodName = Optional.ofNullable(method).map(Method::getName).orElseThrow(() -> new ShenyuException("unexpected error"));
        Class<?>[] parameterTypesClazz = method.getParameterTypes();
        String parameterTypes = Arrays.stream(parameterTypesClazz).map(Class::getName).collect(Collectors.joining(","));
        return MetaDataRegisterDTO.builder()
                .appName(appName)
                .serviceName(serviceName)
                .methodName(methodName)
                .contextPath(getContextPath())
                .host(buildHost())
                .port(buildPort(bean))
                .path(path)
                .ruleName(ruleName)
                .pathDesc(desc)
                .parameterTypes(parameterTypes)
                .rpcExt(buildRpcExt(bean))
                .rpcType(RpcTypeEnum.DUBBO.getName())
                .enabled(shenyuClient.enabled())
                .build();
    }

    private String buildRpcExt(final ServiceBean<?> serviceBean) {
        DubboRpcExt build = DubboRpcExt.builder()
                .group(StringUtils.isNotEmpty(serviceBean.getGroup()) ? serviceBean.getGroup() : "")
                .version(StringUtils.isNotEmpty(serviceBean.getVersion()) ? serviceBean.getVersion() : "")
                .loadbalance(StringUtils.isNotEmpty(serviceBean.getLoadbalance()) ? serviceBean.getLoadbalance() : Constants.DEFAULT_LOADBALANCE)
                .retries(Optional.ofNullable(serviceBean.getRetries()).orElse(Constants.DEFAULT_RETRIES))
                .timeout(Optional.ofNullable(serviceBean.getTimeout()).orElse(Constants.DEFAULT_CONNECT_TIMEOUT))
                .sent(Optional.ofNullable(serviceBean.getSent()).orElse(Boolean.FALSE))
                .cluster(StringUtils.isNotEmpty(serviceBean.getCluster()) ? serviceBean.getCluster() : Constants.DEFAULT_CLUSTER)
                .url("")
                .build();
        return GsonUtils.getInstance().toJson(build);
    }
}
