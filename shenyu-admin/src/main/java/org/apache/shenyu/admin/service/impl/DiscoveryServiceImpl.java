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

package org.apache.shenyu.admin.service.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.admin.discovery.DiscoveryLevel;
import org.apache.shenyu.admin.discovery.DiscoveryProcessor;
import org.apache.shenyu.admin.discovery.DiscoveryProcessorHolder;
import org.apache.shenyu.admin.mapper.DiscoveryHandlerMapper;
import org.apache.shenyu.admin.mapper.DiscoveryMapper;
import org.apache.shenyu.admin.mapper.ProxySelectorMapper;
import org.apache.shenyu.admin.model.dto.DiscoveryDTO;
import org.apache.shenyu.admin.model.entity.DiscoveryDO;
import org.apache.shenyu.admin.model.entity.DiscoveryHandlerDO;
import org.apache.shenyu.admin.model.enums.DiscoveryTypeEnum;
import org.apache.shenyu.admin.model.vo.DiscoveryVO;
import org.apache.shenyu.admin.service.DiscoveryService;
import org.apache.shenyu.admin.transfer.DiscoveryTransfer;
import org.apache.shenyu.admin.utils.ShenyuResultMessage;
import org.apache.shenyu.common.exception.ShenyuException;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.common.utils.UUIDUtils;
import org.apache.shenyu.register.common.dto.DiscoveryConfigRegisterDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

@Service
public class DiscoveryServiceImpl implements DiscoveryService {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryServiceImpl.class);

    private final DiscoveryMapper discoveryMapper;

    private final ProxySelectorMapper proxySelectorMapper;

    private final DiscoveryHandlerMapper discoveryHandlerMapper;

    private final DiscoveryProcessorHolder discoveryProcessorHolder;

    public DiscoveryServiceImpl(final DiscoveryMapper discoveryMapper,
                                final ProxySelectorMapper proxySelectorMapper,
                                final DiscoveryHandlerMapper discoveryHandlerMapper,
                                final DiscoveryProcessorHolder discoveryProcessorHolder) {
        this.discoveryMapper = discoveryMapper;
        this.discoveryProcessorHolder = discoveryProcessorHolder;
        this.proxySelectorMapper = proxySelectorMapper;
        this.discoveryHandlerMapper = discoveryHandlerMapper;
    }

    @Override
    public List<String> typeEnums() {
        return DiscoveryTypeEnum.types();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiscoveryVO discovery(final String pluginName, final String level) {
        return discoveryVO(discoveryMapper.selectByPluginNameAndLevel(pluginName, level));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiscoveryVO createOrUpdate(final DiscoveryDTO discoveryDTO) {
        return StringUtils.isBlank(discoveryDTO.getId()) ? this.create(discoveryDTO) : this.update(discoveryDTO);
    }

    @Override
    public void registerDiscoveryConfig(final DiscoveryConfigRegisterDTO discoveryConfigRegisterDTO) {
        DiscoveryDTO discoveryDTO = new DiscoveryDTO();
        discoveryDTO.setName(discoveryConfigRegisterDTO.getName());
        discoveryDTO.setType(discoveryConfigRegisterDTO.getDiscoveryType());
        discoveryDTO.setPluginName(discoveryConfigRegisterDTO.getPluginName());
        discoveryDTO.setServerList(discoveryConfigRegisterDTO.getServerList());
        discoveryDTO.setLevel(DiscoveryLevel.PLUGIN.getCode());
        discoveryDTO.setProps(GsonUtils.getInstance().toJson(Optional.ofNullable(discoveryConfigRegisterDTO.getProps()).orElse(new Properties())));
        DiscoveryDO discoveryDO = discoveryMapper.selectByName(discoveryConfigRegisterDTO.getName());
        if (Objects.nonNull(discoveryDO)) {
            LOG.warn("shenyu DiscoveryConfigRegisterDTO has been register");
            return;
        }
        this.create(discoveryDTO);
        LOG.info("shenyu success register DiscoveryConfigRegisterDTO name={}|pluginName={}", discoveryConfigRegisterDTO.getName(), discoveryConfigRegisterDTO.getPluginName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String delete(final String discoveryId) {
        List<DiscoveryHandlerDO> discoveryHandlerDOS = discoveryHandlerMapper.selectByDiscoveryId(discoveryId);
        if (CollectionUtils.isNotEmpty(discoveryHandlerDOS)) {
            LOG.warn("shenyu this discovery has discoveryHandler can't be delete");
            throw new ShenyuException("shenyu this discovery has discoveryHandler can't be delete");
        }
        DiscoveryDO discoveryDO = discoveryMapper.selectById(discoveryId);
        DiscoveryProcessor discoveryProcessor = discoveryProcessorHolder.chooseProcessor(discoveryDO.getType());
        discoveryProcessor.removeDiscovery(discoveryDO);
        discoveryMapper.delete(discoveryId);
        return ShenyuResultMessage.DELETE_SUCCESS;
    }

    private DiscoveryVO create(final DiscoveryDTO discoveryDTO) {
        if (discoveryDTO == null) {
            return null;
        }
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        DiscoveryDO discoveryDO = DiscoveryDO.builder()
                .id(discoveryDTO.getId())
                .name(discoveryDTO.getName())
                .pluginName(discoveryDTO.getPluginName())
                .level(discoveryDTO.getLevel())
                .type(discoveryDTO.getType())
                .serverList(discoveryDTO.getServerList())
                .props(discoveryDTO.getProps())
                .dateCreated(currentTime)
                .dateUpdated(currentTime)
                .build();
        if (StringUtils.isEmpty(discoveryDTO.getId())) {
            discoveryDO.setId(UUIDUtils.getInstance().generateShortUuid());
        }
        DiscoveryProcessor discoveryProcessor = discoveryProcessorHolder.chooseProcessor(discoveryDO.getType());
        DiscoveryVO result = discoveryMapper.insert(discoveryDO) > 0 ? discoveryVO(discoveryDO) : null;
        discoveryProcessor.createDiscovery(discoveryDO);
        return result;
    }

    private DiscoveryVO update(final DiscoveryDTO discoveryDTO) {
        if (discoveryDTO == null || discoveryDTO.getId() == null) {
            return null;
        }
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        DiscoveryDO discoveryDO = DiscoveryDO.builder()
                .id(discoveryDTO.getId())
                .name(discoveryDTO.getName())
                .type(discoveryDTO.getType())
                .serverList(discoveryDTO.getServerList())
                .props(discoveryDTO.getProps())
                .dateUpdated(currentTime)
                .build();
        return discoveryMapper.updateSelective(discoveryDO) > 0 ? discoveryVO(discoveryDO) : null;
    }

    /**
     * copy discovery data.
     *
     * @param discoveryDO {@link DiscoveryDTO}
     * @return {@link DiscoveryVO}
     */
    private DiscoveryVO discoveryVO(final DiscoveryDO discoveryDO) {
        if (discoveryDO == null) {
            return null;
        }
        DiscoveryVO discoveryVO = new DiscoveryVO();
        BeanUtils.copyProperties(discoveryDO, discoveryVO);
        return discoveryVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncData() {
        LOG.info("shenyu DiscoveryService sync db ");
        List<DiscoveryDO> discoveryDOS = discoveryMapper.selectAll();
        discoveryDOS.forEach(d -> {
            DiscoveryProcessor discoveryProcessor = discoveryProcessorHolder.chooseProcessor(d.getType());
            discoveryProcessor.createDiscovery(d);
            proxySelectorMapper.selectByDiscoveryId(d.getId()).stream().map(DiscoveryTransfer.INSTANCE::mapToDTO).forEach(ps -> {
                DiscoveryHandlerDO discoveryHandlerDO = discoveryHandlerMapper.selectByProxySelectorId(ps.getId());
                discoveryProcessor.createProxySelector(DiscoveryTransfer.INSTANCE.mapToDTO(discoveryHandlerDO), ps);
                discoveryProcessor.fetchAll(discoveryHandlerDO.getId());
            });
        });
    }
}
