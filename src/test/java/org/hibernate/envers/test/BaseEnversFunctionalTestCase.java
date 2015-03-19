package org.hibernate.envers.test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Strong Liu (stliu@hibernate.org)
 */
@RunWith(EnversRunner.class)
public abstract class BaseEnversFunctionalTestCase extends BaseNonConfigCoreFunctionalTestCase {
	private String auditStrategy;

	@Parameterized.Parameters
	public static List<Object[]> data() {
		return Arrays.asList(
				new Object[] {null},
				new Object[] {"org.hibernate.envers.strategy.ValidityAuditStrategy"}
		);
	}

	public void setTestData(Object[] data) {
		auditStrategy = (String) data[0];
	}

	public String getAuditStrategy() {
		return auditStrategy;
	}

	@Override
	protected Session getSession() {
		Session session = super.getSession();
		if ( session == null || !session.isOpen() ) {
			return openSession();
		}
		return session;
	}

	protected AuditReader getAuditReader() {
		return AuditReaderFactory.get( getSession() );
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, "false" );
	}

	@Override
	protected String getBaseForMappings() {
		return "";
	}
}
