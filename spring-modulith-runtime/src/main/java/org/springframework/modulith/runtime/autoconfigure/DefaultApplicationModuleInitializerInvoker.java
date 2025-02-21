/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.modulith.runtime.autoconfigure;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.modulith.ApplicationModuleInitializer;
import org.springframework.modulith.core.ApplicationModules;

/**
 * @author Oliver Drotbohm
 */
class DefaultApplicationModuleInitializerInvoker implements ApplicationModuleInitializerInvoker {

	private final Supplier<ApplicationModules> modules;

	/**
	 * @param modules
	 */
	DefaultApplicationModuleInitializerInvoker(Supplier<ApplicationModules> modules) {
		this.modules = modules;
	}

	/*
	 *
	 * (non-Javadoc)
	 * @see org.springframework.modulith.runtime.autoconfigure.ApplicationModuleInitializerInvoker#invokeInitializers(java.util.stream.Stream)
	 */
	@Override
	public void invokeInitializers(Stream<ApplicationModuleInitializer> initializers) {

		var modules = this.modules.get();

		initializers
				.sorted(modules.getComparator()) //
				.map(it -> LoggingApplicationModuleInitializerAdapter.of(it, modules))
				.forEach(ApplicationModuleInitializer::initialize);
	}
}
