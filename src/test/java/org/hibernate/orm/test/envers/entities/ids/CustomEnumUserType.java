/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.entities.ids;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * @author Slawek Garwol (slawekgarwol at gmail dot com)
 */
public class CustomEnumUserType implements UserType<CustomEnum> {

	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	public Class<CustomEnum> returnedClass() {
		return CustomEnum.class;
	}

	public boolean equals(CustomEnum x, CustomEnum y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( (x == null) || (y == null) ) {
			return false;
		}
		return x.equals( y );
	}

	public int hashCode(CustomEnum x) throws HibernateException {
		return (x == null) ? 0 : x.hashCode();
	}

	@Override
	public CustomEnum nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
		String name = rs.getString( position );
		if ( rs.wasNull() ) {
			return null;
		}
		return CustomEnum.fromYesNo( name );
	}

	public void nullSafeSet(PreparedStatement st, CustomEnum value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( value == null ) {
			st.setNull( index, Types.VARCHAR );
		}
		else {
			st.setString( index, value.toYesNo() );
		}
	}

	public CustomEnum deepCopy(CustomEnum value) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}

	public Serializable disassemble(CustomEnum value) throws HibernateException {
		return value;
	}

	public CustomEnum assemble(Serializable cached, Object owner) throws HibernateException {
		return (CustomEnum) cached;
	}

	public CustomEnum replace(CustomEnum original, CustomEnum target, Object owner) throws HibernateException {
		return original;
	}
}
