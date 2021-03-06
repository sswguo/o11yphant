/**
 * Copyright (C) 2020 Red Hat, Inc. (nos-devel@redhat.com)
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
package org.commonjava.o11yphant.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.healthcheck.HealthCheck;
import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.api.Metric;
import org.commonjava.o11yphant.metrics.api.MetricRegistry;
import org.commonjava.o11yphant.metrics.api.MetricSet;
import org.commonjava.o11yphant.metrics.api.Timer;
import org.commonjava.o11yphant.metrics.impl.O11Meter;
import org.commonjava.o11yphant.metrics.impl.O11Timer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

@ApplicationScoped
public class DefaultMetricRegistry
                implements MetricRegistry
{
    /**
     * This only contains metric registered via {@link #register(String, Metric)} and {@link #register(String, MetricSet)}
     */
    private final Map<String, Metric> metrics = new ConcurrentHashMap();

    private final com.codahale.metrics.MetricRegistry registry;

    private final HealthCheckRegistry healthCheckRegistry;

    @Inject
    public DefaultMetricRegistry( com.codahale.metrics.MetricRegistry registry,
                                  HealthCheckRegistry healthCheckRegistry )
    {
        this.registry = registry;
        this.healthCheckRegistry = healthCheckRegistry;
    }

    /**
     * Register a detached / standalone metric.
     *
     * By using the {@link #gauge(String, Gauge)}, {@link #meter(String)}, or {@link #timer(String)}, the metrics are
     * registered via underlying codahale registry. We do not keep tracking them in this class. If user constructs
     * detached O11 metric objects, they can use this method to register and get them via {@link #getMetrics()}.
     * Those detached metrics are registered to underlying codahale registry as well.
     */
    @Override
    public <T extends Metric> T register( String name, T metric )
    {
        if ( metric instanceof Gauge )
        {
            Gauge gauge = (Gauge) metric;
            registry.register( name, (com.codahale.metrics.Gauge) gauge::getValue );
        }
        else if ( metric instanceof O11Meter )
        {
            registry.register( name, ( (O11Meter) metric ).getCodahaleMeter() );
        }
        else if ( metric instanceof O11Timer )
        {
            registry.register( name, ( (O11Timer) metric ).getCodahaleTimer() );
        }
        metrics.put( name, metric );
        return metric;
    }

    @Override
    public void register( String name, MetricSet metricSet )
    {
        metricSet.getMetrics().forEach( ( k, v ) -> register( name( name, k ), v ) );
    }

    /**
     * This only returns metric registered via {@link #register(String, Metric)} and {@link #register(String, MetricSet)}
     */
    @Override
    public Map<String, Metric> getMetrics()
    {
        return Collections.unmodifiableMap( metrics );
    }

    @Override
    public void registerHealthCheck( String name, HealthCheck healthCheck )
    {
        healthCheckRegistry.register( name, new com.codahale.metrics.health.HealthCheck()
        {
            @Override
            protected Result check() throws Exception
            {
                HealthCheck.Result ret = healthCheck.check();
                ResultBuilder builder = Result.builder();
                if ( ret.isHealthy() )
                {
                    builder.healthy();
                }
                else
                {
                    builder.unhealthy( ret.getError() );
                    builder.withMessage( ret.getMessage() );
                }
                return builder.build();
            }
        } );
    }

    @Override
    public Meter meter( String name )
    {
        return new O11Meter( registry.meter( name ) );
    }

    @Override
    public Timer timer( String name )
    {
        return new O11Timer( registry.timer( name ) );
    }

    @Override
    public <T> Gauge<T> gauge( String name, Gauge<T> o )
    {
        registry.gauge( name, () -> () -> o.getValue() );
        return o;
    }

    protected com.codahale.metrics.MetricRegistry getRegistry()
    {
        return registry;
    }

    // for test
    private boolean consoleReporterStarted;

    public void startConsoleReporter( int periodInSeconds )
    {
        if ( consoleReporterStarted )
        {
            return;
        }

        ConsoleReporter reporter = ConsoleReporter.forRegistry( registry )
                                                  .convertRatesTo( TimeUnit.SECONDS )
                                                  .convertDurationsTo( TimeUnit.MILLISECONDS )
                                                  .build();
        reporter.start( periodInSeconds, TimeUnit.SECONDS );
        consoleReporterStarted = true;
    }

}
