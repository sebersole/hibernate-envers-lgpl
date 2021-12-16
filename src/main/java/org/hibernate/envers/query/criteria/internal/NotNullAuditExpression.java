/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria.internal;

import java.util.Locale;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.RelationType;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NotNullAuditExpression extends AbstractAtomicExpression {
	private PropertyNameGetter propertyNameGetter;

	public NotNullAuditExpression(String alias, PropertyNameGetter propertyNameGetter) {
		super( alias );
		this.propertyNameGetter = propertyNameGetter;
	}

	@Override
	protected void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			String entityName,
			String alias,
			QueryBuilder qb,
			Parameters parameters) {
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				versionsReader,
				entityName,
				propertyNameGetter
		);
		RelationDescription relatedEntity = CriteriaTools.getRelatedEntity( enversService, entityName, propertyName );

		if ( relatedEntity == null ) {
			parameters.addNotNullRestriction( alias, propertyName );
		}
		else if ( relatedEntity.getRelationType() == RelationType.TO_ONE ) {
			relatedEntity.getIdMapper().addIdEqualsToQuery( parameters, null, alias, null, false );
		}
		else {
			throw new AuditException(
					String.format(
							Locale.ENGLISH,
							"This type of relation (%s.%s) can't be used with not null restrictions.",
							entityName,
							propertyName
					)
			);
		}
	}
}
