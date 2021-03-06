/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.maven.enricher.standard;

import java.util.*;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.maven.core.config.ProcessorConfig;
import io.fabric8.maven.core.util.KubernetesResourceUtil;
import io.fabric8.maven.docker.config.BuildImageConfiguration;
import io.fabric8.maven.docker.config.ImageConfiguration;
import io.fabric8.maven.enricher.api.EnricherContext;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author roland
 * @since 03/06/16
 */
@RunWith(JMockit.class)
public class DefaultServiceEnricherTest {

    @Mocked
    private EnricherContext context;

    @Mocked
    ImageConfiguration imageConfiguration;

    @Test
    public void checkDefaultConfiguration() throws Exception {

        // Setup a sample docker build configuration
        final BuildImageConfiguration buildConfig =
            new BuildImageConfiguration.Builder()
            .ports(Arrays.asList("8080"))
            .build();

        // Setup mock behaviour
        new Expectations() {{
            context.getConfig(); result =
                new ProcessorConfig(null, null, Collections.singletonMap("default.service", new TreeMap(Collections.singletonMap("type", "LoadBalancer"))));
            imageConfiguration.getBuildConfiguration(); result = buildConfig;
            context.getImages(); result = Arrays.asList(imageConfiguration);
        }};

        // Enrich
        DefaultServiceEnricher serviceEnricher = new DefaultServiceEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        serviceEnricher.addMissingResources(builder);

        // Validate that the generated resource contains
        KubernetesList list = builder.build();
        assertEquals(list.getItems().size(),1);

        String json = KubernetesResourceUtil.toJson(list.getItems().get(0));
        assertThat(json, JsonPathMatchers.isJson());
        assertThat(json, JsonPathMatchers.hasJsonPath("$.spec.type", Matchers.equalTo("LoadBalancer")));
    }
}
