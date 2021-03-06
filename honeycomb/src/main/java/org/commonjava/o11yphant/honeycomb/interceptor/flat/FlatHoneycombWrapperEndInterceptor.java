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
import static org.commonjava.o11yphant.honeycomb.util.InterceptorUtils.getMetricNameFromContext;
import static org.commonjava.o11yphant.metrics.MetricsConstants.SKIP_METRIC;

@Interceptor
@MetricWrapper
public class FlatHoneycombWrapperEndInterceptor
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
        logger.trace( "START: Honeycomb metrics-end wrapper: {}", name );
        if ( !config.isEnabled() )
        {
            logger.trace( "SKIP: Honeycomb metrics-end wrapper: {}", name );
            return context.proceed();
        }

        if ( name == null || SKIP_METRIC.equals( name ) || config.getSampleRate( context.getMethod() ) < 1 )
        {
            logger.trace( "SKIP: Honeycomb metrics-end wrapper (span not configured: {})", name );
            return context.proceed();
        }

        Span span = honeycombManager.getActiveSpan();
        try
        {
            if ( span != null )
            {
                honeycombManager.addEndField( span, name, currentTimeMillis() );
            }
            return context.proceed();
        }
        finally
        {
            logger.trace( "END: Honeycomb metrics-end wrapper: {}", name );
        }
    }

}
