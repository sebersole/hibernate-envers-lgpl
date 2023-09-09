/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.Audited;
import org.hibernate.envers.boot.internal.ModifiedColumnNameResolver;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;

import jakarta.persistence.MappedSuperclass;

/**
 * Reads the audited properties for components.
 *
 * @author Hern&aacut;n Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public class ComponentAuditedPropertiesReader extends AuditedPropertiesReader {

	private final ComponentAuditingData componentAuditingData;

	public ComponentAuditedPropertiesReader(
			EnversMetadataBuildingContext metadataBuildingContext,
			PersistentPropertiesSource persistentPropertiesSource,
			AuditedPropertiesHolder auditedPropertiesHolder) {
		this( metadataBuildingContext, persistentPropertiesSource, auditedPropertiesHolder, NO_PREFIX );
	}

	public ComponentAuditedPropertiesReader(
			EnversMetadataBuildingContext metadataBuildingContext,
			PersistentPropertiesSource persistentPropertiesSource,
			AuditedPropertiesHolder auditedPropertiesHolder,
			String propertyNamePrefix) {
		super(
				metadataBuildingContext,
				persistentPropertiesSource,
				auditedPropertiesHolder,
				propertyNamePrefix
		);
		this.componentAuditingData = (ComponentAuditingData) auditedPropertiesHolder;
	}

	@Override
	protected boolean isClassHierarchyTraversalNeeded(Audited allClassAudited) {
		// we always traverse the hierarchy for components
		return true;
	}

	@Override
	protected boolean checkAudited(
			XProperty property,
			PropertyAuditingData propertyData,
			String propertyName,
			Audited allClassAudited,
			String modifiedFlagSuffix) {
		// Checking if this property is explicitly audited or if all properties are.
		final Audited aud = property.getAnnotation( Audited.class );
		if ( aud != null ) {
			propertyData.setRelationTargetAuditMode( aud.targetAuditMode() );
			propertyData.setUsingModifiedFlag( checkUsingModifiedFlag( aud ) );
			propertyData.setModifiedFlagName( ModifiedColumnNameResolver.getName( propertyName, modifiedFlagSuffix ) );
			if ( StringHelper.isNotEmpty( aud.modifiedColumnName() ) ) {
				propertyData.setExplicitModifiedFlagName( aud.modifiedColumnName() );
			}
		}
		else {

			// get declaring class for property
			final XClass declaringClass = property.getDeclaringClass();

			// check component data to make sure that no audit overrides were not defined at
			// the parent class or the embeddable at the property level.
			boolean classAuditedOverride = false;
			boolean classNotAuditedOverride = false;
			for ( AuditOverrideData auditOverride : componentAuditingData.getAuditingOverrides() ) {
				if ( auditOverride.getForClass() != void.class ) {
					final String className = auditOverride.getForClass().getName();
					if ( declaringClass.getName().equals( className ) ) {
						if ( !auditOverride.isAudited() ) {
							if ( "".equals( auditOverride.getName() ) ) {
								classNotAuditedOverride = true;
							}
							if ( property.getName().equals( auditOverride.getName() ) ) {
								return false;
							}
						}
						else {
							if ( "".equals( auditOverride.getName() ) ) {
								classAuditedOverride = true;
							}
							if ( property.getName().equals( auditOverride.getName() ) ) {
								return true;
							}
						}
					}
				}
			}

			// make sure taht if the class or property are explicitly 'isAudited=false', use that.
			if ( classNotAuditedOverride || isOverriddenNotAudited( property) || isOverriddenNotAudited( declaringClass ) ) {
				return false;
			}

			// make sure that if the class or property are explicitly 'isAudited=true', use that.
			if ( classAuditedOverride || isOverriddenAudited( property ) || isOverriddenAudited( declaringClass ) ) {
				return true;
			}

			// make sure that if a component is annotated with audited, it is honored.
			if ( allClassAudited != null ) {
				return true;
			}

			// assumption here is if a component reader is looking at a @MappedSuperclass, it should be treated
			// as not being audited if we have reached htis point; allowing components and any @Embeddable
			// class being audited by default.
			if ( declaringClass.isAnnotationPresent( MappedSuperclass.class ) ) {
				return false;
			}
		}
		return true;
	}

}
