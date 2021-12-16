/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.RelationType;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.criteria.AuditId;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.Type;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public abstract class CriteriaTools {
	public static void checkPropertyNotARelation(
			EnversService enversService,
			String entityName,
			String propertyName) throws AuditException {
		if ( enversService.getEntitiesConfigurations().get( entityName ).isRelation( propertyName ) ) {
			throw new AuditException(
					"This criterion cannot be used on a property that is " +
							"a relation to another property."
			);
		}
	}

	public static RelationDescription getRelatedEntity(
			EnversService enversService,
			String entityName,
			String propertyName) throws AuditException {
		RelationDescription relationDesc = enversService.getEntitiesConfigurations().getRelationDescription( entityName, propertyName );

		if ( relationDesc == null ) {
			return null;
		}

		if ( relationDesc.getRelationType() == RelationType.TO_ONE
				|| relationDesc.getRelationType() == RelationType.TO_MANY_MIDDLE
				|| relationDesc.getRelationType() == RelationType.TO_MANY_NOT_OWNING
				|| relationDesc.getRelationType() == RelationType.TO_MANY_MIDDLE_NOT_OWNING ) {
			return relationDesc;
		}

		throw new AuditException(
				String.format(
						Locale.ENGLISH,
						"This type of relation (%s.%s) isn't supported and can't be used in queries.",
						entityName,
						propertyName
				)
		);
	}

	public static String determinePropertyName(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			String entityName,
			PropertyNameGetter propertyNameGetter) {
		return determinePropertyName(
				enversService,
				versionsReader,
				entityName,
				propertyNameGetter.get( enversService.getConfig() )
		);
	}

	/**
	 * @param enversService The EnversService
	 * @param versionsReader Versions reader.
	 * @param entityName Original entity name (not audited).
	 * @param propertyName Property name or placeholder.
	 *
	 * @return Path to property. Handles identifier placeholder used by {@link AuditId}.
	 */
	public static String determinePropertyName(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			String entityName,
			String propertyName) {
		final SessionFactoryImplementor sessionFactory = versionsReader.getSessionImplementor().getFactory();

		if ( AuditId.IDENTIFIER_PLACEHOLDER.equals( propertyName ) ) {
			final String identifierPropertyName = sessionFactory.getMetamodel().entityPersister( entityName ).getIdentifierPropertyName();
			propertyName = enversService.getConfig().getOriginalIdPropertyName() + "." + identifierPropertyName;
		}
		else {
			final List<String> identifierPropertyNames = identifierPropertyNames( sessionFactory, entityName );
			if ( identifierPropertyNames.contains( propertyName ) ) {
				propertyName = enversService.getConfig().getOriginalIdPropertyName() + "." + propertyName;
			}
			else if ( propertyName != null ) {
				// if property starts with an identifier prefix ( e.g. embedded ids ), substitute with the originalId property
				// because Envers performs replacement this automatically during the mapping.
				for ( String identifierPropertyName : identifierPropertyNames ) {
					if ( propertyName.startsWith( identifierPropertyName + "." ) ) {
						propertyName = enversService.getConfig().getOriginalIdPropertyName() +
								propertyName.substring( identifierPropertyName.length() );
						break;
					}
				}
			}
		}

		return propertyName;
	}

	/**
	 * @param sessionFactory Session factory.
	 * @param entityName Entity name.
	 *
	 * @return List of property names representing entity identifier.
	 */
	private static List<String> identifierPropertyNames(SessionFactoryImplementor sessionFactory, String entityName) {
		final String identifierPropertyName = sessionFactory.getMetamodel().entityPersister( entityName ).getIdentifierPropertyName();
		if ( identifierPropertyName != null ) {
			// Single id.
			return Arrays.asList( identifierPropertyName );
		}
		final Type identifierType = sessionFactory.getMetamodel().entityPersister( entityName ).getIdentifierType();
		if ( identifierType instanceof EmbeddedComponentType ) {
			// Multiple ids.
			final EmbeddedComponentType embeddedComponentType = (EmbeddedComponentType) identifierType;
			return Arrays.asList( embeddedComponentType.getPropertyNames() );
		}
		return Collections.emptyList();
	}
}
