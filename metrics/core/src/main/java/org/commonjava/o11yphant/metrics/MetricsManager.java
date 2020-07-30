/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.o11yphant.conf.MetricsConfig;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public interface MetricsManager
{
    HealthCheckRegistry getHealthCheckRegistry();

    boolean isMetered( Supplier<Boolean> meteringOverride );

    Timer.Context startTimer( String name );

    long stopTimer( String name );

    Meter getMeter( String name );

    void accumulate( String name, final double elapsed );

    <T> T wrapWithStandardMetrics( final Supplier<T> method, final Supplier<String> classifier );

    boolean checkMetered();

    boolean checkMetered( ThreadContext ctx );

    void stopTimers( final Map<String, Timer.Context> timers );

    void mark( final Collection<String> metricNames );

    void addGauges( Class<?> className, String method, Map<String, Gauge<Integer>> gauges );

    MetricRegistry getMetricRegistry();

    MetricsConfig getConfig();
}