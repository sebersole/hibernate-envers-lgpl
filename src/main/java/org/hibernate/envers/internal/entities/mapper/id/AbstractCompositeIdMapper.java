/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.id;

import java.util.Map;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.Tools;
import org.hibernate.internal.util.ReflectHelper;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class AbstractCompositeIdMapper extends AbstractIdMapper implements SimpleIdMapperBuilder {
	protected Map<PropertyData, SingleIdMapper> ids;
	protected Class compositeIdClass;

	protected AbstractCompositeIdMapper(Class compositeIdClass) {
		ids = Tools.newLinkedHashMap();
		this.compositeIdClass = compositeIdClass;
	}

	@Override
	public void add(PropertyData propertyData) {
		ids.put( propertyData, new SingleIdMapper( propertyData ) );
	}

	@Override
	public Object mapToIdFromMap(Map data) {
		if ( data == null ) {
			return null;
		}

		final Object ret;
		try {
			ret = ReflectHelper.getDefaultConstructor( compositeIdClass ).newInstance();
		}
		catch (Exception e) {
			throw new AuditException( e );
		}

		for ( SingleIdMapper mapper : ids.values() ) {
			if ( !mapper.mapToEntityFromMap( ret, data ) ) {
				return null;
			}
		}

		return ret;
	}
}
