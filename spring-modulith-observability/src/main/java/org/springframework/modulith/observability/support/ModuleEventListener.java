/*
 * Copyright 2022-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.modulith.observability.support;

import io.micrometer.core.instrument.Counter;
import io.micrometer.observation.Observation.Event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.modulith.observability.support.ModulithObservations.Events;
import org.springframework.modulith.runtime.ApplicationModulesRuntime;
import org.springframework.util.Assert;

/**
 * @author Oliver Drotbohm
 */
public class ModuleEventListener implements ApplicationListener<ApplicationEvent> {

	private final ApplicationModulesRuntime runtime;
	private final ObservationContext context;
	private final CrossModuleEventCounterFactory factory;

	/**
	 * Creates a new {@link ModuleEventListener} for the given {@link ApplicationModulesRuntime} and
	 * {@link ObservationRegistry} and {@link MeterRegistry}.
	 *
	 * @param runtime must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 * @param counterFactory must not be {@literal null}.
	 */
	public ModuleEventListener(ApplicationModulesRuntime runtime, ObservationContext context,
			CrossModuleEventCounterFactory counterFactory) {

		Assert.notNull(runtime, "ApplicationModulesRuntime must not be null!");
		Assert.notNull(context, "ObservationContext must not be null!");
		Assert.notNull(counterFactory, "ModulithEventCounterFactory must not be null!");

		this.runtime = runtime;
		this.context = context;
		this.factory = counterFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent event) {

		if (!(event instanceof PayloadApplicationEvent<?> payloadEvent)) {
			return;
		}

		var object = payloadEvent.getPayload();
		var payloadType = object.getClass();

		if (!runtime.isApplicationClass(payloadType)) {
			return;
		}

		var moduleByType = runtime.get()
				.getModuleByType(payloadType.getSimpleName())
				.orElse(null);

		if (moduleByType == null) {
			return;
		}

		var registry = context.getMeterRegistry();

		if (registry != null) {

			Counter.builder(ModulithMetrics.EVENTS.getName()) //
					.tags(ModulithMetrics.LowKeys.EVENT_TYPE.name().toLowerCase(), payloadType.getSimpleName()) //
					.tags(ModulithMetrics.LowKeys.MODULE_NAME.name().toLowerCase(), moduleByType.getDisplayName()) //
					.register(registry).increment();

			factory.createCounterBuilder(object).register(registry).increment();
		}

		var observation = context.getObservationRegistry().getCurrentObservation();

		if (observation == null) {
			return;
		}

		observation.event(Event.of(Events.EVENT_PUBLICATION_SUCCESS.getName(), "Published " + payloadType.getName()));
	}
}
