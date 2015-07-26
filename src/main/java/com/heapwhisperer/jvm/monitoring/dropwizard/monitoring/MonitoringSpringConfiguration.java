package com.heapwhisperer.jvm.monitoring.dropwizard.monitoring;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.heapwhisperer.metrics.jvm.NonAccumulatingGarbageCollectorMetricSet;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class MonitoringSpringConfiguration {

    // reporting interval in milliseconds
    private static long interval = 10000;

    @Autowired
    Environment environment;

    @Value("${graphite.host:localhost}")
    String graphiteHost;

    @Bean
    @ConditionalOnMissingBean(MetricRegistry.class)
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

    @Bean
    @ConditionalOnProperty("graphite.host")
    public GraphiteReporter graphiteReporter() {
        Graphite graphite = new Graphite(graphiteHost, 1337);
        GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry()).build(graphite);
        reporter.start(interval, TimeUnit.MILLISECONDS);

        return reporter;
    }

    @Bean
    @ConditionalOnMissingBean(GraphiteReporter.class)
    public ConsoleReporter consoleReporter() {
        ConsoleReporter.Builder builder = ConsoleReporter.forRegistry(metricRegistry());
        ConsoleReporter reporter = builder.build();
        reporter.start(interval, TimeUnit.MILLISECONDS);
        return reporter;
    }

    @PostConstruct
    public void registerMetrics() {
        metricRegistry().registerAll(new MemoryUsageGaugeSet());
        metricRegistry().registerAll(new GarbageCollectorMetricSet());
        // specialized implementation of GarbageCollectorMetricSet that zeroes the stats
        // before each time interval, rather than reporting a running total since the JVM started up
        metricRegistry().register("nonaccumulating",
                new NonAccumulatingGarbageCollectorMetricSet(new GarbageCollectorMetricSet(), interval));
    }
}
