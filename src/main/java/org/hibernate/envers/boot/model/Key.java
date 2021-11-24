/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;

/**
 * Contract for declaring a column name of a foreign key.
 *
 * @author Chris Cranford
 */
public class Key implements ColumnContainer, Bindable<JaxbHbmKeyType>, Cloneable<Key> {

	private final List<Column> columns;

	public Key() {
		this.columns = new ArrayList<>();
	}

	public Key(Key key) {
		this.columns = new ArrayList<>();
		for ( Column column : key.columns ) {
			this.columns.add( new Column( column ) );
		}
	}


	@Override
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public void addColumn(Column column) {
		this.columns.add( column );
	}

	@Override
	public Key deepCopy() {
		return new Key( this );
	}

	@Override
	public JaxbHbmKeyType build() {
		final JaxbHbmKeyType key = new JaxbHbmKeyType();
		for ( Column column : columns ) {
			key.getColumn().add( column.build() );
		}
		return key;
	}
}
