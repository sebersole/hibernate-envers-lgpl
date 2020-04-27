/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.envers.boot;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005601, max = 90005700 )
public interface EnversBootLogger extends BasicLogger {
	String LOGGER_NAME = "org.hibernate.envers.boot";

	EnversBootLogger BOOT_LOGGER = Logger.getMessageLogger(
			EnversBootLogger.class,
			LOGGER_NAME
	);

	boolean TRACE_ENABLED = BOOT_LOGGER.isTraceEnabled();
	boolean DEBUG_ENABLED = BOOT_LOGGER.isDebugEnabled();

	static String subLoggerName(String subName) {
		return LOGGER_NAME + '.' + subName;
	}

	static Logger subLogger(String subName) {
		return Logger.getLogger( subLoggerName( subName ) );
	}

	/**
	 * Log about usage of deprecated Scanner setting
	 */
	@LogMessage( level = INFO )
	@Message(
			value = "Envers-generated HBM mapping...%n%s",
			id = 90000001
	)
	void jaxbContribution(String hbm);
}
