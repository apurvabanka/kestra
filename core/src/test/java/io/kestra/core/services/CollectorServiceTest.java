package io.kestra.core.services;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.Helpers;
import io.kestra.core.models.ServerType;
import io.kestra.core.models.Setting;
import io.kestra.core.models.collectors.Usage;
import io.kestra.core.repositories.SettingRepositoryInterface;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.kestra.core.junit.annotations.KestraTest;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.validation.ConstraintViolationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class CollectorServiceTest {
    @Test
    public void metrics() throws URISyntaxException {
        ImmutableMap<String, Object> properties = ImmutableMap.of("kestra.server-type", ServerType.WEBSERVER.name());

        try (ApplicationContext applicationContext = Helpers.applicationContext(properties).start()) {
            CollectorService collectorService = applicationContext.getBean(CollectorService.class);
            Usage metrics = collectorService.metrics(true);

            assertThat(metrics.getUri(), is("https://mysuperhost.com/subpath"));

            assertThat(metrics.getUuid(), notNullValue());
            assertThat(metrics.getVersion(), notNullValue());
            assertThat(metrics.getStartTime(), notNullValue());
            assertThat(metrics.getEnvironments(), hasItem("test"));
            assertThat(metrics.getStartTime(), notNullValue());
            assertThat(metrics.getHost().getUuid(), notNullValue());
            assertThat(metrics.getHost().getHardware().getLogicalProcessorCount(), notNullValue());
            assertThat(metrics.getHost().getJvm().getName(), notNullValue());
            assertThat(metrics.getHost().getOs().getFamily(), notNullValue());
            assertThat(metrics.getConfigurations().getRepositoryType(), is("memory"));
            assertThat(metrics.getConfigurations().getQueueType(), is("memory"));
            assertThat(metrics.getExecutions(), notNullValue());
            // 1 per hour
            assertThat(metrics.getExecutions().getDailyExecutionsCount().size(), greaterThan(0));
            // no task runs as it's an empty instance
            assertThat(metrics.getExecutions().getDailyTaskRunsCount(), nullValue());
            assertThat(metrics.getInstanceUuid(), is(TestSettingRepository.instanceUuid));
        }
    }

    @Singleton
    @Requires(property = "kestra.unittest")
    @Primary
    public static class TestSettingRepository implements SettingRepositoryInterface {
        public static Object instanceUuid = null;

        @Override
        public Optional<Setting> findByKey(String key) {
            return Optional.empty();
        }

        @Override
        public List<Setting> findAll() {
            return new ArrayList<>();
        }

        @Override
        public Setting save(Setting setting) throws ConstraintViolationException {
            if (setting.getKey().equals(Setting.INSTANCE_UUID)) {
                TestSettingRepository.instanceUuid = setting.getValue();
            }

            return setting;
        }

        @Override
        public Setting delete(Setting setting) {
            return setting;
        }
    }
}