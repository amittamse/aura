/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.auraframework.impl.adapter.format.html;

import org.auraframework.annotations.Annotations.ServiceComponent;
import org.auraframework.system.AuraContext;
import org.auraframework.throwable.AuraExceptionUtil;
import org.auraframework.throwable.AuraRuntimeException;
import org.auraframework.util.json.JsonEncoder;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.Map;

@ThreadSafe
@ServiceComponent
public class AuraContextHTMLFormatAdapter extends HTMLFormatAdapter<AuraContext> {

    @Override
    public Class<AuraContext> getType() {
        return AuraContext.class;
    }

    @Override
    public void write(AuraContext value, Map<String, Object> attributes, Appendable out) throws IOException {
        try {
            JsonEncoder.serialize(value, out);
        } catch (AuraRuntimeException e) {
            AuraExceptionUtil.passQuickFix(e);
        }
    }
}
