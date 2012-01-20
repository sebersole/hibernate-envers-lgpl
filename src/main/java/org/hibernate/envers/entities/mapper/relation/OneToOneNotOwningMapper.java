/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.entities.mapper.relation;

import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.reader.AuditReaderImplementor;

import javax.persistence.OneToOne;
import java.io.Serializable;

/**
 * Property mapper for not owning side of {@link OneToOne} relation.
 * @author Adam Warski (adam at warski dot org)
 * @author Hern�n Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)  
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class OneToOneNotOwningMapper extends AbstractOneToOneMapper {
    private final String owningReferencePropertyName;

    public OneToOneNotOwningMapper(String notOwningEntityName, String owningEntityName, String owningReferencePropertyName,
                                   PropertyData propertyData) {
        super(notOwningEntityName, owningEntityName, propertyData);
        this.owningReferencePropertyName = owningReferencePropertyName;
    }

    @Override
    protected Object queryForReferencedEntity(AuditReaderImplementor versionsReader, EntityInfo referencedEntity,
                                              Serializable primaryKey, Number revision) {
        return versionsReader.createQuery().forEntitiesAtRevision(referencedEntity.getEntityClass(),
                                                                  referencedEntity.getEntityName(), revision)
                                           .add(AuditEntity.relatedId(owningReferencePropertyName).eq(primaryKey))
                                           .getSingleResult();
    }

    @Override
    public void mapModifiedFlagsToMapFromEntity(SessionImplementor session, Map<String, Object> data, Object newObj, Object oldObj) {
    }

    @Override
    public void mapModifiedFlagsToMapForCollectionChange(String collectionPropertyName, Map<String, Object> data) {
        if (propertyData.isUsingModifiedFlag()) {
            data.put(propertyData.getModifiedFlagPropertyName(), collectionPropertyName.equals(propertyData.getName()));
        }
    }
}
