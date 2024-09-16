/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.hbm.allAudited;

import org.hibernate.envers.Audited;

/**
 * @author Hern�n Chanfreau
 */
@Audited
public interface SimpleInterface {

	long getId();

	void setId(long id);

	String getData();

	void setData(String data);

}
