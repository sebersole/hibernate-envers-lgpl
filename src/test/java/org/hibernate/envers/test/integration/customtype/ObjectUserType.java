/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.customtype;

import java.io.Serializable;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.usertype.CompositeUserType;

import jakarta.persistence.Lob;

/**
 * Custom type used to persist binary representation of Java object in the database.
 * Spans over two columns - one storing text representation of Java class name and the second one
 * containing binary data.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ObjectUserType implements CompositeUserType<Object> {

	@Override
	public Object getPropertyValue(Object component, int property) throws HibernateException {
		switch ( property ) {
			case 0:
				return component;
			case 1:
				return component.getClass().getName();
		}
		return null;
	}

	@Override
	public Object instantiate(Supplier<Object[]> values, SessionFactoryImplementor sessionFactory) {
		return values.get()[0];
	}

	@Override
	public Class<?> embeddable() {
		return TaggedObject.class;
	}

	@Override
	public Class<Object> returnedClass() {
		return Object.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		return x.equals( y );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		return value; // Persisting only immutable types.
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}

	public static class TaggedObject {
		String type;
		@Lob
		Serializable object;
	}
}
