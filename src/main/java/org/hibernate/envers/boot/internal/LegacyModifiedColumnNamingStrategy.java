/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.envers.boot.model.AttributeContainer;
import org.hibernate.envers.boot.spi.ModifiedColumnNamingStrategy;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.mapping.Value;

/**
 * A {@link ModifiedColumnNamingStrategy} that adds modified columns with the following rules:
 * <ul>
 *     <li>If an audit annotation modified column name is supplied, use it directly with no suffix.</li>
 *     <li>If no audit annotation modified column name is present, use the property name appended with suffix.</li>
 * </ul>
 *
 * This is the default Envers modified column naming behavior.
 *
 * @author Chris Cranford
 * @since 5.4.7
 */
public class LegacyModifiedColumnNamingStrategy extends AbstractModifiedColumnNamingStrategy {
	@Override
	public void addModifiedColumns(
			Configuration configuration,
			Value value,
			AttributeContainer mapping,
			PropertyAuditingData propertyAuditingData) {
		final String columnName;
		if ( propertyAuditingData.isModifiedFlagNameExplicitlySpecified() ) {
			columnName = propertyAuditingData.getExplicitModifiedFlagName();
		}
		else {
			columnName = propertyAuditingData.getModifiedFlagName();
		}
		mapping.addAttribute( createModifiedFlagAttribute( propertyAuditingData, configuration, columnName ) );
	}
}
