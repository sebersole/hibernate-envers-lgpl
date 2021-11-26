/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.property;

import org.hibernate.envers.configuration.Configuration;

/**
 * Provides a function to get the name of a property, which is used in a query, to apply some restrictions on it.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public interface PropertyNameGetter {
	/**
	 * @param configuration the envers configuration
	 * @return Name of the property, to be used in a query.
	 */
	String get(Configuration configuration);
}
