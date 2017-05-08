/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.script;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.script.Bindings;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import com.axelor.db.JPA;
import com.axelor.db.JpaScanner;
import com.axelor.rpc.Context;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

public class GroovyScriptHelper extends AbstractScriptHelper {

	private static final CompilerConfiguration config = new CompilerConfiguration();

	private static final int DEFAULT_CACHE_SIZE = 500;
	private static final int DEFAULT_CACHE_EXPIRE_TIME = 60;

	private static int cacheSize;
	private static int cacheExpireTime;

	private static final GroovyClassLoader GCL;
	private static final Cache<String, Class<?>> SCRIPT_CACHE;

	public static class Helpers {

		@SuppressWarnings("unchecked")
		public static <T> T doInJPA(Function<EntityManager, T> task) {
			final Object[] result = { null };
			JPA.runInTransaction(() -> result[0] = task.apply(JPA.em()));
			return (T) result[0];
		}
	}

	static {
		config.getOptimizationOptions().put("indy", Boolean.TRUE);
		config.getOptimizationOptions().put("int", Boolean.FALSE);

		final ImportCustomizer importCustomizer = new ImportCustomizer();

		importCustomizer.addImport("__repo__", "com.axelor.db.JpaRepository");

		importCustomizer.addStaticImport(Helpers.class.getName(), "doInJPA");

		importCustomizer.addImports("java.time.ZonedDateTime");
		importCustomizer.addImports("java.time.LocalDateTime");
		importCustomizer.addImports("java.time.LocalDate");
		importCustomizer.addImports("java.time.LocalTime");

		config.addCompilationCustomizers(importCustomizer);

		try {
			cacheSize = Integer.parseInt(System.getProperty("axelor.ScriptCacheSize"));
		} catch (Exception e) {
		}
		try {
			cacheExpireTime = Integer.parseInt(System.getProperty("axelor.ScriptCacheExpireTime"));
		} catch (Exception e) {
		}

		if (cacheSize <= 0) {
			cacheSize = DEFAULT_CACHE_SIZE;
		}
		if (cacheExpireTime <= 0) {
			cacheExpireTime = DEFAULT_CACHE_EXPIRE_TIME;
		}

		SCRIPT_CACHE = CacheBuilder.newBuilder()
				.maximumSize(cacheSize)
				.expireAfterAccess(cacheExpireTime, TimeUnit.MINUTES)
				.build();

		GCL = new GroovyClassLoader(JpaScanner.getClassLoader(), config);
	}

	public GroovyScriptHelper(Bindings bindings) {
		this.setBindings(bindings);
	}

	public GroovyScriptHelper(Context context) {
		this(new ScriptBindings(context));
	}

	private Class<?> parseClass(String code) {

		Class<?> klass = SCRIPT_CACHE.getIfPresent(code);
		if (klass != null) {
			return klass;
		}

		try {
			klass = GCL.parseClass(code);
		} finally {
			GCL.clearCache();
		}

		SCRIPT_CACHE.put(code, klass);

		return klass;
	}

	@Override
	public Object eval(String expr, Bindings bindings) throws Exception {
		Class<?> klass = parseClass(expr);
		Script script = (Script) klass.newInstance();
		script.setBinding(new Binding(bindings) {

			@Override
			public Object getVariable(String name) {
				try {
					return super.getVariable(name);
				} catch (MissingPropertyException e) {
				}
				return null;
			}
		});
		return script.run();
	}
}
