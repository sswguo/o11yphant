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
package org.commonjava.o11yphant.honeycomb.interceptor.flat;

import io.honeycomb.beeline.tracing.Span;
import org.commonjava.o11yphant.metrics.annotation.MetricWrapper;
import org.commonjava.o11yphant.honeycomb.HoneycombManager;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import static java.lang.System.currentTimeMillis;
import static org.commonjava.o11yphant.honeycomb.util.InterceptorUtils.getMetricNameFromContextAfterRun;
import static org.commonjava.o11yphant.honeycomb.util.InterceptorUtils.getMetricNameFromContext;
import static org.commonjava.o11yphant.metrics.MetricsConstants.SKIP_METRIC;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

@Interceptor
@MetricWrapper
public class FlatHoneycombWrapperInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private HoneycombConfiguration config;

    @Inject
    private HoneycombManager honeycombManager;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        String name = getMetricNameFromContext( context );
        logger.debug( "START: Honeycomb lambda wrapper: {}", name );
        if ( !config.isEnabled() )
        {
            logger.debug( "SKIP Honeycomb lambda wrapper: {}", name );
            return context.proceed();
        }

        if ( name == null || SKIP_METRIC.equals( name ) || config.getSampleRate( name ) < 1 )
        {
            logger.debug( "SKIP Honeycomb lambda wrapper (no span name or span not configured: {})", name );
            return context.proceed();
        }

        long begin = currentTimeMillis();
        Span span = null;
        try
        {
            span = honeycombManager.getActiveSpan();
            return context.proceed();
        }
        finally
        {
            if ( span != null )
            {
                long elapse = currentTimeMillis() - begin;
                name = name( name, getMetricNameFromContextAfterRun( context ) );
                honeycombManager.addCumulativeField( span, name, elapse );
            }
            logger.debug( "END: Honeycomb lambda wrapper: {}", name );
        }
    }

}
