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

package org.apache.shenyu.sdk.starter.core.annotation;

import org.apache.shenyu.sdk.core.common.RequestTemplate;
import org.apache.shenyu.sdk.starter.core.factory.AnnotatedParameterProcessor;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.annotation.Annotation;

import static com.google.common.base.Strings.emptyToNull;
import static org.apache.shenyu.sdk.core.util.Util.checkState;

/**
 * {@link RequestHeader} parameter processor.
 */
public class RequestHeaderParameterProcessor implements AnnotatedParameterProcessor {

    private static final Class<RequestHeader> ANNOTATION = RequestHeader.class;

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return ANNOTATION;
    }

    @Override
    public boolean processArgument(final RequestTemplate requestTemplate, final Annotation annotation, final Object arg) {
        String name = ANNOTATION.cast(annotation).value();
        checkState(emptyToNull(name) != null, "RequestHeader.value() was empty on parameter %s", requestTemplate.getMethod().getName());
        return true;
    }

}